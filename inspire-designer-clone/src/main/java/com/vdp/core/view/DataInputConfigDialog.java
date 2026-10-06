package com.vdp.core.view;

import com.vdp.core.model.DataInputModule;
import com.vdp.core.model.DataNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.JTree;

/**
 * Configuration dialog for the CSV Data Input module (Manual 4.3.1).
 * Features tabs: Input File, Properties, Data Structure.
 */
final class DataInputConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataInputModule module;

    // Input File Tab
    private final JTextField arrayNameField;
    private final JTextField inputPathField;
    private final JComboBox<DataInputModule.FileType> fileTypeBox;
    private final JCheckBox autoDetectCheckBox;
    private final JCheckBox proofLimitCheckBox;
    private final JSpinner proofRecordCountSpinner;
    private final JCheckBox productionRangeCheckBox;
    private final JSpinner startRecordSpinner;
    private final JSpinner endRecordSpinner;

    // Properties Tab
    private final JComboBox<String> encodingBox;
    private final JTextField separatorField;
    private final JTextField qualifierField;
    private final JSpinner skippedLinesSpinner;
    private final JCheckBox firstRecordIsHeaderCheckBox;

    // Data Structure Tab
    private final JTree structureTree;
    private DefaultTreeModel treeModel;

    private boolean accepted;
    private Point dragOffset;

    DataInputConfigDialog(JFrame owner, DataInputModule module) {
        super(owner, true);
        this.module = module;
        setUndecorated(true);
        setSize(650, 480);
        setMinimumSize(new Dimension(600, 450));
        setLocationRelativeTo(owner);

        // -- Input File UI --
        arrayNameField = new JTextField(module.getRootArrayName(), 28);
        inputPathField = new JTextField(module.getInputFilePath() == null ? "" : module.getInputFilePath(), 28);
        fileTypeBox = new JComboBox<>(DataInputModule.FileType.values());
        fileTypeBox.setSelectedItem(module.getFileType());
        
        autoDetectCheckBox = new JCheckBox("Auto detect file type and separator", module.isAutoDetect());
        autoDetectCheckBox.setOpaque(false);

        proofLimitCheckBox = new JCheckBox("Use only first records in Proof", module.isUseOnlyFirstRecordsInProof());
        proofLimitCheckBox.setOpaque(false);
        proofRecordCountSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, module.getProofRecordCount()), 1, 100_000_000, 1));

        productionRangeCheckBox = new JCheckBox("Range for Production", module.isRangeForProduction());
        productionRangeCheckBox.setOpaque(false);
        startRecordSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, module.getStartRecord()), 1, 100_000_000, 1));
        endRecordSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, module.getEndRecord()), 1, 100_000_000, 1));

        proofLimitCheckBox.addActionListener(event -> updateRecordLimitState());
        productionRangeCheckBox.addActionListener(event -> updateRecordLimitState());
        updateRecordLimitState();

        // -- Properties UI --
        encodingBox = new JComboBox<>(new String[]{"UTF-8", "ISO-8859-1", "Windows-1252", "US-ASCII"});
        encodingBox.setEditable(true);
        encodingBox.setSelectedItem(module.getTextEncoding());

        separatorField = new JTextField(module.getFieldSeparator(), 4);
        qualifierField = new JTextField(module.getTextQualifier(), 4);
        skippedLinesSpinner = new JSpinner(new SpinnerNumberModel(module.getSkipFirstLines(), 0, 1_000_000, 1));
        firstRecordIsHeaderCheckBox = new JCheckBox("Use first data record as field names", module.isUseFirstRecordAsFieldNames());
        firstRecordIsHeaderCheckBox.setOpaque(false);

        // -- Data Structure UI --
        structureTree = new JTree();
        structureTree.setShowsRootHandles(true);
        structureTree.setCellRenderer(new SchemaTreeRenderer());
        loadSchemaToTree();

        // Build Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Input File", createInputFileTab());
        tabs.addTab("Properties", createPropertiesTab());
        tabs.addTab("Data Structure", createDataStructureTab());

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55), 2));
        root.add(createTitleBar(), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(createButtons(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    boolean isAccepted() { return accepted; }

    private void updateRecordLimitState() {
        proofRecordCountSpinner.setEnabled(proofLimitCheckBox.isSelected());
        startRecordSpinner.setEnabled(productionRangeCheckBox.isSelected());
        endRecordSpinner.setEnabled(productionRangeCheckBox.isSelected());
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(58, 58, 58));
        titleBar.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 7));

        JLabel title = new JLabel("\ud83d\udcc1  Data Input - " + module.getName());
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

    private JPanel createInputFileTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(240, 240, 240));
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, constraints, 0, "Root array name:", arrayNameField);
        addFileRow(form, constraints, 1);
        addRow(form, constraints, 2, "File type:", fileTypeBox);

        constraints.gridx = 1; constraints.gridy = 3; constraints.weightx = 1;
        form.add(autoDetectCheckBox, constraints);

        constraints.gridx = 1; constraints.gridy = 4;
        form.add(proofLimitCheckBox, constraints);
        addRow(form, constraints, 5, "Record count:", proofRecordCountSpinner);

        constraints.gridx = 1; constraints.gridy = 6;
        form.add(productionRangeCheckBox, constraints);
        addRow(form, constraints, 7, "Start record:", startRecordSpinner);
        addRow(form, constraints, 8, "End record:", endRecordSpinner);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel createPropertiesTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(240, 240, 240));
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, constraints, 0, "Text encoding:", encodingBox);
        addRow(form, constraints, 1, "Field separator:", separatorField);
        addRow(form, constraints, 2, "Text qualifier:", qualifierField);
        addRow(form, constraints, 3, "Skip first lines:", skippedLinesSpinner);

        constraints.gridx = 1; constraints.gridy = 4; constraints.weightx = 1;
        form.add(firstRecordIsHeaderCheckBox, constraints);

        JLabel info = new JLabel("Only CSV format is fully supported in this milestone.");
        info.setForeground(Color.GRAY);
        constraints.gridy = 5;
        form.add(info, constraints);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel createDataStructureTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(structureTree);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.add(scroll, BorderLayout.CENTER);

        JButton generateBtn = new JButton("Generate and Read");
        generateBtn.addActionListener(e -> regenerateSchema());
        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(generateBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void addFileRow(JPanel panel, GridBagConstraints constraints, int row) {
        constraints.gridx = 0; constraints.gridy = row; constraints.weightx = 0;
        panel.add(new JLabel("Input file:"), constraints);

        JPanel filePanel = new JPanel(new BorderLayout(6, 0));
        filePanel.setOpaque(false);
        filePanel.add(inputPathField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(event -> chooseFile());
        filePanel.add(browseButton, BorderLayout.EAST);

        constraints.gridx = 1; constraints.weightx = 1;
        panel.add(filePanel, constraints);
    }

    private void addRow(JPanel panel, GridBagConstraints constraints, int row, String label, java.awt.Component component) {
        constraints.gridx = 0; constraints.gridy = row; constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1; constraints.weightx = 1;
        panel.add(component, constraints);
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

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Data files (*.csv, *.dbf, *.txt)", "csv", "dbf", "txt"));
        if (!inputPathField.getText().isBlank()) {
            try {
                Path currentPath = Path.of(inputPathField.getText().trim());
                if (Files.exists(currentPath)) chooser.setSelectedFile(currentPath.toFile());
            } catch (InvalidPathException ignored) {}
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadSchemaToTree() {
        DataNode schemaRoot = module.getDesignSchema();
        DefaultMutableTreeNode rootUI = buildTreeNode(schemaRoot);
        treeModel = new DefaultTreeModel(rootUI);
        structureTree.setModel(treeModel);
        for (int i = 0; i < structureTree.getRowCount(); i++) {
            structureTree.expandRow(i);
        }
    }

    private DefaultMutableTreeNode buildTreeNode(DataNode node) {
        DefaultMutableTreeNode uiNode = new DefaultMutableTreeNode(node);
        for (DataNode child : node.getChildren()) {
            uiNode.add(buildTreeNode(child));
        }
        return uiNode;
    }

    private void regenerateSchema() {
        // Apply current UI settings to the module temporarily so we can read the file correctly
        module.setRootArrayName(arrayNameField.getText().trim());
        module.setInputFilePath(inputPathField.getText().trim());
        module.setAutoDetect(autoDetectCheckBox.isSelected());
        module.setTextEncoding(String.valueOf(encodingBox.getSelectedItem()).trim());
        module.setFieldSeparator(separatorField.getText());
        module.setSkipFirstLines((Integer) skippedLinesSpinner.getValue());
        module.setUseFirstRecordAsFieldNames(firstRecordIsHeaderCheckBox.isSelected());

        loadSchemaToTree();
    }

    private void accept() {
        String arrayName = arrayNameField.getText().trim();
        String filePath = inputPathField.getText().trim();
        String encoding = String.valueOf(encodingBox.getSelectedItem()).trim();
        String separator = separatorField.getText();
        String qualifier = qualifierField.getText();

        if (arrayName.isBlank()) { showError("Array name cannot be empty."); return; }
        if (filePath.isBlank()) { showError("Select an input file."); return; }
        try {
            if (!Files.isRegularFile(Path.of(filePath))) { showError("The selected file does not exist."); return; }
        } catch (InvalidPathException e) { showError("The selected file path is invalid."); return; }
        try { Charset.forName(encoding); } catch (Exception e) { showError("Unsupported encoding."); return; }
        if (separator.length() != 1) { showError("Field separator must be one character."); return; }
        if (qualifier.length() > 1) { showError("Text qualifier must be empty or one character."); return; }
        if (productionRangeCheckBox.isSelected() && (Integer) endRecordSpinner.getValue() < (Integer) startRecordSpinner.getValue()) {
            showError("End record cannot be smaller than the start record."); return;
        }

        module.setRootArrayName(arrayName);
        module.setInputFilePath(filePath);
        module.setFileType((DataInputModule.FileType) fileTypeBox.getSelectedItem());
        module.setAutoDetect(autoDetectCheckBox.isSelected());
        module.setUseOnlyFirstRecordsInProof(proofLimitCheckBox.isSelected());
        module.setProofRecordCount((Integer) proofRecordCountSpinner.getValue());
        module.setRangeForProduction(productionRangeCheckBox.isSelected());
        module.setStartRecord((Integer) startRecordSpinner.getValue());
        module.setEndRecord((Integer) endRecordSpinner.getValue());
        
        module.setTextEncoding(encoding);
        module.setFieldSeparator(separator);
        module.setTextQualifier(qualifier);
        module.setSkipFirstLines((Integer) skippedLinesSpinner.getValue());
        module.setUseFirstRecordAsFieldNames(firstRecordIsHeaderCheckBox.isSelected());

        accepted = true;
        dispose();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Data Input", JOptionPane.ERROR_MESSAGE);
    }

    /** Renders the tree nodes as "Name (Type)" */
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
