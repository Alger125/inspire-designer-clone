package com.vdp.core.model.layout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Base class for all design objects on the Sheet canvas. */
public abstract class LayoutElement {
    protected int x, y, width, height;
    protected boolean selected;
    protected String name;

    public LayoutElement(String name, int x, int y, int width, int height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void paint(Graphics2D g2);

    public boolean contains(int px, int py) {
        return new Rectangle(x, y, width, height).contains(px, py);
    }

    public void setLocation(int dx, int dy) {
        this.x = dx;
        this.y = dy;
    }

    public void setSize(int w, int h) {
        this.width = Math.max(10, w);
        this.height = Math.max(10, h);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    protected void paintSelectionHandles(Graphics2D g2) {
        if (!selected) return;
        
        g2.setColor(new Color(0, 120, 215)); // Inspire-like blue selection
        int size = 6;
        int offset = size / 2;

        // Draw 8 handles
        int[] xs = { x, x + width / 2, x + width, x, x + width, x, x + width / 2, x + width };
        int[] ys = { y, y, y, y + height / 2, y + height / 2, y + height, y + height, y + height };

        for (int i = 0; i < 8; i++) {
            g2.fillRect(xs[i] - offset, ys[i] - offset, size, size);
        }
        
        // Draw dashed selection border
        java.awt.Stroke oldStroke = g2.getStroke();
        g2.setStroke(new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 10, new float[]{3, 3}, 0));
        g2.drawRect(x, y, width, height);
        g2.setStroke(oldStroke);
    }
}
