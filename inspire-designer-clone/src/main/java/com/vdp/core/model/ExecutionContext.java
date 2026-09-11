package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;

public class ExecutionContext {
    // Aquí guardaremos los registros procesados para pasarlos entre módulos
    private List<String[]> records = new ArrayList<>();
    private String[] columnNames; // Para guardar el nombre de las cabeceras

    public List<String[]> getRecords() { return records; }
    public void setRecords(List<String[]> records) { this.records = records; }

    public String[] getColumnNames() { return columnNames; }
    public void setColumnNames(String[] columnNames) { this.columnNames = columnNames; }
}