package com.vdp.core.model;

import com.vdp.core.model.ExecutionContext.DataType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Deep immutable copy of one module's execution context. */
public final class ExecutionSnapshot {
    private final String moduleId;
    private final String moduleName;
    private final String rootArrayName;
    private final String[] columnNames;
    private final DataType[] columnTypes;
    private final List<String[]> records;

    private ExecutionSnapshot(
            String moduleId,
            String moduleName,
            String rootArrayName,
            String[] columnNames,
            DataType[] columnTypes,
            List<String[]> records) {
        this.moduleId = Objects.requireNonNull(moduleId, "moduleId");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.rootArrayName = Objects.requireNonNull(rootArrayName, "rootArrayName");
        this.columnNames = Arrays.copyOf(columnNames, columnNames.length);
        this.columnTypes = Arrays.copyOf(columnTypes, columnTypes.length);
        if (columnNames.length != columnTypes.length) {
            throw new IllegalArgumentException("Each column must have a type");
        }

        List<String[]> copiedRecords = new ArrayList<>(records.size());
        for (String[] record : records) {
            copiedRecords.add(Arrays.copyOf(record, record.length));
        }
        this.records = Collections.unmodifiableList(copiedRecords);
    }

    public static ExecutionSnapshot from(InspireModule module, ExecutionContext context) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(context, "context");
        String rootName = context.getRootArrayName();
        if (rootName == null || rootName.isBlank()) {
            throw new IllegalStateException("Execution did not produce a root array name");
        }
        return new ExecutionSnapshot(
                module.getId(),
                module.getName(),
                rootName,
                context.getColumnNames(),
                context.getColumnTypes(),
                context.getRecords());
    }

    public String getModuleId() { return moduleId; }
    public String getModuleName() { return moduleName; }
    public String getRootArrayName() { return rootArrayName; }
    public String[] getColumnNames() { return Arrays.copyOf(columnNames, columnNames.length); }
    public DataType[] getColumnTypes() { return Arrays.copyOf(columnTypes, columnTypes.length); }
    public int getRecordCount() { return records.size(); }
    public String[] getRecord(int index) {
        String[] record = records.get(index);
        return Arrays.copyOf(record, record.length);
    }

    public List<String[]> getRecords() {
        List<String[]> copy = new ArrayList<>(records.size());
        for (String[] record : records) {
            copy.add(Arrays.copyOf(record, record.length));
        }
        return Collections.unmodifiableList(copy);
    }
}
