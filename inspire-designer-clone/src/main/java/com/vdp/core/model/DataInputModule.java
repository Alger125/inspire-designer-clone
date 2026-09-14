package com.vdp.core.model;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DataInputModule extends BaseDataInputModule {

    // --- INPUT FILE TAB ---[cite: 2]
    private String rootArrayName = "Records"; 
    private String inputFilePath;
    private FileType fileType = FileType.CSV; 
    private boolean useOnlyFirstRecordsInProof = false;
    private int recordCount = 0;

    // --- PROPERTIES TAB ---[cite: 2]
    private String textEncoding = "UTF-8";
    private String fieldSeparators = ","; 
    private String textQualifiers = "\""; 
    private LineSeparator lineSeparator = LineSeparator.WINDOWS_MAC;
    private int skipFirstLines = 0;
    private boolean multipleRecordTypes = false;
    private boolean useFirstRecordAsFieldNames = true; 

    public enum FileType {
        CSV, FIXED_LENGTH_FIELDS, DBF, CUSTOMER_CODE
    }
    
    public enum LineSeparator {
        WINDOWS_MAC, UNIX, AUTO, WINDOWS_ONLY
    }

    public DataInputModule() {
        super("DataInput1");
    }

    @Override
    protected void validateConfiguration(List<String> errors) {
        if (inputFilePath == null || inputFilePath.isEmpty()) {
            errors.add("Input file path is missing");
        } else if (!Files.isRegularFile(Path.of(inputFilePath))) {
            errors.add("Input file does not exist: " + inputFilePath);
        }
        if (rootArrayName == null || rootArrayName.isBlank()) {
            errors.add("Root array name cannot be blank");
        }
        if (fileType != FileType.CSV) {
            errors.add("Only CSV is implemented in the current milestone");
        }
        if (fieldSeparators == null || fieldSeparators.isEmpty()) {
            errors.add("Field separator cannot be empty");
        }
        try {
            Charset.forName(textEncoding);
        } catch (Exception exception) {
            errors.add("Unsupported text encoding: " + textEncoding);
        }
    }

    @Override
    protected DataInputResult readData() throws Exception {
        List<String[]> records = new ArrayList<>();
        String[] columnNames = null;

        try (BufferedReader br = Files.newBufferedReader(
                Path.of(inputFilePath), Charset.forName(textEncoding))) {
            String line;
            int currentLine = 0;
            boolean isFirstDataRow = true;

            while ((line = br.readLine()) != null) {
                currentLine++;
                
                if (currentLine <= skipFirstLines) {
                    continue;
                }

                String separator = Pattern.quote(fieldSeparators);
                String regex = separator + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
                String[] values = line.split(regex, -1);

                for (int i = 0; i < values.length; i++) {
                    values[i] = removeOuterQualifier(values[i]);
                }

                if (useFirstRecordAsFieldNames && isFirstDataRow) {
                    columnNames = values;
                    isFirstDataRow = false;
                } else {
                    if (columnNames == null) {
                        columnNames = defaultColumnNames(values.length);
                    }
                    records.add(values);
                    isFirstDataRow = false;
                }
            }
        }

        if (columnNames == null) {
            columnNames = new String[0];
        }
        ExecutionContext.DataType[] types = new ExecutionContext.DataType[columnNames.length];
        java.util.Arrays.fill(types, ExecutionContext.DataType.STRING);
        return new DataInputResult(rootArrayName, columnNames, types, records);
    }

    private String removeOuterQualifier(String value) {
        String trimmed = value.trim();
        if (!textQualifiers.isEmpty()
                && trimmed.startsWith(textQualifiers)
                && trimmed.endsWith(textQualifiers)
                && trimmed.length() >= textQualifiers.length() * 2) {
            return trimmed.substring(
                    textQualifiers.length(), trimmed.length() - textQualifiers.length());
        }
        return trimmed;
    }

    private String[] defaultColumnNames(int count) {
        String[] names = new String[count];
        for (int index = 0; index < count; index++) {
            names[index] = "Field" + (index + 1);
        }
        return names;
    }

    // Getters y Setters
    public void setInputFilePath(String path) { this.inputFilePath = path; }
    public void setFieldSeparators(String sep) { this.fieldSeparators = sep; }
    public void setSkipFirstLines(int skip) { this.skipFirstLines = skip; }
    public void setTextEncoding(String encoding) { this.textEncoding = encoding; }
    public void setRootArrayName(String rootArrayName) { this.rootArrayName = rootArrayName; }
}
