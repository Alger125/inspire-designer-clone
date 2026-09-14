package com.vdp.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clase base para todos los módulos de entrada de datos en Inspire Designer.
 * Centraliza la creación del puerto de salida y el manejo del ExecutionContext.
 */
public abstract class BaseDataInputModule implements InspireModule {
    protected String id;
    protected String name;
    protected List<Port> outputPorts;
    protected Map<String, Object> properties; // Almacena la configuración (ej. Array, From, To)

    public BaseDataInputModule(String defaultName) {
        this.id = UUID.randomUUID().toString();
        this.name = defaultName;
        this.outputPorts = new ArrayList<>();
        this.outputPorts.add(new Port("DataOutput", Port.PortType.DATA)); // 1 puerto de salida
        this.properties = new HashMap<>();
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public String getModuleFamily() { return "Data Inputs"; }

    @Override
    public List<Port> getInputPorts() { return new ArrayList<>(); } // 0 puertos de entrada

    @Override
    public List<Port> getOutputPorts() { return outputPorts; }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    // Validación unificada que llama a las reglas específicas de cada hijo
    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            errors.add("Error: Module name cannot be empty.");
        }
        errors.addAll(specificValidation());
        return errors;
    }

    // Ejecución unificada
    @Override
    public void execute(ExecutionContext context) {
        System.out.println("\n--- Ejecutando " + name + " ---");
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            System.out.println("Validacion fallida: " + errors);
            return;
        }

        try {
            // El hijo genera/lee los datos
            List<String[]> generatedRecords = generateOrReadData();
            
            // Reemplaza atómicamente los datos en el contexto
            context.setRecords(generatedRecords);
            context.setColumnNames(getGeneratedColumnNames());
            System.out.println("Exito: Se cargaron " + generatedRecords.size() + " registros en memoria.");
            
        } catch (Exception e) {
            System.out.println("Error critico durante ejecucion: " + e.getMessage());
        }
    }

    // Contratos que los módulos hijos (como DataGenerator) DEBEN cumplir
    protected abstract List<String> specificValidation();
    protected abstract List<String[]> generateOrReadData();
    protected abstract String[] getGeneratedColumnNames();
}