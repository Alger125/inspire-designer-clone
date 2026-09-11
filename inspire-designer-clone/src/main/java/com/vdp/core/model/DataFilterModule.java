package com.vdp.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Clon exacto del Data Filter Module (Seccion 5.13)
public class DataFilterModule implements InspireModule {
    private String id;
    private String name = "DataFilter1";
    private List<Port> inputPorts;
    private List<Port> outputPorts;

    // Configuracion principal documentada
    private String fieldName; // Campo a evaluar
    private Condition condition = Condition.EQUALS;
    private String filterValue; // Valor a comparar

    public enum Condition {
        EQUALS, CONTAINS, GREATER_THAN, LESS_THAN // Condiciones documentadas
    }

    public DataFilterModule() {
        this.id = UUID.randomUUID().toString();
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.inputPorts.add(new Port("DataInput", Port.PortType.DATA));
        this.outputPorts.add(new Port("DataOutput", Port.PortType.DATA));
    }

    @Override
    public String getId() { return id; }
    @Override
    public String getName() { return name; }
    @Override
    public String getModuleFamily() { return "Data Processing"; } 
    @Override
    public List<Port> getInputPorts() { return inputPorts; }
    @Override
    public List<Port> getOutputPorts() { return outputPorts; }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (fieldName == null || fieldName.isEmpty()) errors.add("Falta el campo a filtrar en " + name);
        if (filterValue == null) errors.add("Falta el valor de filtro en " + name);
        return errors;
    }

    @Override
    public void execute(ExecutionContext context) {
        System.out.println("\n--- Ejecutando Modulo: " + name + " ---");
        
        String[] headers = context.getColumnNames();
        
        // --- SEGURO DE VIDA: Si no hay datos, detenemos el filtro sin que explote ---
        if (headers == null) {
            System.out.println("Error: No hay datos ni cabeceras en la memoria. Revisa que el archivo CSV se haya leido bien.");
            return;
        }

        int targetIndex = -1;

        // Buscar en que columna esta el campo que queremos filtrar
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(fieldName)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            System.out.println("Error: El campo '" + fieldName + "' no existe en los datos.");
            return;
        }

        List<String[]> filteredRecords = new ArrayList<>();
        
        for (String[] record : context.getRecords()) {
            String cellValue = record[targetIndex].trim();
            boolean keep = false;

            switch (condition) {
                case EQUALS:
                    keep = cellValue.equalsIgnoreCase(filterValue);
                    break;
                case CONTAINS:
                    keep = cellValue.toLowerCase().contains(filterValue.toLowerCase());
                    break;
            }

            if (keep) {
                filteredRecords.add(record);
                System.out.println("Registro CONSERVADO: " + Arrays.toString(record));
            } else {
                System.out.println("Registro BLOQUEADO: " + Arrays.toString(record));
            }
        }
        
        context.setRecords(filteredRecords);
    }

    // Setters para la configuracion
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public void setCondition(Condition condition) { this.condition = condition; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
}