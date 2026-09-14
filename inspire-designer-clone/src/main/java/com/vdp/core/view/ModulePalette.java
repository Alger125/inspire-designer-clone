package com.vdp.core.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;

final class ModulePalette extends JPanel {
    private static final long serialVersionUID = 1L;

    ModulePalette() {
        super(new BorderLayout());
        setBackground(InspireTheme.PANEL);

        JLabel title = new JLabel("Modules");
        title.setForeground(InspireTheme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 4));
        add(title, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(InspireTheme.PANEL);

        addCategory(list, "Data Inputs", InspireTheme.DATA_INPUT);
        addItem(list, "Data Input", InspireTheme.DATA_INPUT, true);
        addItem(list, "Internal Data Input", InspireTheme.DATA_INPUT, false);
        addItem(list, "Script Data Input", InspireTheme.DATA_INPUT, false);
        addItem(list, "XML Data Input", InspireTheme.DATA_INPUT, false);
        addItem(list, "ODBC Data Input", InspireTheme.DATA_INPUT, false);
        addItem(list, "Param Input", InspireTheme.DATA_INPUT, false);
        addItem(list, "HTTP JSON Input", InspireTheme.DATA_INPUT, true);
        addItem(list, "SAP Data Input", InspireTheme.DATA_INPUT, false);
        addItem(list, "Data Generator", InspireTheme.DATA_INPUT, true);
        addItem(list, "Line Data Input", InspireTheme.DATA_INPUT, false);

        addCategory(list, "Data Processing", InspireTheme.DATA_PROCESSING);
        addItem(list, "Data Filter", InspireTheme.DATA_PROCESSING, true);
        addItem(list, "Data Sorter", InspireTheme.DATA_PROCESSING, false);
        addItem(list, "Data Transformer", InspireTheme.DATA_PROCESSING, false);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);
    }

    private void addCategory(JPanel parent, String text, Color color) {
        JLabel category = new JLabel("▾ " + text);
        category.setOpaque(true);
        category.setBackground(color);
        category.setForeground(Color.WHITE);
        category.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 4));
        category.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
        category.setPreferredSize(new Dimension(230, 23));
        parent.add(category);
    }

    private void addItem(JPanel parent, String type, Color color, boolean implemented) {
        JPanel row = new JPanel(new BorderLayout(7, 0));
        row.setBackground(InspireTheme.PANEL);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(232, 232, 232)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setPreferredSize(new Dimension(230, 28));

        PaletteGlyph glyph = new PaletteGlyph(color, implemented);
        glyph.setPreferredSize(new Dimension(27, 27));
        row.add(glyph, BorderLayout.WEST);

        JLabel label = new JLabel(type);
        label.setForeground(implemented ? InspireTheme.TEXT : new Color(165, 165, 165));
        row.add(label, BorderLayout.CENTER);

        if (implemented) {
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.setToolTipText("Drag to the workflow area");
            row.setTransferHandler(new TransferHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                protected java.awt.datatransfer.Transferable createTransferable(JComponent component) {
                    return new StringSelection(type);
                }

                @Override
                public int getSourceActions(JComponent component) {
                    return COPY;
                }
            });
            row.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    row.getTransferHandler().exportAsDrag(row, event, TransferHandler.COPY);
                }
            });
        } else {
            row.setToolTipText("Not implemented yet");
        }
        parent.add(row);
    }

    private static final class PaletteGlyph extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final boolean implemented;

        PaletteGlyph(Color color, boolean implemented) {
            this.color = color;
            this.implemented = implemented;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(implemented ? color : new Color(190, 190, 190));
            for (int column = 0; column < 3; column++) {
                g2.drawRect(7 + column * 5, 8, 2, 11);
            }
            g2.dispose();
        }
    }
}
