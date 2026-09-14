package com.vdp.core.model;

import com.vdp.core.model.ExecutionContext.DataType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Navigation and row mapping for Data Proof, with no Swing dependency. */
public final class DataProofModel {
    private final ExecutionSnapshot snapshot;
    private int currentRecordIndex;

    public DataProofModel(ExecutionSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        currentRecordIndex = snapshot.getRecordCount() == 0 ? -1 : 0;
    }

    public int getCurrentRecordIndex() { return currentRecordIndex; }
    public int getCurrentRecordNumber() { return currentRecordIndex < 0 ? 0 : currentRecordIndex + 1; }
    public int getTotalRecords() { return snapshot.getRecordCount(); }

    public void first() {
        if (getTotalRecords() > 0) currentRecordIndex = 0;
    }

    public void previous() {
        if (currentRecordIndex > 0) currentRecordIndex--;
    }

    public void next() {
        if (currentRecordIndex >= 0 && currentRecordIndex < getTotalRecords() - 1) {
            currentRecordIndex++;
        }
    }

    public void last() {
        if (getTotalRecords() > 0) currentRecordIndex = getTotalRecords() - 1;
    }

    public boolean selectRecordNumber(int oneBasedRecordNumber) {
        if (oneBasedRecordNumber < 1 || oneBasedRecordNumber > getTotalRecords()) {
            return false;
        }
        currentRecordIndex = oneBasedRecordNumber - 1;
        return true;
    }

    public List<DataProofRow> getRows() {
        List<DataProofRow> rows = new ArrayList<>();
        String position = getTotalRecords() == 0
                ? "0/0" : getCurrentRecordNumber() + "/" + getTotalRecords();
        rows.add(new DataProofRow(snapshot.getRootArrayName(), "Array", position, 0));

        String[] names = snapshot.getColumnNames();
        DataType[] types = snapshot.getColumnTypes();
        String[] record = currentRecordIndex < 0
                ? new String[0] : snapshot.getRecord(currentRecordIndex);
        for (int index = 0; index < names.length; index++) {
            String value = index < record.length ? record[index] : "";
            rows.add(new DataProofRow(names[index], displayType(types[index]), value, 1));
        }
        return List.copyOf(rows);
    }

    private String displayType(DataType type) {
        String lower = type.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
