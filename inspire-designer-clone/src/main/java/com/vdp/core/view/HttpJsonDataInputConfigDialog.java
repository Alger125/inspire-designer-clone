package com.vdp.core.view;

import com.vdp.core.model.HttpJsonDataInputModule;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

/**
 * Basic editor for the HTTP JSON input module.
 */
final class HttpJsonDataInputConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final HttpJsonDataInputModule module;

    private final JTextField urlField;
    private final JTextField arrayNameField;
    private final JTextField jsonPathField;
    private final JSpinner timeoutSpinner;

    private boolean accepted;

    HttpJsonDataInputConfigDialog(
            JFrame owner,
            HttpJsonDataInputModule module) {

        super(owner, "HTTP JSON Input - " + module.getName(), true);

        this.module = module;

        setSize(570, 260);
        setMinimumSize(new Dimension(570, 260));
        setLocationRelativeTo(owner);

        urlField = new JTextField(
                module.getEndpointUrl() == null
                        ? ""
                        : module.getEndpointUrl(),
                34
        );

        arrayNameField = new JTextField(
                module.getRootArrayName(),
                34
        );

        jsonPathField = new JTextField(
                module.getJsonArrayPath(),
                34
        );

        timeoutSpinner = new JSpinner(
                new SpinnerNumberModel(
                        module.getTimeoutMilliseconds(),
                        1_000,
                        120_000,
                        1_000
                )
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

        addRow(form, constraints, 0, "API URL:", urlField);
        addRow(form, constraints, 1, "Array name:", arrayNameField);
        addRow(form, constraints, 2, "JSON array path:", jsonPathField);
        addRow(form, constraints, 3, "Timeout (ms):", timeoutSpinner);

        JLabel hint = new JLabel(
                "Examples: $ for a root array, or $.data.users for a nested array."
        );

        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 1;

        form.add(hint, constraints);

        return form;
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

    private void accept() {
        String url = urlField.getText().trim();
        String arrayName = arrayNameField.getText().trim();
        String jsonPath = jsonPathField.getText().trim();
        int timeout = (Integer) timeoutSpinner.getValue();

        if (url.isBlank()) {
            showError("API URL cannot be empty.");
            return;
        }

        if (arrayName.isBlank()) {
            showError("Array name cannot be empty.");
            return;
        }

        if (!jsonPath.equals("$") && !jsonPath.startsWith("$.")) {
            showError("JSON path must be $ or start with $.");
            return;
        }

        module.setEndpointUrl(url);
        module.setRootArrayName(arrayName);
        module.setJsonArrayPath(jsonPath);
        module.setTimeoutMilliseconds(timeout);

        if (!module.validate().isEmpty()) {
            showError(String.join("\n", module.validate()));
            return;
        }

        accepted = true;
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "HTTP JSON Input",
                JOptionPane.ERROR_MESSAGE
        );
    }
}