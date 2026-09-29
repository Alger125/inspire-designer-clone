package com.vdp.core.view.layout;

import com.vdp.core.view.InspireTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

public class SheetEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final PageCanvas pageCanvas;

    public SheetEditorPanel() {
        super(new BorderLayout());
        setBackground(InspireTheme.CANVAS);

        pageCanvas = new PageCanvas();
        
        JScrollPane scroll = new JScrollPane(pageCanvas);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(new Color(230, 230, 230));
        
        // Left toolbar for layout tools
        JToolBar tools = new JToolBar(JToolBar.VERTICAL);
        tools.setFloatable(false);
        tools.setBackground(InspireTheme.PANEL);
        tools.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, InspireTheme.BORDER));
        
        tools.add(InspireTheme.toolbarButton("⬈", "Selección"));
        tools.add(InspireTheme.toolbarButton("T", "Objeto de Texto"));
        tools.add(InspireTheme.toolbarButton("□", "Rectángulo"));
        tools.add(InspireTheme.toolbarButton("🖼", "Imagen"));
        tools.add(InspireTheme.toolbarButton("▦", "Tabla"));

        add(tools, BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
    }
    
    private class PageCanvas extends JPanel {
        private static final long serialVersionUID = 1L;
        // Basic A4 proportion at low dpi
        private final int PAGE_WIDTH = 595;
        private final int PAGE_HEIGHT = 842;
        
        public PageCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(PAGE_WIDTH + 100, PAGE_HEIGHT + 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Center the page horizontally
            int x = Math.max(30, (getWidth() - PAGE_WIDTH) / 2);
            int y = 30;
            
            // Draw drop shadow
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRect(x + 4, y + 4, PAGE_WIDTH, PAGE_HEIGHT);
            
            // Draw white page
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, PAGE_WIDTH, PAGE_HEIGHT);
            
            // Draw page border
            g2.setColor(InspireTheme.BORDER);
            g2.drawRect(x, y, PAGE_WIDTH, PAGE_HEIGHT);
            
            // Placeholder for margins
            g2.setColor(new Color(0, 150, 255, 50));
            g2.drawRect(x + 50, y + 50, PAGE_WIDTH - 100, PAGE_HEIGHT - 100);
            
            // Placeholder text
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("Diseñador de Layout - Página 1", x + 50, y + 45);
            
            g2.dispose();
        }
    }
}
