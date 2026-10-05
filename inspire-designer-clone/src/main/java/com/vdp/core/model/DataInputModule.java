package com.vdp.core.model;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads delimited CSV-like files into an ExecutionContext.
 * Supports headers, configurable encoding, separator, text qualifier,
 * skipped lines, quoted fields and empty values.
 */
public final class DataInputModule extends BaseDataInputModule {

    private String rootArrayName = "Records";
    private String inputFilePath;
    private FileType fileType = FileType.CSV;

    private String textEncoding = "UTF-8";
    private String fieldSeparator = ",";
    private String textQualifier = "\"";
    private int skipFirstLines;
    private boolean useFirstRecordAsFieldNames = true;

    private boolean autoDetect;
    private boolean useOnlyFirstRecordsInProof;
    private int proofRecordCount = 100;
    private boolean rangeForProduction;
    private int startRecord = 1;
    private int endRecord = 1000;

    public enum FileType {
        CSV,
        FIXED_LENGTH_FIELDS,
        DBF,
        CUSTOMER_CODE
    }

    public DataInputModule() {
        super("DataInput1");
    }

    @Override
    protected void validateConfiguration(List<String> errors) {
        if (inputFilePath == null || inputFilePath.isBlank()) {
            errors.add("Input file path is missing.");
        } else {
            try {
                if (!Files.isRegularFile(Path.of(inputFilePath))) {
                    errors.add("Input file does not exist: " + inputFilePath);
                }
            } catch (InvalidPathException exception) {
                errors.add("Input file path is invalid: " + inputFilePath);
            }
        }

        if (rootArrayName == null || rootArrayName.isBlank()) {
            errors.add("Root array name cannot be blank.");
        }

        if (fileType != FileType.CSV) {
            errors.add("Only CSV is implemented in the current milestone.");
        }

        if (fieldSeparator == null || fieldSeparator.length() != 1) {
            errors.add("Field separator must contain exactly one character.");
        }

        if (textQualifier == null || textQualifier.length() > 1) {
            errors.add(
                    "Text qualifier must be empty or contain exactly one character."
            );
        }

        if (skipFirstLines < 0) {
            errors.add("Skipped lines cannot be negative.");
        }

        try {
            Charset.forName(textEncoding);
        } catch (Exception exception) {
            errors.add("Unsupported text encoding: " + textEncoding);
        }
    }

