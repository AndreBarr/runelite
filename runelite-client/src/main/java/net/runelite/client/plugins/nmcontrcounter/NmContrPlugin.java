package net.runelite.client.plugins.nmcontrcounter;

import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Hitsplat.HitsplatType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Nightmare Contribution Counter",
        description = "Counts your contribution to the nightmare kill",
        enabledByDefault = true
)
public class NmContrPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private NmContrOverlay nmContrOverlay;
    private int idleticks;
    private boolean shouldrender;
    private int total;
    private int totemtotal;
    private int bosstotal;
    private int p;
    private int totemp;
    private int bossp;
    private float totalperc;
    private float bossperc;
    private float totemperc;

    public NmContrPlugin() {
    }

    protected void startUp() {
        this.total = 0;
        this.totemtotal = 0;
        this.bosstotal = 0;
        this.p = 0;
        this.totemp = 0;
        this.bossp = 0;
        this.totalperc = 0.0F;
        this.bossperc = 0.0F;
        this.totemperc = 0.0F;
        this.overlayManager.add(this.nmContrOverlay);
    }

    protected void shutDown() {
        this.overlayManager.remove(this.nmContrOverlay);
    }

    @Subscribe
    private void onHitsplatApplied(HitsplatApplied event) {
        Actor actor = event.getActor();
        if (actor instanceof NPC) {
            Hitsplat hitsplat = event.getHitsplat();
            HitsplatType type = hitsplat.getHitsplatType();
            if (type != HitsplatType.HEAL && type != HitsplatType.DAMAGE_OTHER_WHITE) {
                int damage = hitsplat.getAmount();
                if (damage != 800) {
                    this.total += damage;
                    if (hitsplat.isMine()) {
                        this.p += damage;
                    }

                    if (this.total != 0) {
                        this.totalperc = (float)this.p / (float)this.total;
                    }

                    if (event.getActor().getName().contentEquals("The Nightmare")) {
                        this.idleticks = 0;
                        this.bosstotal += damage;
                        if (hitsplat.isMine()) {
                            this.bossp += damage;
                        }

                        if (this.bosstotal != 0) {
                            this.bossperc = (float)this.bossp / (float)this.bosstotal;
                        }
                    } else if (event.getActor().getName().contentEquals("<col=00ffff>Totem</col>")) {
                        this.idleticks = 0;
                        this.totemtotal += damage;
                        if (hitsplat.isMine()) {
                            this.totemp += damage;
                        }

                        if (this.totemtotal != 0) {
                            this.totemperc = (float)this.totemp / (float)this.totemtotal;
                        }
                    }

                }
            }
        }
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        ++this.idleticks;
        if (this.client.getLocalPlayer() != null) {
            boolean preshouldrender = false;

            for(int i = 0; i < this.client.getMapRegions().length; ++i) {
                if (this.client.getMapRegions()[i] == 15258) {
                    preshouldrender = true;
                }
            }

            this.shouldrender = preshouldrender;
        }

        if (this.idleticks > 65) {
            this.total = 0;
            this.totemtotal = 0;
            this.bosstotal = 0;
            this.p = 0;
            this.totemp = 0;
            this.bossp = 0;
            this.totalperc = 0.0F;
            this.bossperc = 0.0F;
            this.totemperc = 0.0F;
        }

    }

    boolean isShouldrender() {
        return this.shouldrender;
    }

    int getTotal() {
        return this.total;
    }

    int getTotemtotal() {
        return this.totemtotal;
    }

    int getBosstotal() {
        return this.bosstotal;
    }

    int getP() {
        return this.p;
    }

    int getTotemp() {
        return this.totemp;
    }

    int getBossp() {
        return this.bossp;
    }

    float getTotalperc() {
        return this.totalperc;
    }

    float getBossperc() {
        return this.bossperc;
    }

    float getTotemperc() {
        return this.totemperc;
    }
}
