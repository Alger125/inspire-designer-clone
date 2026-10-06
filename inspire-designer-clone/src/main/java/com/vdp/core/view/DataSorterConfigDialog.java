package com.vdp.core.view;

import com.vdp.core.model.DataNode;
import com.vdp.core.model.DataSorterModule;
import com.vdp.core.model.DataSorterModule.ComparisonType;
import com.vdp.core.model.DataSorterModule.Direction;
import com.vdp.core.model.DataSorterModule.MemoryUsage;
import com.vdp.core.model.DataSorterModule.NullOrder;
import com.vdp.core.model.DataSorterModule.SortCriterion;
import com.vdp.core.model.DataSorterModule.Strength;
import com.vdp.core.model.Workflow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/** Editor for the Data Sorter configuration from manual section 5.20. */
final class DataSorterConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    /** Item shown in the Strength combo when no explicit strength is chosen. */
    private static final String DEFAULT_STRENGTH = "Default";

    private final DataSorterModule module;
    private final Workflow workflow;
    private final List<CriterionRow> rows = new ArrayList<>();
    private final JPanel criteriaPanel;

    private final JComboBox<MemoryUsage> memoryUsage;
    private final JCheckBox addSortedToNewCopy;
    private final JTextField sortedName;
    private final JCheckBox removeDuplicates;
    
    private final JTree structureTree;
    private final List<String> availableFields = new ArrayList<>();

    private boolean accepted;
    private Point dragOffset;

    private class CriterionRow extends JPanel {
        private static final long serialVersionUID = 1L;

        JComboBox<String> fieldName;
        JComboBox<Direction> direction = new JComboBox<>(Direction.values());
        JComboBox<ComparisonType> type = new JComboBox<>(ComparisonType.values());
        JCheckBox ignoreCase = new JCheckBox("Ignore case");
        JComboBox<NullOrder> nullOrder = new JComboBox<>(NullOrder.values());

        JCheckBox binaryCompare = new JCheckBox("Binary compare");
        JComboBox<String> locale = new JComboBox<>(new String[]{
            "", "en-US", "es-ES", "es-MX", "fr-FR", "de-DE", "pt-BR", "it-IT"});
        JComboBox<Object> strength = new JComboBox<>();
        JCheckBox normalization = new JCheckBox("Normalization");

        JButton upBtn = new JButton("\u2191");
        JButton downBtn = new JButton("\u2193");
        JButton removeBtn = new JButton("X");

        CriterionRow(SortCriterion crit) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)));

            fieldName = new JComboBox<>(availableFields.toArray(String[]::new));
            fieldName.setEditable(true);
            fieldName.setPreferredSize(new Dimension(100, 24));

            locale.setEditable(true);
            strength.addItem(DEFAULT_STRENGTH);
            for (Strength s : Strength.values()) strength.addItem(s);

            if (crit != null) {
                fieldName.setSelectedItem(crit.getFieldName());
                direction.setSelectedItem(crit.getDirection());
                type.setSelectedItem(crit.getComparisonType());
                ignoreCase.setSelected(crit.isIgnoreCase());
                nullOrder.setSelectedItem(crit.getNullOrder());
                binaryCompare.setSelected(crit.isBinaryCompare());
                locale.setSelectedItem(crit.getLocale());
                strength.setSelectedItem(
                        crit.getStrength() == null ? DEFAULT_STRENGTH : crit.getStrength());
                normalization.setSelected(crit.isNormalization());
            } else {
                ignoreCase.setSelected(true);
                nullOrder.setSelectedItem(NullOrder.LAST);
                locale.setSelectedItem("");
                strength.setSelectedItem(DEFAULT_STRENGTH);
            }

            binaryCompare.addActionListener(e -> updateTextOptionsState());
            updateTextOptionsState();

            upBtn.setMargin(new Insets(0, 2, 0, 2));
            downBtn.setMargin(new Insets(0, 2, 0, 2));
            removeBtn.setMargin(new Insets(0, 2, 0, 2));

            upBtn.addActionListener(e -> moveRow(-1));
            downBtn.addActionListener(e -> moveRow(1));
            removeBtn.addActionListener(e -> removeRow());

            JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            line1.setOpaque(false);
            line1.add(new JLabel("Field:")); line1.add(fieldName);
            line1.add(new JLabel("Dir:")); line1.add(direction);
            line1.add(new JLabel("Type:")); line1.add(type);
            line1.add(ignoreCase);
            line1.add(new JLabel("Nulls:")); line1.add(nullOrder);
            line1.add(upBtn); line1.add(downBtn); line1.add(removeBtn);

            JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            line2.setOpaque(false);
            line2.add(createIndent());
            line2.add(binaryCompare);
            line2.add(new JLabel("Locale:")); line2.add(locale);
            line2.add(new JLabel("Strength:")); line2.add(strength);
            line2.add(normalization);

            add(line1);
            add(line2);
        }

        private JLabel createIndent() {
            JLabel indent = new JLabel(" ");
            indent.setPreferredSize(new Dimension(30, 10));
            return indent;
        }

        private void updateTextOptionsState() {
            boolean collation = !binaryCompare.isSelected();
            locale.setEnabled(collation);
            strength.setEnabled(collation);
            normalization.setEnabled(collation);
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
            Object selectedField = fieldName.getSelectedItem();
            SortCriterion criterion = new SortCriterion(
                selectedField == null ? "" : selectedField.toString().trim(),
                (Direction) direction.getSelectedItem(),
                (ComparisonType) type.getSelectedItem(),
                ignoreCase.isSelected(),
                (NullOrder) nullOrder.getSelectedItem()
            );
            criterion.setBinaryCompare(binaryCompare.isSelected());
            Object localeValue = locale.getSelectedItem();
            criterion.setLocale(localeValue == null ? "" : localeValue.toString());
            Object strengthValue = strength.getSelectedItem();
            criterion.setStrength(
                    strengthValue instanceof Strength s ? s : null);
            criterion.setNormalization(normalization.isSelected());
            return criterion;
        }
    }

    DataSorterConfigDialog(JFrame owner, DataSorterModule module, Workflow workflow) {
        super(owner, true);
        this.module = module;
        this.workflow = workflow;
        
        setUndecorated(true);
        setSize(980, 520);
        setMinimumSize(new Dimension(860, 420));
        setLocationRelativeTo(owner);

        // Load Schema
        DataNode incomingSchema = workflow.getIncomingSchema(module.getId(), "DataInput");
        if (incomingSchema == null) {
            incomingSchema = DataNode.arrayNode("Records");
            incomingSchema.setDataType(DataNode.DataType.ARRAY);
        }
        extractFields(incomingSchema);

        structureTree = new JTree(buildTreeNode(incomingSchema));
        structureTree.setShowsRootHandles(true);
        structureTree.setCellRenderer(new SchemaTreeRenderer());
        for (int i = 0; i < structureTree.getRowCount(); i++) {
            structureTree.expandRow(i);
        }

        memoryUsage = new JComboBox<>(MemoryUsage.values());
        memoryUsage.setSelectedItem(module.getMemoryUsage());
        addSortedToNewCopy = new JCheckBox("Add sorted to new copy", module.isAddSortedToNewCopy());
        addSortedToNewCopy.setOpaque(false);
        sortedName = new JTextField(module.getSortedName(), 10);
        sortedName.setEnabled(module.isAddSortedToNewCopy());
        addSortedToNewCopy.addActionListener(
                e -> sortedName.setEnabled(addSortedToNewCopy.isSelected()));
        removeDuplicates = new JCheckBox("Remove duplicate records", module.isRemoveDuplicates());
        removeDuplicates.setOpaque(false);

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
        root.add(createSplitPane(), BorderLayout.CENTER);
        root.add(createButtons(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    boolean isAccepted() { return accepted; }

    private void extractFields(DataNode node) {
        if (node.getDataType() == DataNode.DataType.STRING || 
            node.getDataType() == DataNode.DataType.NUMBER || 
            node.getDataType() == DataNode.DataType.BOOL) {
            availableFields.add(node.getName());
        }
        for (DataNode child : node.getChildren()) {
            extractFields(child);
        }
    }

    private DefaultMutableTreeNode buildTreeNode(DataNode node) {
        DefaultMutableTreeNode uiNode = new DefaultMutableTreeNode(node);
        for (DataNode child : node.getChildren()) {
            uiNode.add(buildTreeNode(child));
        }
        return uiNode;
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(58, 58, 58));
        titleBar.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 7));

        JLabel title = new JLabel("\ud83d\udd24  Data Sorter - " + module.getName());
        title.setForeground(new Color(225, 225, 225));
        titleBar.add(title, BorderLayout.WEST);

        JLabel actions = new JLabel("?     \ud83d\uddd5     \ud83d\uddd6     \u2715");
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

    private JSplitPane createSplitPane() {
        // Left Pane (Data Structure)
        JPanel leftPane = new JPanel(new BorderLayout());
        leftPane.setBackground(Color.WHITE);
        JLabel leftTitle = new JLabel(" Data Structure");
        leftTitle.setOpaque(true);
        leftTitle.setBackground(new Color(220, 220, 220));
        leftTitle.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        leftPane.add(leftTitle, BorderLayout.NORTH);
        
        JScrollPane treeScroll = new JScrollPane(structureTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        leftPane.add(treeScroll, BorderLayout.CENTER);

        // Right Pane (Sorter Options)
        JPanel rightPane = new JPanel(new BorderLayout(10, 10));
        rightPane.setBackground(new Color(232, 232, 232));
        rightPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel moduleOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        moduleOptions.setOpaque(false);
        moduleOptions.add(new JLabel("Memory usage:"));
        moduleOptions.add(memoryUsage);
        moduleOptions.add(addSortedToNewCopy);
        moduleOptions.add(new JLabel("Sorted name:"));
        moduleOptions.add(sortedName);
        moduleOptions.add(removeDuplicates);
        rightPane.add(moduleOptions, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(criteriaPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        rightPane.add(scroll, BorderLayout.CENTER);

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

        rightPane.add(options, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        splitPane.setDividerLocation(200);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        return splitPane;
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
        MemoryUsage oldMemory = module.getMemoryUsage();
        boolean oldAddSorted = module.isAddSortedToNewCopy();
        String oldSortedName = module.getSortedName();
        boolean oldRemoveDuplicates = module.isRemoveDuplicates();

        module.getCriteria().clear();
        for (CriterionRow row : rows) {
            module.getCriteria().add(row.toCriterion());
        }
        module.setMemoryUsage((MemoryUsage) memoryUsage.getSelectedItem());
        module.setSortedName(sortedName.getText());
        module.setAddSortedToNewCopy(addSortedToNewCopy.isSelected());
        module.setRemoveDuplicates(removeDuplicates.isSelected());

        List<String> errors = module.validate();
        if (!errors.isEmpty()) {
            module.getCriteria().clear();
            module.getCriteria().addAll(oldCriteria);
            module.setMemoryUsage(oldMemory);
            module.setSortedName(oldSortedName);
            module.setAddSortedToNewCopy(oldAddSorted);
            module.setRemoveDuplicates(oldRemoveDuplicates);
            JOptionPane.showMessageDialog(
                    this, String.join("\n", errors), "Data Sorter", JOptionPane.ERROR_MESSAGE);
            return;
        }
        accepted = true;
        dispose();
    }

    private static class SchemaTreeRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof DataNode dataNode) {
                    setText(dataNode.getName() + "  [" + dataNode.getDataType().name() + "]");
                }
            }
            return this;
        }
    }
}