    @Override
    protected DataInputResult readData() throws Exception {
        DataNode rootNode = DataNode.arrayNode(rootArrayName);
        String[] columnNames = null;

        try (BufferedReader reader = Files.newBufferedReader(
                Path.of(inputFilePath),
                Charset.forName(textEncoding))) {

            String line;
            int lineNumber = 0;
            boolean firstDataRecord = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber <= skipFirstLines) {
                    continue;
                }

                char separator = autoDetect ? detectSeparator() : fieldSeparator.charAt(0);
                String[] values = parseLine(line, separator);

                if (useFirstRecordAsFieldNames && firstDataRecord) {
                    columnNames = values;
                } else {
                    if (columnNames == null) {
                        columnNames = defaultColumnNames(values.length);
                    }

                    if (values.length != columnNames.length) {
                        throw new IllegalStateException(
                                "Line " + lineNumber
                                + " contains " + values.length
                                + " fields but the data structure contains "
                                + columnNames.length + "."
                        );
                    }

                    DataNode record = DataNode.objectNode("Record");
                    for (int i = 0; i < columnNames.length; i++) {
                        record.addChild(DataNode.valueNode(columnNames[i], values[i]));
                    }
                    rootNode.addChild(record);
                }

                firstDataRecord = false;
            }
        }

        return new DataInputResult(rootNode);
    }

    private String[] parseLine(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean qualifierEnabled = !textQualifier.isEmpty();
        char qualifier = qualifierEnabled ? textQualifier.charAt(0) : '\0';

        boolean insideQuotedValue = false;

        for (int index = 0; index < line.length(); index++) {
            char currentCharacter = line.charAt(index);

            if (qualifierEnabled && currentCharacter == qualifier) {
                boolean escapedQualifier = insideQuotedValue
                        && index + 1 < line.length()
                        && line.charAt(index + 1) == qualifier;

                if (escapedQualifier) {
                    currentValue.append(qualifier);
                    index++;
                } else {
                    insideQuotedValue = !insideQuotedValue;
                }

            } else if (currentCharacter == separator && !insideQuotedValue) {
                fields.add(currentValue.toString().trim());
                currentValue.setLength(0);

            } else {
                currentValue.append(currentCharacter);
            }
        }

        if (insideQuotedValue) {
            throw new IllegalStateException(
                    "CSV line contains an unclosed text qualifier."
            );
        }

        fields.add(currentValue.toString().trim());

        return fields.toArray(String[]::new);
    }

    private String[] defaultColumnNames(int fieldCount) {
        String[] names = new String[fieldCount];

        for (int index = 0; index < fieldCount; index++) {
            names[index] = "Field" + (index + 1);
        }

        return names;
    }

    public String getRootArrayName() {
        return rootArrayName;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public String getTextEncoding() {
        return textEncoding;
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    public String getTextQualifier() {
        return textQualifier;
    }

    public int getSkipFirstLines() {
        return skipFirstLines;
    }

    public boolean isUseFirstRecordAsFieldNames() {
        return useFirstRecordAsFieldNames;
    }

    public void setRootArrayName(String rootArrayName) {
        this.rootArrayName = rootArrayName;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public void setTextEncoding(String textEncoding) {
        this.textEncoding = textEncoding;
    }

    public void setFieldSeparator(String fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    public void setTextQualifier(String textQualifier) {
        this.textQualifier = textQualifier;
    }

    public void setSkipFirstLines(int skipFirstLines) {
        this.skipFirstLines = skipFirstLines;
    }

    public void setUseFirstRecordAsFieldNames(
            boolean useFirstRecordAsFieldNames) {
        this.useFirstRecordAsFieldNames = useFirstRecordAsFieldNames;
    }

    public boolean isAutoDetect() { return autoDetect; }
    public void setAutoDetect(boolean autoDetect) { this.autoDetect = autoDetect; }

    public boolean isUseOnlyFirstRecordsInProof() { return useOnlyFirstRecordsInProof; }
    public void setUseOnlyFirstRecordsInProof(boolean useOnlyFirstRecordsInProof) { this.useOnlyFirstRecordsInProof = useOnlyFirstRecordsInProof; }

    public int getProofRecordCount() { return proofRecordCount; }
    public void setProofRecordCount(int proofRecordCount) { this.proofRecordCount = proofRecordCount; }

    public boolean isRangeForProduction() { return rangeForProduction; }
    public void setRangeForProduction(boolean rangeForProduction) { this.rangeForProduction = rangeForProduction; }

    public int getStartRecord() { return startRecord; }
    public void setStartRecord(int startRecord) { this.startRecord = startRecord; }

    public int getEndRecord() { return endRecord; }
    public void setEndRecord(int endRecord) { this.endRecord = endRecord; }

    /**
     * Generates the schema (Data Structure) at design time without executing the module.
     * This reads only the headers of the CSV.
     */
    public DataNode getDesignSchema() {
        DataNode root = DataNode.arrayNode(rootArrayName);
        root.setDataType(DataNode.DataType.ARRAY);

        DataNode record = DataNode.objectNode("Record");
        record.setDataType(DataNode.DataType.OBJECT);
        root.addChild(record);

        if (inputFilePath == null || inputFilePath.isBlank()) return root;
        Path path = Path.of(inputFilePath);
        if (!Files.isRegularFile(path)) return root;

        try {
            char separator = autoDetect ? detectSeparator() : fieldSeparator.charAt(0);
            try (BufferedReader reader = Files.newBufferedReader(path, Charset.forName(textEncoding))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber <= skipFirstLines) continue;

                    String[] values = parseLine(line, separator);
                    String[] columnNames = useFirstRecordAsFieldNames ? values : defaultColumnNames(values.length);

                    for (String col : columnNames) {
                        DataNode field = DataNode.valueNode(col, null);
                        field.setDataType(DataNode.DataType.STRING);
                        record.addChild(field);
                    }
                    break; // Just read the first data line to get the schema
                }
            }
        } catch (Exception e) {
            // Ignore errors at design time
        }
        return root;
    }
    private char detectSeparator() throws java.io.IOException {
        char fallback = fieldSeparator != null && fieldSeparator.length() == 1 ? fieldSeparator.charAt(0) : ',';
        return fallback; // Dummy implementation for now, or just fallback
    }
}
