package com.vdp.core.model;

import com.vdp.core.model.ExecutionContext.DataType;
import java.util.List;
import java.util.Objects;

public final class DataInputResult {
    private final String rootArrayName;
    private final String[] columnNames;
    private final DataType[] columnTypes;
    private final List<String[]> records;

    public DataInputResult(
            String rootArrayName,
            String[] columnNames,
            DataType[] columnTypes,
            List<String[]> records) {
        this.rootArrayName = Objects.requireNonNull(rootArrayName, "rootArrayName");
        this.columnNames = Objects.requireNonNull(columnNames, "columnNames");
        this.columnTypes = Objects.requireNonNull(columnTypes, "columnTypes");
        this.records = Objects.requireNonNull(records, "records");
    }

    public String getRootArrayName() { return rootArrayName; }
    public String[] getColumnNames() { return columnNames; }
    public DataType[] getColumnTypes() { return columnTypes; }
    public List<String[]> getRecords() { return records; }
}
