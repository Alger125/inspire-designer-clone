package com.vdp.core.view;

import com.vdp.core.model.InspireModule;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

final class WorkflowNode extends JComponent {
    private static final long serialVersionUID = 1L;
    static final int WIDTH = 94;
    static final int HEIGHT = 103;

    private final String moduleType;
    private final InspireModule module;
    private final Color accent;
    private final boolean acceptsInput;
    private boolean selected;

    WorkflowNode(String moduleType, InspireModule module, Color accent, boolean acceptsInput) {
        this.moduleType = moduleType;
        this.module = module;
        this.accent = accent;
        this.acceptsInput = acceptsInput;
        setSize(WIDTH, HEIGHT);
        setOpaque(false);
        setToolTipText(moduleType);
    }

    InspireModule getModule() { return module; }
    boolean acceptsInput() { return acceptsInput; }
    void setNodeSelected(boolean value) { selected = value; repaint(); }

    Point inputPoint() { return new Point(13, 43); }
    Point outputPoint() { return new Point(WIDTH - 13, 43); }

    boolean isOutputHit(Point point) {
        Point output = outputPoint();
        return point.distance(output) <= 10;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int iconX = 21;
        int iconY = 9;
        int iconWidth = 52;
        int iconHeight = 68;

        if (selected) {
            g2.setColor(new Color(90, 135, 205, 45));
            g2.fillRoundRect(3, 2, WIDTH - 6, HEIGHT - 3, 4, 4);
            g2.setColor(new Color(90, 135, 205));
            g2.drawRoundRect(3, 2, WIDTH - 7, HEIGHT - 4, 4, 4);
        }

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(iconX, iconY, iconWidth, iconHeight, 4, 4);
        g2.setColor(new Color(205, 205, 205));
        g2.drawRoundRect(iconX, iconY, iconWidth, iconHeight, 4, 4);

        paintGlyph(g2, iconX, iconY, iconWidth, iconHeight);

        if (acceptsInput) {
            paintPort(g2, inputPoint());
        }
        paintPort(g2, outputPoint());

        g2.setColor(accent);
        g2.fillRect(9, 78, WIDTH - 18, 19);
        g2.setColor(Color.WHITE);
        FontMetrics metrics = g2.getFontMetrics();
        String label = module.getName();
        int maxWidth = WIDTH - 22;
        while (metrics.stringWidth(label) > maxWidth && label.length() > 4) {
            label = label.substring(0, label.length() - 2);
        }
        if (!label.equals(module.getName())) {
            label += "…";
        }
        g2.drawString(label, (WIDTH - metrics.stringWidth(label)) / 2, 92);
        g2.dispose();
    }

    private void paintPort(Graphics2D g2, Point point) {
        g2.setColor(Color.WHITE);
        g2.fillOval(point.x - 5, point.y - 5, 10, 10);
        g2.setColor(new Color(190, 190, 190));
        g2.drawOval(point.x - 5, point.y - 5, 10, 10);
    }

    private void paintGlyph(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.8f));
        int centerX = x + width / 2;
        int centerY = y + height / 2;

        if (moduleType.equals("Data Generator")) {
            Path2D cube = new Path2D.Double();
            cube.moveTo(centerX, centerY - 17);
            cube.lineTo(centerX + 16, centerY - 8);
            cube.lineTo(centerX + 16, centerY + 10);
            cube.lineTo(centerX, centerY + 19);
            cube.lineTo(centerX - 16, centerY + 10);
            cube.lineTo(centerX - 16, centerY - 8);
            cube.closePath();
            g2.draw(cube);
            g2.drawLine(centerX, centerY, centerX, centerY + 19);
            g2.drawLine(centerX - 16, centerY - 8, centerX, centerY);
            g2.drawLine(centerX + 16, centerY - 8, centerX, centerY);
        } else if (moduleType.equals("Data Filter")) {
            g2.drawLine(centerX - 15, centerY - 14, centerX + 15, centerY - 14);
            g2.drawLine(centerX - 10, centerY - 4, centerX + 10, centerY - 4);
            g2.drawLine(centerX - 5, centerY + 6, centerX + 5, centerY + 6);
            g2.drawLine(centerX, centerY + 6, centerX, centerY + 18);
        } else {
            for (int column = -2; column <= 2; column++) {
                g2.drawRect(centerX + column * 7 - 2, centerY - 16, 4, 32);
            }
        }
    }
}
