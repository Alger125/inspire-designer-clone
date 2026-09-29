package com.vdp.core.model.layout;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class TextElement extends LayoutElement {
    private String text;
    private Font font;
    private Color textColor;

    public TextElement(String name, int x, int y, int width, int height) {
        super(name, x, y, width, height);
        this.text = "Double click to edit";
        this.font = new Font("Arial", Font.PLAIN, 12);
        this.textColor = Color.BLACK;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    @Override
    public void paint(Graphics2D g2) {
        // Draw background (usually transparent, but maybe slightly visible when editing)
        g2.setColor(new Color(255, 255, 255, 150));
        g2.fillRect(x, y, width, height);

        // Draw light border indicating it's a text box (Inspire has very faint borders for Flow Areas)
        g2.setColor(new Color(200, 200, 200));
        g2.drawRect(x, y, width, height);

        // Draw text
        if (!text.isEmpty()) {
            g2.setFont(font);
            g2.setColor(textColor);
            
            // Basic multiline drawing
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int lineHeight = fm.getHeight();
            int currentY = y + fm.getAscent() + 2;
            
            for (String line : text.split("\n")) {
                if (currentY > y + height) break; // Clip vertically
                g2.drawString(line, x + 2, currentY);
                currentY += lineHeight;
            }
        }

        // Draw handles if selected
        paintSelectionHandles(g2);
    }
}
