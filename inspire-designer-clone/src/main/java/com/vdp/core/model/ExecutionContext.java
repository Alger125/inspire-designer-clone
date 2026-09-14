package com.vdp.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExecutionContext {
    public enum DataType {
        STRING,
        NUMBER
    }

    private String rootArrayName;
    private List<String[]> records = new ArrayList<>();
    private String[] columnNames = new String[0];
    private DataType[] columnTypes = new DataType[0];

    public List<String[]> getRecords() { return records; }
    public void setRecords(List<String[]> records) {
        this.records = Objects.requireNonNull(records, "records");
    }

    public String[] getColumnNames() { return columnNames; }
    public void setColumnNames(String[] columnNames) {
        this.columnNames = Objects.requireNonNull(columnNames, "columnNames");
    }

    public String getRootArrayName() { return rootArrayName; }
    public DataType[] getColumnTypes() { return columnTypes; }

    public void replaceData(
            String newRootArrayName,
            String[] newColumnNames,
            DataType[] newColumnTypes,
            List<String[]> newRecords) {
        Objects.requireNonNull(newRootArrayName, "rootArrayName");
        Objects.requireNonNull(newColumnNames, "columnNames");
        Objects.requireNonNull(newColumnTypes, "columnTypes");
        Objects.requireNonNull(newRecords, "records");
        if (newColumnNames.length != newColumnTypes.length) {
            throw new IllegalArgumentException("Each column must have a data type");
        }

        rootArrayName = newRootArrayName;
        columnNames = Arrays.copyOf(newColumnNames, newColumnNames.length);
        columnTypes = Arrays.copyOf(newColumnTypes, newColumnTypes.length);
        records = new ArrayList<>(newRecords);
    }
}
