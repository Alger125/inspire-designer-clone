package com.vdp.core.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.awt.Insets;
import javax.swing.UIManager;

public final class InspireTheme {
    public static final Color TOP_BAR = new Color(58, 58, 58);
    public static final Color TOOLBAR = new Color(246, 246, 246);
    public static final Color CANVAS = new Color(242, 242, 242);
    public static final Color PANEL = new Color(252, 252, 252);
    public static final Color BORDER = new Color(199, 199, 199);
    public static final Color TEXT = new Color(90, 90, 90);
    public static final Color DATA_INPUT = new Color(79, 194, 207);
    public static final Color DATA_PROCESSING = new Color(220, 66, 91);
    public static final Color LAYOUT = new Color(139, 188, 35);
    public static final Font UI_FONT = new Font("Dialog", Font.PLAIN, 12);

    private InspireTheme() {}

    public static void install() {
        UIManager.put("Label.font", UI_FONT);
        UIManager.put("Button.font", UI_FONT);
        UIManager.put("Menu.font", UI_FONT);
        UIManager.put("MenuItem.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("Spinner.font", UI_FONT);
        UIManager.put("ToolTip.font", UI_FONT);
    }


    public static ImageIcon getIcon(String name, int size) {
        URL url = InspireTheme.class.getResource("/icons/" + name + ".png");
        if (url != null) {
            ImageIcon icon = new ImageIcon(url);
            Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        }
        return null; // Fallback to text/emoji if missing
    }

    public static JButton toolbarButton(String text, String tooltip, String iconName) {
        JButton button = new JButton();
        ImageIcon icon = getIcon(iconName, 16);
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText(text);
            button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        }
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setBackground(TOOLBAR);
        button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(220, 225, 235));
                button.setBorder(BorderFactory.createLineBorder(new Color(160, 180, 200)));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(TOOLBAR);
                button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            }
        });
        return button;
    }

    public static JButton toolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(2, 7, 2, 7));
        button.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        button.setBackground(TOOLBAR);
        return button;
    }
}
