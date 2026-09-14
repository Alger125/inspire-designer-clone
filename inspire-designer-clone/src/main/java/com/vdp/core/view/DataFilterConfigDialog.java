package com.vdp.core.view;

import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataFilterModule.Condition;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Editor for the first Data Filter criterion from manual section 5.13. */
final class DataFilterConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataFilterModule module;
    private final JTextField fieldName;
    private final JComboBox<Condition> condition;
    private final JTextField filterValue;
    private final JCheckBox multipleValues;
    private final JCheckBox invertCondition;
    private boolean accepted;
    private Point dragOffset;

    DataFilterConfigDialog(JFrame owner, DataFilterModule module) {
        super(owner, true);
        this.module = module;
        setUndecorated(true);
        setSize(430, 290);
        setMinimumSize(new Dimension(430, 290));
        setLocationRelativeTo(owner);

        fieldName = new JTextField(module.getFieldName(), 20);
        condition = new JComboBox<>(Condition.values());
        condition.setSelectedItem(module.getCondition());
        filterValue = new JTextField(module.getFilterValue(), 20);
        multipleValues = new JCheckBox(
                "Allow multiple values", module.isAllowMultipleValues());
        invertCondition = new JCheckBox(
                "Invert condition", module.isInvertCondition());

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55), 2));
        root.add(createTitleBar(), BorderLayout.NORTH);
        root.add(createForm(), BorderLayout.CENTER);
        root.add(createButtons(), BorderLayout.SOUTH);
        setContentPane(root);
        condition.addActionListener(event -> updateEnabledState());
        updateEnabledState();
    }

    boolean isAccepted() { return accepted; }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(58, 58, 58));
        titleBar.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 7));
        JLabel title = new JLabel("◆  Data Filter - " + module.getName());
        title.setForeground(new Color(225, 225, 225));
        titleBar.add(title, BorderLayout.WEST);

        JLabel actions = new JLabel("?     —     □     ×");
        actions.setForeground(new Color(225, 225, 225));
        actions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getX() > actions.getWidth() - 22) dispose();
            }
        });
        titleBar.add(actions, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) { dragOffset = event.getPoint(); }

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

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(232, 232, 232));
        form.setBorder(BorderFactory.createEmptyBorder(12, 24, 6, 24));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        addRow(form, constraints, 0, "Field:", fieldName);
        addRow(form, constraints, 1, "Condition:", condition);
        addRow(form, constraints, 2, "Value:", filterValue);
        constraints.gridx = 1;
        constraints.gridy = 3;
        form.add(multipleValues, constraints);
        constraints.gridy = 4;
        form.add(invertCondition, constraints);
        return form;
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
        cancel.setBackground(new Color(170, 170, 170));
        cancel.setForeground(Color.WHITE);
        ok.addActionListener(event -> accept());
        cancel.addActionListener(event -> dispose());
        buttons.add(ok);
        buttons.add(cancel);
        return buttons;
    }

    private void updateEnabledState() {
        Condition selected = (Condition) condition.getSelectedItem();
        boolean hasCondition = selected != null && selected != Condition.NONE;
        fieldName.setEnabled(hasCondition);
        filterValue.setEnabled(hasCondition);
        multipleValues.setEnabled(selected == Condition.EQUAL_TO
                || selected == Condition.CONTAINS
                || selected == Condition.BEGINS_WITH);
    }

    private void accept() {
        String oldField = module.getFieldName();
        Condition oldCondition = module.getCondition();
        String oldValue = module.getFilterValue();
        boolean oldMultiple = module.isAllowMultipleValues();
        boolean oldInvert = module.isInvertCondition();

        module.setFieldName(fieldName.getText().trim());
        module.setCondition((Condition) condition.getSelectedItem());
        module.setFilterValue(filterValue.getText().trim());
        module.setAllowMultipleValues(multipleValues.isSelected());
        module.setInvertCondition(invertCondition.isSelected());
        java.util.List<String> errors = module.validate();
        if (!errors.isEmpty()) {
            module.setFieldName(oldField);
            module.setCondition(oldCondition);
            module.setFilterValue(oldValue);
            module.setAllowMultipleValues(oldMultiple);
            module.setInvertCondition(oldInvert);
            JOptionPane.showMessageDialog(
                    this, String.join("\n", errors), "Data Filter", JOptionPane.ERROR_MESSAGE);
            return;
        }
        accepted = true;
        dispose();
    }
}
