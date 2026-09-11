package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataInputModule implements InspireModule {
    private String id;
    private String name = "DataInput1"; 
    private List<Port> outputPorts;

    // --- INPUT FILE TAB ---
    private String rootArrayName = "Records"; 
    private String inputFilePath;
    private FileType fileType = FileType.CSV; 
    private boolean useOnlyFirstRecordsInProof = false;
    private int recordCount = 0;

    // --- PROPERTIES TAB ---
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
        this.id = UUID.randomUUID().toString();
        this.outputPorts = new ArrayList<>();
        this.outputPorts.add(new Port("DataOutput", Port.PortType.DATA));
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public String getModuleFamily() { return "Data Inputs"; }

    @Override
    public List<Port> getInputPorts() { return new ArrayList<>(); } // No tiene entradas

    @Override
    public List<Port> getOutputPorts() { return outputPorts; }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (inputFilePath == null || inputFilePath.isEmpty()) {
            errors.add("Error: Input file path is missing in " + name);
        }
        return errors;
    }

    @Override
    public void execute(ExecutionContext context) {
        // La lógica de parseo irá aquí en la Entrega E1
    }

    public void setInputFilePath(String path) { this.inputFilePath = path; }
    public void setFieldSeparators(String sep) { this.fieldSeparators = sep; }
}