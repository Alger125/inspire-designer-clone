package com.vdp.core.view;

import com.vdp.core.model.DataInputModule;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Configuration dialog for the CSV Data Input module.
 */
final class DataInputConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataInputModule module;

    private final JTextField arrayNameField;
    private final JTextField inputPathField;
    private final JComboBox<String> encodingBox;
    private final JTextField separatorField;
    private final JTextField qualifierField;
    private final JSpinner skippedLinesSpinner;
    private final JCheckBox firstRecordIsHeaderCheckBox;

    private boolean accepted;

    DataInputConfigDialog(JFrame owner, DataInputModule module) {
        super(owner, "Data Input - " + module.getName(), true);

        this.module = module;

        setSize(560, 350);
        setMinimumSize(new Dimension(560, 350));
        setLocationRelativeTo(owner);

        arrayNameField = new JTextField(module.getRootArrayName(), 28);
        inputPathField = new JTextField(
                module.getInputFilePath() == null
                        ? ""
                        : module.getInputFilePath(),
                28
        );

        encodingBox = new JComboBox<>(new String[]{
            "UTF-8",
            "ISO-8859-1",
            "Windows-1252",
            "US-ASCII"
        });
        encodingBox.setEditable(true);
        encodingBox.setSelectedItem(module.getTextEncoding());

        separatorField = new JTextField(module.getFieldSeparator(), 4);
        qualifierField = new JTextField(module.getTextQualifier(), 4);

        skippedLinesSpinner = new JSpinner(
                new SpinnerNumberModel(
                        module.getSkipFirstLines(),
                        0,
                        1_000_000,
                        1
                )
        );

        firstRecordIsHeaderCheckBox = new JCheckBox(
                "Use first data record as field names",
                module.isUseFirstRecordAsFieldNames()
        );

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        root.add(createForm(), BorderLayout.CENTER);
        root.add(createButtons(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    boolean isAccepted() {
        return accepted;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, constraints, 0, "Array name:", arrayNameField);
        addFileRow(form, constraints, 1);
        addRow(form, constraints, 2, "Encoding:", encodingBox);
        addRow(form, constraints, 3, "Field separator:", separatorField);
        addRow(form, constraints, 4, "Text qualifier:", qualifierField);
        addRow(form, constraints, 5, "Skip first lines:", skippedLinesSpinner);

        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.weightx = 1;
        form.add(firstRecordIsHeaderCheckBox, constraints);

        JLabel description = new JLabel(
                "Only CSV files are supported in this milestone."
        );
        description.setForeground(new Color(110, 110, 110));

        constraints.gridx = 1;
        constraints.gridy = 7;
        form.add(description, constraints);

        return form;
    }

    private void addFileRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row) {

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;

        panel.add(new JLabel("CSV file:"), constraints);

        JPanel filePanel = new JPanel(new BorderLayout(6, 0));
        filePanel.add(inputPathField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(event -> chooseFile());
        filePanel.add(browseButton, BorderLayout.EAST);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(filePanel, constraints);
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component component) {

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;

        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;

        panel.add(component, constraints);
    }

    private JPanel createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(event -> accept());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());

        buttons.add(okButton);
        buttons.add(cancelButton);

        return buttons;
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(new FileNameExtensionFilter(
                "CSV files (*.csv)",
                "csv"
        ));

        if (!inputPathField.getText().isBlank()) {
            try {
                Path currentPath = Path.of(inputPathField.getText().trim());

                if (Files.exists(currentPath)) {
                    chooser.setSelectedFile(currentPath.toFile());
                }
            } catch (InvalidPathException exception) {
                // The validation shown by the dialog handles invalid paths.
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputPathField.setText(
                    chooser.getSelectedFile().getAbsolutePath()
            );
        }
    }

    private void accept() {
        String arrayName = arrayNameField.getText().trim();
        String filePath = inputPathField.getText().trim();
        String encoding = String.valueOf(encodingBox.getSelectedItem()).trim();
        String separator = separatorField.getText();
        String qualifier = qualifierField.getText();
        int skippedLines = (Integer) skippedLinesSpinner.getValue();

        String error = validateForm(
                arrayName,
                filePath,
                encoding,
                separator,
                qualifier
        );

        if (error != null) {
            JOptionPane.showMessageDialog(
                    this,
                    error,
                    "Data Input",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        module.setRootArrayName(arrayName);
        module.setInputFilePath(filePath);
        module.setTextEncoding(encoding);
        module.setFieldSeparator(separator);
        module.setTextQualifier(qualifier);
        module.setSkipFirstLines(skippedLines);
        module.setUseFirstRecordAsFieldNames(
                firstRecordIsHeaderCheckBox.isSelected()
        );

        accepted = true;
        dispose();
    }

    private String validateForm(
            String arrayName,
            String filePath,
            String encoding,
            String separator,
            String qualifier) {

        if (arrayName.isBlank()) {
            return "Array name cannot be empty.";
        }

        if (filePath.isBlank()) {
            return "Select a CSV file.";
        }

        try {
            if (!Files.isRegularFile(Path.of(filePath))) {
                return "The selected file does not exist.";
            }
        } catch (InvalidPathException exception) {
            return "The selected file path is invalid.";
        }

        try {
            Charset.forName(encoding);
        } catch (Exception exception) {
            return "Unsupported text encoding: " + encoding;
        }

        if (separator.length() != 1) {
            return "Field separator must contain exactly one character.";
        }

        if (qualifier.length() > 1) {
            return "Text qualifier must be empty or contain one character.";
        }

        return null;
    }
}