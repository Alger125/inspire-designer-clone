package com.vdp.core.view;

import com.vdp.core.model.DataProofModel;
import com.vdp.core.model.DataProofRow;
import com.vdp.core.model.ExecutionSnapshot;
import com.vdp.core.model.LogEntry;
import com.vdp.core.model.ProofRunResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Independent Data Proof view modeled after manual section 10.4. */
final class DataProofPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String EMPTY_CARD = "EMPTY";
    private static final String DATA_CARD = "DATA";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    private final JComboBox<ExecutionSnapshot> moduleSelector = new JComboBox<>();
    private final JSpinner recordSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JButton firstButton = new JButton("|◀");
    private final JButton previousButton = new JButton("‹");
    private final JButton nextButton = new JButton("›");
    private final JButton lastButton = new JButton("▶|");
    private final DefaultTableModel structureModel;
    private final DefaultTableModel logModel;
    private final JTable structureTable;
    private final JLabel characterCount = new JLabel("Chars: 0");
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(contentLayout);
    private DataProofModel proofModel;
    private boolean updatingControls;

    DataProofPanel() {
        super(new BorderLayout());
        setBackground(Color.WHITE);

        structureModel = readOnlyModel(new String[]{"Structure", "Type", "Value"});
        structureTable = new JTable(structureModel);
        configureStructureTable();
        logModel = readOnlyModel(new String[]{"Time", "Severity", "Event Description", "Code"});

        JPanel proofContent = new JPanel(new BorderLayout());
        proofContent.add(createInputToolbar(), BorderLayout.NORTH);
        proofContent.add(createDataAndLogArea(), BorderLayout.CENTER);
        proofContent.add(createStatusBar(), BorderLayout.SOUTH);

        JLabel empty = new JLabel(
                "Run Proof to inspect data at each supported module.", SwingConstants.CENTER);
        empty.setForeground(new Color(145, 145, 145));
        empty.setFont(empty.getFont().deriveFont(Font.PLAIN, 15f));
        contentCards.add(empty, EMPTY_CARD);
        contentCards.add(proofContent, DATA_CARD);
        add(contentCards, BorderLayout.CENTER);
        contentLayout.show(contentCards, EMPTY_CARD);
        installListeners();
        updateNavigationState();
    }

    void showResult(ProofRunResult result) {
        updatingControls = true;
        DefaultComboBoxModel<ExecutionSnapshot> comboModel = new DefaultComboBoxModel<>();
        for (ExecutionSnapshot snapshot : result.getSnapshotsInWorkflowOrder()) {
            comboModel.addElement(snapshot);
        }
        moduleSelector.setModel(comboModel);
        moduleSelector.setRenderer(new SnapshotRenderer());
        populateLogs(result.getLogEntries());

        ExecutionSnapshot initial = result.getInitialModuleId()
                .map(result.getSnapshots()::get)
                .orElse(comboModel.getSize() == 0 ? null : comboModel.getElementAt(0));
        if (initial != null) {
            moduleSelector.setSelectedItem(initial);
            loadSnapshot(initial);
            contentLayout.show(contentCards, DATA_CARD);
        } else {
            proofModel = null;
            structureModel.setRowCount(0);
            contentLayout.show(contentCards, EMPTY_CARD);
        }
        updatingControls = false;
        updateNavigationState();
    }

    void showLastOrEmpty() {
        contentLayout.show(contentCards, proofModel == null ? EMPTY_CARD : DATA_CARD);
    }

    private JPanel createInputToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        toolbar.setBackground(InspireTheme.TOOLBAR);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, InspireTheme.BORDER));
        moduleSelector.setPreferredSize(new Dimension(220, 25));
        recordSpinner.setPreferredSize(new Dimension(64, 25));
        toolbar.add(moduleSelector);
        toolbar.add(new JLabel("  Record"));
        toolbar.add(recordSpinner);
        toolbar.add(firstButton);
        toolbar.add(previousButton);
        toolbar.add(nextButton);
        toolbar.add(lastButton);
        toolbar.add(new JLabel("     ◉   ◌   Bind Data"));
        return toolbar;
    }

    private JSplitPane createDataAndLogArea() {
        JScrollPane structureScroll = new JScrollPane(structureTable);
        structureScroll.setBorder(BorderFactory.createEmptyBorder());

        JTable logTable = new JTable(logModel);
        logTable.setShowGrid(true);
        logTable.setGridColor(new Color(225, 225, 225));
        logTable.setFillsViewportHeight(true);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(620);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(130);

        JPanel logPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Log");
        title.setForeground(InspireTheme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 4));
        logPanel.add(title, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, structureScroll, logPanel);
        split.setResizeWeight(0.78);
        split.setDividerLocation(500);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(new Color(248, 248, 248));
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, InspireTheme.BORDER));
        JLabel ready = new JLabel("Data proof ready");
        ready.setForeground(InspireTheme.TEXT);
        ready.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 4));
        status.add(ready, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 2));
        right.setOpaque(false);
        right.add(characterCount);
        right.add(new JLabel("Not logged"));
        status.add(right, BorderLayout.EAST);
        return status;
    }

    private void configureStructureTable() {
        structureTable.setShowGrid(false);
        structureTable.setFillsViewportHeight(true);
        structureTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        structureTable.setRowHeight(23);
        structureTable.getTableHeader().setBackground(new Color(245, 245, 245));
        structureTable.getColumnModel().getColumn(0).setPreferredWidth(330);
        structureTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        structureTable.getColumnModel().getColumn(2).setPreferredWidth(480);
        structureTable.setDefaultRenderer(Object.class, new StructureCellRenderer());
        structureTable.getSelectionModel().addListSelectionListener(event -> updateCharacterCount());
    }

    private void installListeners() {
        moduleSelector.addActionListener(event -> {
            if (!updatingControls && moduleSelector.getSelectedItem() instanceof ExecutionSnapshot snapshot) {
                loadSnapshot(snapshot);
            }
        });
        firstButton.addActionListener(event -> navigate(DataProofModel::first));
        previousButton.addActionListener(event -> navigate(DataProofModel::previous));
        nextButton.addActionListener(event -> navigate(DataProofModel::next));
        lastButton.addActionListener(event -> navigate(DataProofModel::last));
        recordSpinner.addChangeListener(event -> {
            if (!updatingControls && proofModel != null) {
                proofModel.selectRecordNumber((Integer) recordSpinner.getValue());
                refreshStructure();
            }
        });
    }

    private void navigate(java.util.function.Consumer<DataProofModel> navigation) {
        if (proofModel != null) {
            navigation.accept(proofModel);
            refreshStructure();
        }
    }

    private void loadSnapshot(ExecutionSnapshot snapshot) {
        proofModel = new DataProofModel(snapshot);
        int maximum = Math.max(1, proofModel.getTotalRecords());
        updatingControls = true;
        recordSpinner.setModel(new SpinnerNumberModel(
                Math.max(1, proofModel.getCurrentRecordNumber()), 1, maximum, 1));
        updatingControls = false;
        refreshStructure();
    }

    private void refreshStructure() {
        structureModel.setRowCount(0);
        if (proofModel != null) {
            for (DataProofRow row : proofModel.getRows()) {
                String prefix = row.depth() == 0 ? "▾ " : "      ";
                structureModel.addRow(new Object[]{
                        prefix + row.structure(), row.type(), row.value()
                });
            }
        }
        updatingControls = true;
        recordSpinner.setValue(Math.max(1, proofModel == null ? 1 : proofModel.getCurrentRecordNumber()));
        updatingControls = false;
        updateNavigationState();
        updateCharacterCount();
    }

    private void updateNavigationState() {
        boolean hasRecords = proofModel != null && proofModel.getTotalRecords() > 0;
        int current = hasRecords ? proofModel.getCurrentRecordIndex() : -1;
        int last = hasRecords ? proofModel.getTotalRecords() - 1 : -1;
        recordSpinner.setEnabled(hasRecords);
        firstButton.setEnabled(hasRecords && current > 0);
        previousButton.setEnabled(hasRecords && current > 0);
        nextButton.setEnabled(hasRecords && current < last);
        lastButton.setEnabled(hasRecords && current < last);
    }

    private void populateLogs(List<LogEntry> entries) {
        logModel.setRowCount(0);
        for (LogEntry entry : entries) {
            logModel.addRow(new Object[]{
                    TIME_FORMAT.format(entry.timestamp()),
                    entry.severity(),
                    entry.description(),
                    entry.code()
            });
        }
    }

    private void updateCharacterCount() {
        int row = structureTable.getSelectedRow();
        Object value = row < 0 ? null : structureModel.getValueAt(row, 2);
        characterCount.setText("Chars: " + (value == null ? 0 : value.toString().length()));
    }

    private DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static final class SnapshotRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
                javax.swing.JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setText(value instanceof ExecutionSnapshot snapshot
                    ? snapshot.getModuleName() : "");
            return label;
        }
    }

    private static final class StructureCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                component.setBackground(row == 0 ? new Color(229, 229, 229) : Color.WHITE);
            }
            return component;
        }
    }
}
