package com.vdp.core.view;

import com.vdp.core.model.DataGeneratorModule;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** Compact editor matching the documented Data Generator dialog. */
public final class DataGeneratorConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataGeneratorModule module;
    private final JTextField arrayField;
    private final JSpinner fromSpinner;
    private final JSpinner toSpinner;
    private boolean accepted;
    private Point dragOffset;

    public DataGeneratorConfigDialog(JFrame owner, DataGeneratorModule module) {
        super(owner, true);
        this.module = module;
        setUndecorated(true);
        setSize(330, 205);
        setMinimumSize(new Dimension(330, 205));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55), 2));
        root.add(createTitleBar(), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(232, 232, 232));
        form.setBorder(BorderFactory.createEmptyBorder(12, 22, 6, 20));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 5, 4, 5);
        constraints.anchor = GridBagConstraints.WEST;

        arrayField = new JTextField(module.getArrayName(), 15);
        fromSpinner = new JSpinner(new SpinnerNumberModel(
                module.getFrom(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        toSpinner = new JSpinner(new SpinnerNumberModel(
                module.getTo(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        fromSpinner.setPreferredSize(new Dimension(175, 24));
        toSpinner.setPreferredSize(new Dimension(175, 24));

        addRow(form, constraints, 0, "Array:", arrayField);
        addRow(form, constraints, 1, "From:", fromSpinner);
        addRow(form, constraints, 2, "To:", toSpinner);
        root.add(form, BorderLayout.CENTER);
        root.add(createButtons(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    public boolean isAccepted() { return accepted; }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(58, 58, 58));
        titleBar.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 7));

        JLabel title = new JLabel("◆  Data Generator - " + module.getName());
        title.setForeground(new Color(225, 225, 225));
        titleBar.add(title, BorderLayout.WEST);

        JLabel actions = new JLabel("?     —     □     ×");
        actions.setForeground(new Color(225, 225, 225));
        actions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getX() > actions.getWidth() - 22) {
                    dispose();
                }
            }
        });
        titleBar.add(actions, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                dragOffset = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                Point screen = event.getLocationOnScreen();
                setLocation(screen.x - dragOffset.x, screen.y - dragOffset.y);
            }
        };
        titleBar.addMouseListener(drag);
        titleBar.addMouseMotionListener(drag);
        return titleBar;
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        JLabel text = new JLabel(label);
        text.setFont(text.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(text, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, constraints);
    }

    private JPanel createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttons.setBackground(new Color(232, 232, 232));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.setPreferredSize(new Dimension(92, 27));
        cancel.setPreferredSize(new Dimension(92, 27));
        ok.setBackground(new Color(111, 196, 56));
        ok.setForeground(Color.WHITE);
        ok.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        cancel.setBackground(new Color(170, 170, 170));
        cancel.setForeground(Color.WHITE);
        cancel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        ok.addActionListener(event -> accept());
        cancel.addActionListener(event -> dispose());
        buttons.add(ok);
        buttons.add(cancel);
        return buttons;
    }

    private void accept() {
        String arrayName = arrayField.getText().trim();
        int from = (Integer) fromSpinner.getValue();
        int to = (Integer) toSpinner.getValue();
        if (arrayName.isEmpty()) {
            showError("Array name cannot be empty.");
            return;
        }
        if (from > to) {
            showError("From must be less than or equal to To.");
            return;
        }
        module.setArrayName(arrayName);
        module.setFrom(from);
        module.setTo(to);
        accepted = true;
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Data Generator", JOptionPane.ERROR_MESSAGE);
    }
}
