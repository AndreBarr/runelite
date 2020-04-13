package net.runelite.client.plugins.TickCount;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class TickCountOverlay extends Overlay {

    private TickCountPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public TickCountOverlay(TickCountPlugin plugin) {
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
