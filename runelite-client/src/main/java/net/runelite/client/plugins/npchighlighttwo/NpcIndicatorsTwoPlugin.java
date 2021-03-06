/*
 * Copyright (c) 2018, James Swindle <wilingua@gmail.com>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.npchighlighttwo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;

@PluginDescriptor(
	name = "NPC Indicators Two",
	description = "Highlight NPCs on-screen and/or on the minimap",
	tags = {"highlight", "minimap", "npcs", "overlay", "respawn", "tags"}
)
@Slf4j
public class NpcIndicatorsTwoPlugin extends Plugin
{
	private static final int MAX_ACTOR_VIEW_RANGE = 15;

	// Option added to NPC menu
	private static final String TAG = "Tag";
	private static final String UNTAG = "Un-tag";

	private static final Set<MenuAction> NPC_MENU_ACTIONS = ImmutableSet.of(MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_SECOND_OPTION,
		MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION, MenuAction.NPC_FIFTH_OPTION);

	@Inject
	private Client client;

	@Inject
	private NpcIndicatorsConfigTwo config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NpcSceneOverlayTwo npcSceneOverlay;

	@Inject
	private NpcMinimapOverlayTwo npcMinimapOverlay;

	@Inject
	private NpcIndicatorsTwoInput inputListener;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	@Setter(AccessLevel.PACKAGE)
	private boolean hotKeyPressed = false;

	/**
	 * NPCs to highlight
	 */
	@Getter(AccessLevel.PACKAGE)
	private final Set<NPC> highlightedNpcs = new HashSet<>();

	/**
	 * Dead NPCs that should be displayed with a respawn indicator if the config is on.
	 */
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, MemorizedNpcTwo> deadNpcsToDisplay = new HashMap<>();

	/**
	 * The time when the last game tick event ran.
	 */
	@Getter(AccessLevel.PACKAGE)
	private Instant lastTickUpdate;

	/**
	 * Tagged NPCs that have died at some point, which are memorized to
	 * remember when and where they will respawn
	 */
	private final Map<Integer, MemorizedNpcTwo> memorizedNpcs = new HashMap<>();

	/**
	 * Highlight strings from the configuration
	 */
	private List<String> highlights = new ArrayList<>();

	private List<String> npcAttackAnimations = new ArrayList<>();

	/**
	 * NPC ids marked with the Tag option
	 */
	private final Set<Integer> npcTags = new HashSet<>();

	/**
	 * Tagged NPCs that spawned this tick, which need to be verified that
	 * they actually spawned and didn't just walk into view range.
	 */
	private final List<NPC> spawnedNpcsThisTick = new ArrayList<>();

	/**
	 * Tagged NPCs that despawned this tick, which need to be verified that
	 * they actually spawned and didn't just walk into view range.
	 */
	private final List<NPC> despawnedNpcsThisTick = new ArrayList<>();

	/**
	 * World locations of graphics object which indicate that an
	 * NPC teleported that were played this tick.
	 */
	private final Set<WorldPoint> teleportGraphicsObjectSpawnedThisTick = new HashSet<>();

	/**
	 * The players location on the last game tick.
	 */
	private WorldPoint lastPlayerLocation;

	/**
	 * When hopping worlds, NPCs can spawn without them actually respawning,
	 * so we would not want to mark it as a real spawn in those cases.
	 */
	private boolean skipNextSpawnCheck = false;

	@Provides
	NpcIndicatorsConfigTwo provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcIndicatorsConfigTwo.class);
	}



	@Override
	protected void startUp() throws Exception
	{
//		attackAnimations.add(5849);
//		attackAnimations.add(422);
//		attackAnimations.add(6188);

		overlayManager.add(npcSceneOverlay);
		overlayManager.add(npcMinimapOverlay);
		keyManager.registerKeyListener(inputListener);
		npcAttackAnimations = getNPCAttackAnimations();
		highlights = getHighlights();
		npcSceneOverlay.setColor(config.getHighlightColor());
		clientThread.invoke(() ->
		{
			skipNextSpawnCheck = true;
			rebuildAllNpcs();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(npcSceneOverlay);
		overlayManager.remove(npcMinimapOverlay);
		deadNpcsToDisplay.clear();
		memorizedNpcs.clear();
		spawnedNpcsThisTick.clear();
		despawnedNpcsThisTick.clear();
		teleportGraphicsObjectSpawnedThisTick.clear();
		npcTags.clear();
		highlightedNpcs.clear();
		npcAttackAnimations.clear();
		keyManager.unregisterKeyListener(inputListener);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN ||
			event.getGameState() == GameState.HOPPING)
		{
			highlightedNpcs.clear();
			npcAttackAnimations.clear();
			deadNpcsToDisplay.clear();
			memorizedNpcs.forEach((id, npc) -> npc.setDiedOnTick(-1));
			lastPlayerLocation = null;
			skipNextSpawnCheck = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals("npcindicators"))
		{
			return;
		}

		highlights = getHighlights();
		npcAttackAnimations = getNPCAttackAnimations();
		npcSceneOverlay.setColor(config.getHighlightColor());
		rebuildAllNpcs();
	}

	@Subscribe
	public void onFocusChanged(FocusChanged focusChanged)
	{
		if (!focusChanged.isFocused())
		{
			hotKeyPressed = false;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int type = event.getType();

		if (type >= MENU_ACTION_DEPRIORITIZE_OFFSET)
		{
			type -= MENU_ACTION_DEPRIORITIZE_OFFSET;
		}

		if (config.highlightMenuNames() &&
			NPC_MENU_ACTIONS.contains(MenuAction.of(type)) &&
			highlightedNpcs.stream().anyMatch(npc -> npc.getIndex() == event.getIdentifier()))
		{
			MenuEntry[] menuEntries = client.getMenuEntries();
			final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
			final String target = ColorUtil.prependColorTag(Text.removeTags(event.getTarget()), config.getHighlightColor());
			menuEntry.setTarget(target);
			client.setMenuEntries(menuEntries);
		}
		else if (hotKeyPressed && type == MenuAction.EXAMINE_NPC.getId())
		{
			// Add tag option
			MenuEntry[] menuEntries = client.getMenuEntries();
			menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);
			final MenuEntry tagEntry = menuEntries[menuEntries.length - 1] = new MenuEntry();
			tagEntry.setOption(highlightedNpcs.stream().anyMatch(npc -> npc.getIndex() == event.getIdentifier()) ? UNTAG : TAG);
			tagEntry.setTarget(event.getTarget());
			tagEntry.setParam0(event.getActionParam0());
			tagEntry.setParam1(event.getActionParam1());
			tagEntry.setIdentifier(event.getIdentifier());
			tagEntry.setType(MenuAction.RUNELITE.getId());
			client.setMenuEntries(menuEntries);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked click)
	{
		if (click.getMenuAction() != MenuAction.RUNELITE ||
			!(click.getMenuOption().equals(TAG) || click.getMenuOption().equals(UNTAG)))
		{
			return;
		}

		final int id = click.getId();
		final boolean removed = npcTags.remove(id);
		final NPC[] cachedNPCs = client.getCachedNPCs();
		final NPC npc = cachedNPCs[id];

		if (npc == null || npc.getName() == null)
		{
			return;
		}

		if (removed)
		{
			highlightedNpcs.remove(npc);
			memorizedNpcs.remove(npc.getIndex());
		}
		else
		{
			memorizeNpc(npc);
			npcTags.add(id);
			highlightedNpcs.add(npc);
		}

		click.consume();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		final NPC npc = npcSpawned.getNpc();
		final String npcName = npc.getName();

		if (npcName == null)
		{
			return;
		}

		if (npcTags.contains(npc.getIndex()))
		{
			memorizeNpc(npc);
			highlightedNpcs.add(npc);
			spawnedNpcsThisTick.add(npc);
			return;
		}

		for (String highlight : highlights)
		{
			if (WildcardMatcher.matches(highlight, npcName))
			{
				memorizeNpc(npc);
				highlightedNpcs.add(npc);
				spawnedNpcsThisTick.add(npc);
				break;
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		final NPC npc = npcDespawned.getNpc();

		if (memorizedNpcs.containsKey(npc.getIndex()))
		{
			despawnedNpcsThisTick.add(npc);
		}

		highlightedNpcs.remove(npc);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		final GraphicsObject go = event.getGraphicsObject();

		if (go.getId() == GraphicID.GREY_BUBBLE_TELEPORT)
		{
			teleportGraphicsObjectSpawnedThisTick.add(WorldPoint.fromLocal(client, go.getLocation()));
		}
	}

	@Getter @Setter
	private int counter = 0;

	@Getter @Setter
	HashMap<Actor, Integer> lastAttacks = new HashMap<>();

	@Subscribe
	public void onGameTick(GameTick event)
	{
		removeOldHighlightedRespawns();
		validateSpawnedNpcs();
		lastTickUpdate = Instant.now();
		lastPlayerLocation = client.getLocalPlayer().getWorldLocation();

		if (interactingActors.isEmpty()) {
			return;
		}

		for (Actor a: interactingActors) {
			int attackStart = attackStarts.get(a);
			int attackSpeed = attackSpeeds.get(a);

//			System.out.println(a.getName() + " Attack Speed: " + Integer.toString(attackSpeed));

			if (getCounter() - lastAttacks.get(a) > 10) {
				npcSceneOverlay.actorColors.put(a, config.getHighlightColor());
			} else if (((getCounter() - attackStart) % attackSpeed) == 0) {
				npcSceneOverlay.actorColors.put(a, config.getHighlightColorAttack());
			} else if (((getCounter() - attackStart) % attackSpeed) == attackSpeed - 1) {
//				System.out.println("Current Counter: " + getCounter());
				npcSceneOverlay.actorColors.put(a, config.getHighlightColorWarning());
			} else {
				npcSceneOverlay.actorColors.put(a, config.getHighlightColor());
			}
		}

		setCounter(getCounter() + 1);
	}

	@Getter @Setter
	private Boolean interactionStarted = false;

	@Getter @Setter
	Set<Actor> interactingActors = new HashSet<>();

	@Subscribe
	public void onInteractingChanged(InteractingChanged event) {
		if (highlightedNpcs.contains(event.getSource())) {
			if (event.getTarget() == client.getLocalPlayer()) {
				setInteractionStarted(Boolean.TRUE);
				attackStarts.put(event.getSource(), getCounter());
				lastAttacks.put(event.getSource(), getCounter());
				interactingActors.add(event.getSource());
				if (!attackSpeeds.containsKey(event.getSource())) {
					attackSpeeds.put(event.getSource(), 4);
				}
			} else {
				setInteractionStarted(Boolean.FALSE);
				npcSceneOverlay.actorColors.put(event.getSource(), config.getHighlightColor());
				interactingActors.remove(event.getSource());
				interactingActors.remove(event.getTarget());
			}
		}
	}

	@Getter @Setter
	HashMap<Actor, Integer> attackStarts = new HashMap<>();

	@Getter @Setter
	HashMap<Actor, Integer> attackSpeeds = new HashMap<>();

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if (highlightedNpcs.contains(event.getActor())) {
			if (npcAttackAnimations.contains(Integer.toString(event.getActor().getAnimation()))) {
				lastAttacks.put(event.getActor(), getCounter());
				if (attackStarts.containsKey(event.getActor())) {
					int attackSpeed = getCounter() - attackStarts.get(event.getActor());
					if (attackSpeed <= 10) {
						attackSpeeds.put(event.getActor(), attackSpeed);
					}
				}
				attackStarts.put(event.getActor(), getCounter());
//				System.out.println("Attack start: " + getAttackStart());
			}
		}
	}

//	@Subscribe
//	public void onNpcActionChanged(NpcActionChanged event) {
//		NPCComposition npcComposition = event.getNpcComposition();
//		if (highlightedNpcs.contains(npcComposition)) {
//			printNPCComp(npcComposition, event.getIdx());
//			System.out.println("\n\n\n\n");
//			printNPCComp(npcComposition.transform(), 1);
//		}
//	}

	private void printNPCComp(NPCComposition npcComposition, int index) {
		System.out.println("Raw index of changed action: " + index);
		System.out.println("NpcComposition: " + npcComposition.toString());
		System.out.println("Npc Name: " + npcComposition.getName());
		for (int i = 0; i < npcComposition.getModels().length; i++) {
			System.out.println("Model " + i + " id: " + Integer.toString(npcComposition.getModels()[i]));
		}
		for (int i = 0; i < npcComposition.getActions().length; i++) {
			System.out.println("Action " + i + " id: " + npcComposition.getActions()[i]);
		}
		System.out.println("Action changed: " + npcComposition.getActions()[index]);
		System.out.println("Is Clickable: " + npcComposition.isClickable());
		System.out.println("Is Interactible: " + npcComposition.isInteractible());
		System.out.println("Is Visible: " + npcComposition.isVisible());
		System.out.println("NPC ID: " + npcComposition.getId());
		System.out.println("NPC Combat Lvl: " + npcComposition.getCombatLevel());
		for (int i = 0; i < npcComposition.getConfigs().length; i++) {
			System.out.println("Config " + i + " id: " + Integer.toString(npcComposition.getConfigs()[i]));
		}
		System.out.println("NPC Size: " + npcComposition.getSize());
		System.out.println("HeadIcon: " + npcComposition.getOverheadIcon());
	}

	private static boolean isInViewRange(WorldPoint wp1, WorldPoint wp2)
	{
		int distance = wp1.distanceTo(wp2);
		return distance < MAX_ACTOR_VIEW_RANGE;
	}

	private static WorldPoint getWorldLocationBehind(NPC npc)
	{
		final int orientation = npc.getOrientation() / 256;
		int dx = 0, dy = 0;

		switch (orientation)
		{
			case 0: // South
				dy = -1;
				break;
			case 1: // Southwest
				dx = -1;
				dy = -1;
				break;
			case 2: // West
				dx = -1;
				break;
			case 3: // Northwest
				dx = -1;
				dy = 1;
				break;
			case 4: // North
				dy = 1;
				break;
			case 5: // Northeast
				dx = 1;
				dy = 1;
				break;
			case 6: // East
				dx = 1;
				break;
			case 7: // Southeast
				dx = 1;
				dy = -1;
				break;
		}

		final WorldPoint currWP = npc.getWorldLocation();
		return new WorldPoint(currWP.getX() - dx, currWP.getY() - dy, currWP.getPlane());
	}

	private void memorizeNpc(NPC npc)
	{
		final int npcIndex = npc.getIndex();
		memorizedNpcs.putIfAbsent(npcIndex, new MemorizedNpcTwo(npc));
	}

	private void removeOldHighlightedRespawns()
	{
		deadNpcsToDisplay.values().removeIf(x -> x.getDiedOnTick() + x.getRespawnTime() <= client.getTickCount() + 1);
	}

	@VisibleForTesting
	List<String> getHighlights()
	{
		final String configNpcs = config.getNpcToHighlight().toLowerCase();

		if (configNpcs.isEmpty())
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(configNpcs);
	}

	List<String> getNPCAttackAnimations() {
		final String configNpcs = config.getNpcAnimationIDs();

		if (configNpcs.isEmpty())
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(configNpcs);
	}

	private void rebuildAllNpcs()
	{
		highlightedNpcs.clear();

		if (client.getGameState() != GameState.LOGGED_IN &&
			client.getGameState() != GameState.LOADING)
		{
			// NPCs are still in the client after logging out,
			// but we don't want to highlight those.
			return;
		}

		outer:
		for (NPC npc : client.getNpcs())
		{
			final String npcName = npc.getName();

			if (npcName == null)
			{
				continue;
			}

			if (npcTags.contains(npc.getIndex()))
			{
				highlightedNpcs.add(npc);
				continue;
			}

			for (String highlight : highlights)
			{
				if (WildcardMatcher.matches(highlight, npcName))
				{
					memorizeNpc(npc);
					highlightedNpcs.add(npc);
					continue outer;
				}
			}

			// NPC is not highlighted
			memorizedNpcs.remove(npc.getIndex());
		}
	}

	private void validateSpawnedNpcs()
	{
		if (skipNextSpawnCheck)
		{
			skipNextSpawnCheck = false;
		}
		else
		{
			for (NPC npc : despawnedNpcsThisTick)
			{
				if (!teleportGraphicsObjectSpawnedThisTick.isEmpty())
				{
					if (teleportGraphicsObjectSpawnedThisTick.contains(npc.getWorldLocation()))
					{
						// NPC teleported away, so we don't want to add the respawn timer
						continue;
					}
				}

				if (isInViewRange(client.getLocalPlayer().getWorldLocation(), npc.getWorldLocation()))
				{
					final MemorizedNpcTwo mn = memorizedNpcs.get(npc.getIndex());

					if (mn != null)
					{
						mn.setDiedOnTick(client.getTickCount() + 1); // This runs before tickCounter updates, so we add 1

						if (!mn.getPossibleRespawnLocations().isEmpty())
						{
							log.debug("Starting {} tick countdown for {}", mn.getRespawnTime(), mn.getNpcName());
							deadNpcsToDisplay.put(mn.getNpcIndex(), mn);
						}
					}
				}
			}

			for (NPC npc : spawnedNpcsThisTick)
			{
				if (!teleportGraphicsObjectSpawnedThisTick.isEmpty())
				{
					if (teleportGraphicsObjectSpawnedThisTick.contains(npc.getWorldLocation()) ||
						teleportGraphicsObjectSpawnedThisTick.contains(getWorldLocationBehind(npc)))
					{
						// NPC teleported here, so we don't want to update the respawn timer
						continue;
					}
				}

				if (lastPlayerLocation != null && isInViewRange(lastPlayerLocation, npc.getWorldLocation()))
				{
					final MemorizedNpcTwo mn = memorizedNpcs.get(npc.getIndex());

					if (mn.getDiedOnTick() != -1)
					{
						final int respawnTime = client.getTickCount() + 1 - mn.getDiedOnTick();

						// By killing a monster and leaving the area before seeing it again, an erroneously lengthy
						// respawn time can be recorded. Thus, if the respawn time is already set and is greater than
						// the observed time, assume that the lower observed respawn time is correct.
						if (mn.getRespawnTime() == -1 || respawnTime < mn.getRespawnTime())
						{
							mn.setRespawnTime(respawnTime);
						}

						mn.setDiedOnTick(-1);
					}

					final WorldPoint npcLocation = npc.getWorldLocation();

					// An NPC can move in the same tick as it spawns, so we also have
					// to consider whatever tile is behind the npc
					final WorldPoint possibleOtherNpcLocation = getWorldLocationBehind(npc);

					mn.getPossibleRespawnLocations().removeIf(x ->
						x.distanceTo(npcLocation) != 0 && x.distanceTo(possibleOtherNpcLocation) != 0);

					if (mn.getPossibleRespawnLocations().isEmpty())
					{
						mn.getPossibleRespawnLocations().add(npcLocation);
						mn.getPossibleRespawnLocations().add(possibleOtherNpcLocation);
					}
				}
			}
		}

		spawnedNpcsThisTick.clear();
		despawnedNpcsThisTick.clear();
		teleportGraphicsObjectSpawnedThisTick.clear();
	}
}
