package net.runelite.client.plugins.MyFirstPlugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("myconfig")
public interface MyFirstPluginConfig extends Config {

    @ConfigItem(
            position = 1,
            keyName = "numTicks",
            name = "Number of Ticks",
            description = "The number of ticks to count to."
    )
    default int tickCount() { return 4; }
}
