package com.vdp.core.model;

import com.vdp.core.model.ExecutionContext.DataType;
import java.util.List;

/** Inspire Designer 14 Data Generator, manual section 4.2, page 106. */
public final class DataGeneratorModule extends BaseDataInputModule {
    public static final String VALUE_FIELD_NAME = "Value";

    private String arrayName = "Numbers";
    private int from = 1;
    private int to = 100;

    public DataGeneratorModule() {
        super("DataGenerator1");
    }

    public String getArrayName() { return arrayName; }
    public void setArrayName(String arrayName) { this.arrayName = arrayName; }
    public int getFrom() { return from; }
    public void setFrom(int from) { this.from = from; }
    public int getTo() { return to; }
    public void setTo(int to) { this.to = to; }

    @Override
    protected void validateConfiguration(List<String> errors) {
        if (arrayName == null || arrayName.isBlank()) {
            errors.add("Array name cannot be blank");
        }
        if (from > to) {
            errors.add("From must be less than or equal to To");
        }
        if ((long) to - from + 1L > Integer.MAX_VALUE) {
            errors.add("The selected range is too large");
        }
    }

    @Override
    protected DataInputResult readData() {
        int count = to - from + 1;
        List<String[]> records = new java.util.ArrayList<>(count);
        for (long value = from; value <= to; value++) {
            records.add(new String[]{Long.toString(value)});
        }
        return new DataInputResult(
                arrayName,
                new String[]{VALUE_FIELD_NAME},
                new DataType[]{DataType.NUMBER},
                records);
    }
}
