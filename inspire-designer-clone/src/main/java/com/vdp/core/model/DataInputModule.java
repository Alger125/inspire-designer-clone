package com.vdp.core.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DataInputModule implements InspireModule {
    private String id;
    private String name = "DataInput1"; 
    private List<Port> outputPorts;

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
    public List<Port> getInputPorts() { return new ArrayList<>(); }

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
        System.out.println("--- Ejecutando Modulo: " + name + " ---");
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            int currentLine = 0;
            boolean isFirstDataRow = true;

            while ((line = br.readLine()) != null) {
                currentLine++;
                
                // 1. Saltamos las lineas configuradas en "skipFirstLines"[cite: 2]
                if (currentLine <= skipFirstLines) {
                    continue; 
                }

                // 2. Separar por comas (fieldSeparators), ignorando las que están entre comillas (textQualifiers)[cite: 2]
                String regex = fieldSeparators + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
                String[] values = line.split(regex, -1);

                // 3. Limpiar las comillas de los valores finales[cite: 2]
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].replaceAll("^" + textQualifiers, "")
                                         .replaceAll(textQualifiers + "$", "").trim();
                }

                // 4. Determinar si es cabecera o registro y guardar en el CONTEXTO[cite: 2]
                if (useFirstRecordAsFieldNames && isFirstDataRow) {
                    context.setColumnNames(values);
                    System.out.println("=> ESQUEMA DETECTADO Y GUARDADO: " + Arrays.toString(values));
                    isFirstDataRow = false;
                } else {
                    context.getRecords().add(values);
                    System.out.println("Registro leido en memoria: " + Arrays.toString(values));
                }
            }
        } catch (Exception e) {
            System.out.println("Error fatal al leer el archivo: " + e.getMessage());
        }
    }

    // Getters y Setters
    public void setInputFilePath(String path) { this.inputFilePath = path; }
    public void setFieldSeparators(String sep) { this.fieldSeparators = sep; }
    public void setSkipFirstLines(int skip) { this.skipFirstLines = skip; }
}