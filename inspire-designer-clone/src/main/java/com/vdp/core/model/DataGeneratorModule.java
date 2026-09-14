package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación exacta del Data Generator Module (Manual p. 106).
 */
public class DataGeneratorModule extends BaseDataInputModule {

    public DataGeneratorModule() {
        super("Data Generator");
        // Valores predeterminados documentados
        setProperty("Array", "Numbers");
        setProperty("From", 1);
        setProperty("To", 100);
    }

    @Override
    protected List<String> specificValidation() {
        List<String> errors = new ArrayList<>();
        
        String arrayName = (String) getProperty("Array");
        if (arrayName == null || arrayName.trim().isEmpty()) {
            errors.add("Error: Array name cannot be empty.");
        }

        int from = (int) getProperty("From");
        int to = (int) getProperty("To");
        if (from > to) {
            errors.add("Error: 'From' value (" + from + ") cannot be greater than 'To' value (" + to + ").");
        }

        return errors;
    }

    @Override
    protected String[] getGeneratedColumnNames() {
        // Estructura de salida documentada: un solo campo llamado Value
        return new String[] { "Value" }; 
    }

    @Override
    protected List<String[]> generateOrReadData() {
        int from = (int) getProperty("From");
        int to = (int) getProperty("To");
        
        List<String[]> data = new ArrayList<>();
        
        // El rango es inclusivo
        for (int i = from; i <= to; i++) {
            data.add(new String[]{ String.valueOf(i) });
        }
        
        return data;
    }
}