package com.vdp.core.view;

import com.vdp.core.model.DataConcatenatorModule;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;

public class DataConcatenatorConfigDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private final DataConcatenatorModule module;
    private boolean accepted = false;
    
    private final JTextField nameField;
    private final JSpinner inputsSpinner;
    
    public DataConcatenatorConfigDialog(Frame owner, DataConcatenatorModule module) {
        super(owner, "Data Concatenator", true);
        this.module = module;
        
        setSize(300, 150);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        form.add(new JLabel("Name:"));
        nameField = new JTextField(module.getName(), 15);
        form.add(nameField);
        
        form.add(new JLabel("Number of inputs:"));
        inputsSpinner = new JSpinner(new SpinnerNumberModel(module.getNumberOfInputs(), 1, 100, 1));
        form.add(inputsSpinner);
        
        add(form, BorderLayout.CENTER);
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            applyChanges();
            accepted = true;
            dispose();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        
        buttons.add(ok);
        buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);
    }
    
    private void applyChanges() {
        module.setName(nameField.getText().trim());
        module.setNumberOfInputs((Integer) inputsSpinner.getValue());
    }
    
    public boolean isAccepted() {
        return accepted;
    }
}
