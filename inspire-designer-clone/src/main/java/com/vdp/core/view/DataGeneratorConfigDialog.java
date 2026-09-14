package com.vdp.core.view;

import com.vdp.core.model.DataGeneratorModule;
import javax.swing.*;
import java.awt.*;

public class DataGeneratorConfigDialog extends JDialog {
    private JTextField txtArray;
    private JSpinner spnFrom;
    private JSpinner spnTo;
    private boolean isOk = false;

    public DataGeneratorConfigDialog(Frame owner, DataGeneratorModule module) {
        super(owner, "Data Generator Configuration", true); // Modal
        setSize(300, 200);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Panel de formulario
        JPanel pnlForm = new JPanel(new GridLayout(3, 2, 10, 10));
        pnlForm.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        pnlForm.add(new JLabel("Array:"));
        txtArray = new JTextField((String) module.getProperty("Array"));
        pnlForm.add(txtArray);

        pnlForm.add(new JLabel("From:"));
        spnFrom = new JSpinner(new SpinnerNumberModel((int) module.getProperty("From"), -1000000, 1000000, 1));
        pnlForm.add(spnFrom);

        pnlForm.add(new JLabel("To:"));
        spnTo = new JSpinner(new SpinnerNumberModel((int) module.getProperty("To"), -1000000, 1000000, 1));
        pnlForm.add(spnTo);

        add(pnlForm, BorderLayout.CENTER);

        // Botones
        JPanel pnlButtons = new JPanel();
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOk.addActionListener(e -> {
            // Validación visual antes de guardar
            if (txtArray.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Array name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int from = (int) spnFrom.getValue();
            int to = (int) spnTo.getValue();
            if (from > to) {
                JOptionPane.showMessageDialog(this, "'From' value cannot be greater than 'To'.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Actualizamos la instancia real del modelo
            module.setProperty("Array", txtArray.getText().trim());
            module.setProperty("From", from);
            module.setProperty("To", to);
            isOk = true;
            dispose();
        });

        btnCancel.addActionListener(e -> dispose());

        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);
        add(pnlButtons, BorderLayout.SOUTH);
    }

    public boolean isOk() { return isOk; }
}