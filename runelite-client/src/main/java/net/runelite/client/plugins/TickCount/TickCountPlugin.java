package net.runelite.client.plugins.TickCount;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "TickCounter",
        description = "Counts Game Ticks"
)

public class TickCountPlugin extends Plugin {

    @Getter @Setter
    private int tickCount = 0;

    @Getter @Setter
    private int count = 0;

    private TickCounter counter;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TickCountOverlay myOverLay;

    @Inject
    private TickCountConfig myConfig;

    @Inject
    private InfoBoxManager infoBoxManager;

    @Provides
    TickCountConfig getMyConfig(ConfigManager configManager) {
        return configManager.getConfig(TickCountConfig.class);
    }

    @Subscribe
    public void onConfigChanged (ConfigChanged configChanged) {
        setTickCount(myConfig.tickCount());
    }

    @Override
    public void startUp(){
//        overlayManager.add(myOverLay);

        counter = new TickCounter(this, 1);
        infoBoxManager.addInfoBox(counter);
    }

    @Override
    public void shutDown(){
//        overlayManager.remove(myOverLay);

        infoBoxManager.removeInfoBox(counter);
        counter = null;
    }

    @Subscribe
    public void onGameTick (GameTick event) {
        updateCount();
    }

    private void updateCount () {
        counter.setCount((counter.getCount() % getTickCount()) + 1);
    }

}
