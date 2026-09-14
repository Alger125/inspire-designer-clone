package com.vdp.core.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;

final class InspireTheme {
    static final Color TOP_BAR = new Color(58, 58, 58);
    static final Color TOOLBAR = new Color(246, 246, 246);
    static final Color CANVAS = new Color(242, 242, 242);
    static final Color PANEL = new Color(252, 252, 252);
    static final Color BORDER = new Color(199, 199, 199);
    static final Color TEXT = new Color(90, 90, 90);
    static final Color DATA_INPUT = new Color(79, 194, 207);
    static final Color DATA_PROCESSING = new Color(220, 66, 91);
    static final Color LAYOUT = new Color(139, 188, 35);
    static final Font UI_FONT = new Font("Dialog", Font.PLAIN, 12);

    private InspireTheme() {}

    static void install() {
        UIManager.put("Label.font", UI_FONT);
        UIManager.put("Button.font", UI_FONT);
        UIManager.put("Menu.font", UI_FONT);
        UIManager.put("MenuItem.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("Spinner.font", UI_FONT);
        UIManager.put("ToolTip.font", UI_FONT);
    }

    static JButton toolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(2, 7, 2, 7));
        button.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        button.setBackground(TOOLBAR);
        return button;
    }
}
