package net.runelite.client.plugins.MyFirstPlugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.NpcActionChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "MyFirstPlugin",
        description = "My first plugin"
)

public class MyFirstPlugin extends Plugin {

    @Inject
    private Client client;

    @Subscribe
    public void onNpcActionChanged (NpcActionChanged event) {
        int actionIndex = event.getIdx();
        NPCComposition composition = event.getNpcComposition();

        if (composition.getId() == 2854) {
            System.out.println("List of Actions:");
            for (String s: composition.getActions()) {
                System.out.println(s);
            }
            System.out.println("");

            System.out.println("Action Changed: " + composition.getActions()[actionIndex]);
        }
    }
}
