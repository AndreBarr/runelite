package net.runelite.client.plugins.MyFirstPlugin;

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
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "MyFirstPlugin",
        description = "My first plugin"
)

public class MyFirstPlugin extends Plugin {

    @Getter @Setter
    private int tickCount = 0;

    @Getter @Setter
    private int count = 0;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MyFirstPluginOverlay myOverLay;

    @Inject
    private MyFirstPluginConfig myConfig;

    @Provides
    MyFirstPluginConfig getMyConfig(ConfigManager configManager) {
        return configManager.getConfig(MyFirstPluginConfig.class);
    }

    @Subscribe
    public void onConfigChanged (ConfigChanged configChanged) {
        setTickCount(myConfig.tickCount());
    }

    @Override
    public void startUp(){
        overlayManager.add(myOverLay);
    }

    @Override
    public void shutDown(){
        overlayManager.remove(myOverLay);
    }

    @Subscribe
    public void onGameTick (GameTick event) {
        setCount((getCount() % getTickCount()) + 1);
    }

}
