package com.vdp.core.view;

import com.vdp.core.model.DataNode;
import com.vdp.core.model.DataTransformerModule;
import com.vdp.core.model.DataTransformerModule.FieldMapping;
import com.vdp.core.model.Workflow;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Mapping editor for the Data Transformer (manual 5.21): a table of
 * Target Field / Expression rows plus the list of incoming source fields.
 * Double-clicking a source field adds a mapping {@code Field -> {Field}}.
 */
final class DataTransformerConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataTransformerModule module;
    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{"Target Field", "Expression"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JCheckBox keepUnmapped;
    private final List<String> sourceFields = new ArrayList<>();
    private boolean accepted;

    DataTransformerConfigDialog(JFrame owner, DataTransformerModule module, Workflow workflow) {
        super(owner, "Data Transformer - " + module.getName(), true);
        this.module = module;
        setSize(720, 460);
        setLocationRelativeTo(owner);

        DataNode schema = workflow.getIncomingSchema(module.getId(), "DataInput");
        Set<String> fields = new LinkedHashSet<>();
        if (schema != null) collectFields(schema, fields);
        sourceFields.addAll(fields);

        for (FieldMapping mapping : module.getMappings()) {
            tableModel.addRow(new Object[]{mapping.getTargetField(), mapping.getExpression()});
        }

        JList<String> sourceList = new JList<>(sourceFields.toArray(String[]::new));
        sourceList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && sourceList.getSelectedValue() != null) {
                    String field = sourceList.getSelectedValue();
                    tableModel.addRow(new Object[]{field, "{" + field + "}"});
                }
            }
        });
        JPanel left = new JPanel(new BorderLayout());
        left.add(new JLabel(" Source fields (double-click to map)"), BorderLayout.NORTH);
        left.add(new JScrollPane(sourceList), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(190, 0));

        table.setRowHeight(22);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JButton add = new JButton("Add");
        JButton remove = new JButton("Remove");
        add.addActionListener(e -> tableModel.addRow(new Object[]{"", ""}));
        remove.addActionListener(e -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            int row = table.getSelectedRow();
            if (row >= 0) tableModel.removeRow(row);
        });
        JPanel rowButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowButtons.add(add);
        rowButtons.add(remove);
        rowButtons.add(new JLabel("  Expressions: text, {Field}, UPPER(..) LOWER(..) TRIM(..) LENGTH(..)"));

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(table), BorderLayout.CENTER);
        right.add(rowButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(190);

        keepUnmapped = new JCheckBox("Keep unmapped source fields", module.isKeepUnmappedFields());
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> accept());
        cancel.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bottom.add(keepUnmapped, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);
        bottom.add(buttons, BorderLayout.EAST);

        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    private static void collectFields(DataNode node, Set<String> out) {
        if (node.getType() == DataNode.NodeType.VALUE && !node.getName().isBlank()) {
            out.add(node.getName());
        }
        for (DataNode child : node.getChildren()) collectFields(child, out);
    }

    private void accept() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        List<FieldMapping> rows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            rows.add(new FieldMapping(
                    String.valueOf(tableModel.getValueAt(i, 0)).trim(),
                    String.valueOf(tableModel.getValueAt(i, 1))));
        }
        List<FieldMapping> previous = new ArrayList<>(module.getMappings());
        module.getMappings().clear();
        module.getMappings().addAll(rows);
        List<String> errors = module.validate();
        if (!errors.isEmpty()) {
            module.getMappings().clear();
            module.getMappings().addAll(previous);
            JOptionPane.showMessageDialog(this, String.join("\n", errors),
                    "Data Transformer", JOptionPane.WARNING_MESSAGE);
            return;
        }
        module.setKeepUnmappedFields(keepUnmapped.isSelected());
        accepted = true;
        dispose();
    }

    boolean isAccepted() { return accepted; }
}
