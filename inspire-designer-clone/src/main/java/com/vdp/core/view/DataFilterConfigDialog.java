package com.vdp.core.view;

import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataFilterModule.Condition;
import com.vdp.core.model.DataFilterModule.FilterCriterion;
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
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Editor for Data Filter criteria from manual section 5.13. */
final class DataFilterConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataFilterModule module;
    private final List<CriterionRow> rows = new ArrayList<>();
    private final JPanel criteriaPanel;
    
    private final JCheckBox multipleValues;
    private final JCheckBox invertCondition;
    private final JCheckBox createElseOutput;
    
    private boolean accepted;
    private Point dragOffset;

    private class CriterionRow extends JPanel {
        JTextField fieldName = new JTextField(12);
        JComboBox<Condition> condition = new JComboBox<>(Condition.values());
        JTextField filterValue = new JTextField(12);
        JCheckBox ignoreCase = new JCheckBox("Ignore case", true);
        JButton removeBtn = new JButton("X");
        
        CriterionRow(FilterCriterion crit) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setOpaque(false);
            
            if (crit != null) {
                fieldName.setText(crit.getFieldName());
                condition.setSelectedItem(crit.getCondition());
                filterValue.setText(crit.getFilterValue());
                ignoreCase.setSelected(crit.isIgnoreCase());
            }
            
            removeBtn.setPreferredSize(new Dimension(42, 22));
            removeBtn.setMargin(new Insets(0, 0, 0, 0));
            removeBtn.addActionListener(e -> {
                criteriaPanel.remove(this);
                rows.remove(this);
                criteriaPanel.revalidate();
                criteriaPanel.repaint();
            });
            
            add(new JLabel("Field:")); add(fieldName);
            add(new JLabel("Cond:")); add(condition);
            add(new JLabel("Value:")); add(filterValue);
            add(ignoreCase);
            add(removeBtn);
        }
        
        FilterCriterion toCriterion() {
            return new FilterCriterion(
                fieldName.getText().trim(),
                (Condition) condition.getSelectedItem(),
                filterValue.getText().trim(),
                ignoreCase.isSelected()
            );
        }
    }

    DataFilterConfigDialog(JFrame owner, DataFilterModule module) {
        super(owner, true);
        this.module = module;
        setUndecorated(true);
        setSize(780, 420);
        setMinimumSize(new Dimension(680, 320));
        setLocationRelativeTo(owner);

        multipleValues = new JCheckBox("Allow multiple values", module.isAllowMultipleValues());
        multipleValues.setOpaque(false);
        invertCondition = new JCheckBox("Invert condition", module.isInvertCondition());
        invertCondition.setOpaque(false);
        createElseOutput = new JCheckBox("Create else output", module.isCreateElseOutput());
        createElseOutput.setOpaque(false);

        criteriaPanel = new JPanel();
        criteriaPanel.setLayout(new BoxLayout(criteriaPanel, BoxLayout.Y_AXIS));
        criteriaPanel.setBackground(new Color(245, 245, 245));
        
        for (FilterCriterion c : module.getCriteria()) {
            CriterionRow row = new CriterionRow(c);
            rows.add(row);
            criteriaPanel.add(row);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55), 2));
        root.add(createTitleBar(), BorderLayout.NORTH);
        root.add(createForm(), BorderLayout.CENTER);
        root.add(createButtons(), BorderLayout.SOUTH);
        setContentPane(root);
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
        JPanel form = new JPanel(new BorderLayout(10, 10));
        form.setBackground(new Color(232, 232, 232));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        
        JScrollPane scroll = new JScrollPane(criteriaPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        form.add(scroll, BorderLayout.CENTER);
        
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        options.setOpaque(false);
        options.add(multipleValues);
        options.add(invertCondition);
        options.add(createElseOutput);
        
        JButton addBtn = new JButton("+ Add Criterion");
        addBtn.addActionListener(e -> {
            CriterionRow row = new CriterionRow(null);
            rows.add(row);
            criteriaPanel.add(row);
            criteriaPanel.revalidate();
            criteriaPanel.repaint();
        });
        options.add(addBtn);
        
        form.add(options, BorderLayout.SOUTH);
        return form;
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

    private void accept() {
        List<FilterCriterion> oldCriteria = new ArrayList<>(module.getCriteria());
        boolean oldMultiple = module.isAllowMultipleValues();
        boolean oldInvert = module.isInvertCondition();
        boolean oldCreateElse = module.isCreateElseOutput();

        module.getCriteria().clear();
        for (CriterionRow row : rows) {
            module.getCriteria().add(row.toCriterion());
        }
        
        module.setAllowMultipleValues(multipleValues.isSelected());
        module.setInvertCondition(invertCondition.isSelected());
        module.setCreateElseOutput(createElseOutput.isSelected());
        
        List<String> errors = module.validate();
        if (!errors.isEmpty()) {
            module.getCriteria().clear();
            module.getCriteria().addAll(oldCriteria);
            module.setAllowMultipleValues(oldMultiple);
            module.setInvertCondition(oldInvert);
            module.setCreateElseOutput(oldCreateElse);
            JOptionPane.showMessageDialog(
                    this, String.join("\n", errors), "Data Filter", JOptionPane.ERROR_MESSAGE);
            return;
        }
        accepted = true;
        dispose();
    }
}
