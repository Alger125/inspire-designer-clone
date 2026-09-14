package com.vdp.core.view;


import com.vdp.core.model.ValidationMessage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Displays validation data and contains no workflow logic. */
final class ValidationResultsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DefaultTableModel tableModel;
    private final JLabel emptyMessage;

    ValidationResultsPanel() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, InspireTheme.BORDER));

        JLabel title = new JLabel("Validation Results");
        title.setForeground(InspireTheme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 4));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Module", "Severity", "Message"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(600);

        emptyMessage = new JLabel("No validation has been run.");
        emptyMessage.setForeground(new Color(155, 155, 155));
        emptyMessage.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 10));

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(emptyMessage, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    void showMessages(List<ValidationMessage> messages) {
        tableModel.setRowCount(0);
        emptyMessage.setText(messages.isEmpty()
                ? "No validation errors." : "");
        for (ValidationMessage message : messages) {
            tableModel.addRow(new Object[]{
                    message.moduleName(), message.severity(), message.message()
            });
        }
    }
}
