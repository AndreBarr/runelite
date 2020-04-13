package net.runelite.client.plugins.MyFirstPlugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class MyFirstPluginOverlay extends Overlay {

    private MyFirstPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public MyFirstPluginOverlay(MyFirstPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();
//        panelComponent.setBackgroundColor(Color.GREEN);

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(Integer.toString(plugin.getCount()))
                .color(Color.RED)
                .build());

        return panelComponent.render(graphics);
    }
}
