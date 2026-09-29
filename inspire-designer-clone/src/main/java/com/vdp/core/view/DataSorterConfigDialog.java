package com.vdp.core.view;

import com.vdp.core.model.DataSorterModule;
import com.vdp.core.model.DataSorterModule.ComparisonType;
import com.vdp.core.model.DataSorterModule.Direction;
import com.vdp.core.model.DataSorterModule.NullOrder;
import com.vdp.core.model.DataSorterModule.SortCriterion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

/** Editor for the Data Sorter configuration from manual section 5.14. */
final class DataSorterConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataSorterModule module;
    private final List<CriterionRow> rows = new ArrayList<>();
    private final JPanel criteriaPanel;
    
    private boolean accepted;
    private Point dragOffset;

    private class CriterionRow extends JPanel {
        JTextField fieldName = new JTextField(10);
        JComboBox<Direction> direction = new JComboBox<>(Direction.values());
        JComboBox<ComparisonType> type = new JComboBox<>(ComparisonType.values());
        JCheckBox ignoreCase = new JCheckBox("Ignore case");
        JComboBox<NullOrder> nullOrder = new JComboBox<>(NullOrder.values());
        
        JButton upBtn = new JButton("↑");
        JButton downBtn = new JButton("↓");
        JButton removeBtn = new JButton("X");
        
        CriterionRow(SortCriterion crit) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setOpaque(false);
            
            if (crit != null) {
                fieldName.setText(crit.getFieldName());
                direction.setSelectedItem(crit.getDirection());
                type.setSelectedItem(crit.getComparisonType());
                ignoreCase.setSelected(crit.isIgnoreCase());
                nullOrder.setSelectedItem(crit.getNullOrder());
            } else {
                ignoreCase.setSelected(true);
                nullOrder.setSelectedItem(NullOrder.LAST);
            }
            
            upBtn.setMargin(new Insets(0, 2, 0, 2));
            downBtn.setMargin(new Insets(0, 2, 0, 2));
            removeBtn.setMargin(new Insets(0, 2, 0, 2));
            
            upBtn.addActionListener(e -> moveRow(-1));
            downBtn.addActionListener(e -> moveRow(1));
            removeBtn.addActionListener(e -> removeRow());
            
            add(new JLabel("Field:")); add(fieldName);
            add(new JLabel("Dir:")); add(direction);
            add(new JLabel("Type:")); add(type);
            add(ignoreCase);
            add(new JLabel("Nulls:")); add(nullOrder);
            add(upBtn); add(downBtn); add(removeBtn);
        }
        
        private void moveRow(int offset) {
            int idx = rows.indexOf(this);
            if (idx < 0) return;
            int newIdx = idx + offset;
            if (newIdx >= 0 && newIdx < rows.size()) {
                rows.remove(idx);
                rows.add(newIdx, this);
                criteriaPanel.removeAll();
                for (CriterionRow r : rows) criteriaPanel.add(r);
                criteriaPanel.revalidate();
                criteriaPanel.repaint();
            }
        }
        
        private void removeRow() {
            criteriaPanel.remove(this);
            rows.remove(this);
            criteriaPanel.revalidate();
            criteriaPanel.repaint();
        }
        
        SortCriterion toCriterion() {
            return new SortCriterion(
                fieldName.getText().trim(),
                (Direction) direction.getSelectedItem(),
                (ComparisonType) type.getSelectedItem(),
                ignoreCase.isSelected(),
                (NullOrder) nullOrder.getSelectedItem()
            );
        }
    }

    DataSorterConfigDialog(JFrame owner, DataSorterModule module) {
        super(owner, true);
        this.module = module;
        setUndecorated(true);
        setSize(750, 380);
        setMinimumSize(new Dimension(750, 320));
        setLocationRelativeTo(owner);

        criteriaPanel = new JPanel();
        criteriaPanel.setLayout(new BoxLayout(criteriaPanel, BoxLayout.Y_AXIS));
        criteriaPanel.setBackground(new Color(245, 245, 245));
        
        for (SortCriterion c : module.getCriteria()) {
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

        JLabel title = new JLabel("🔤  Data Sorter - " + module.getName());
        title.setForeground(new Color(225, 225, 225));
        titleBar.add(title, BorderLayout.WEST);

        JLabel actions = new JLabel("?     🗕     🗖     ✕");
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
        
        JButton addBtn = new JButton("+ Add Sort Criterion");
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
        List<SortCriterion> oldCriteria = new ArrayList<>(module.getCriteria());

        module.getCriteria().clear();
        for (CriterionRow row : rows) {
            module.getCriteria().add(row.toCriterion());
        }

        List<String> errors = module.validate();
        if (!errors.isEmpty()) {
            module.getCriteria().clear();
            module.getCriteria().addAll(oldCriteria);
            JOptionPane.showMessageDialog(
                    this, String.join("\n", errors), "Data Sorter", JOptionPane.ERROR_MESSAGE);
            return;
        }
        accepted = true;
        dispose();
    }
}
