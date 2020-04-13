package net.runelite.client.plugins.TickCount;

import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import java.awt.image.BufferedImage;

public class TickCounter extends Counter {

    TickCounter(Plugin plugin, int count) {
        super(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), plugin, count);
        setPriority(InfoBoxPriority.MED);
    }
}
