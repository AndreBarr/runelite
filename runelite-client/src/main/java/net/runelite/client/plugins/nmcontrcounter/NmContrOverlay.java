package net.runelite.client.plugins.nmcontrcounter;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class NmContrOverlay extends OverlayPanel {
    private static final DecimalFormat FORMAT = new DecimalFormat("#0.0%");
    private static final int PANEL_WIDTH_OFFSET = 10;
    private final NmContrPlugin nmContrPlugin;
    private final Client client;
    private final TooltipManager tooltipManager;

    @Inject
    NmContrOverlay(NmContrPlugin nmContrPlugin, Client client, TooltipManager tooltipManager) {
        super(nmContrPlugin);
        this.nmContrPlugin = nmContrPlugin;
        this.client = client;
        this.tooltipManager = tooltipManager;
    }

    public Dimension render(Graphics2D graphics) {
        if (!this.nmContrPlugin.isShouldrender()) {
            return null;
        } else {
            int maxWidth = 129;
            FontMetrics fontMetrics = graphics.getFontMetrics();
            String left = "Total";
            String right = FORMAT.format((double)this.nmContrPlugin.getTotalperc());
            maxWidth = Math.max(maxWidth, fontMetrics.stringWidth(left) + fontMetrics.stringWidth(right));
            this.panelComponent.getChildren().add(LineComponent.builder().left(left).right(right).build());
            left = "Boss";
            right = FORMAT.format((double)this.nmContrPlugin.getBossperc());
            maxWidth = Math.max(maxWidth, fontMetrics.stringWidth(left) + fontMetrics.stringWidth(right));
            this.panelComponent.getChildren().add(LineComponent.builder().left(left).right(right).build());
            left = "Totems";
            right = FORMAT.format((double)this.nmContrPlugin.getTotemperc());
            maxWidth = Math.max(maxWidth, fontMetrics.stringWidth(left) + fontMetrics.stringWidth(right));
            this.panelComponent.getChildren().add(LineComponent.builder().left(left).right(right).build());
            this.panelComponent.setPreferredSize(new Dimension(maxWidth + 10, 0));
            return super.render(graphics);
        }
    }
}
