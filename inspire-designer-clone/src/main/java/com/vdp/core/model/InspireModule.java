package com.vdp.core.model;

import java.util.List;

public interface InspireModule {
    String getId();
    String getName();
    String getModuleFamily(); // Ej: "Data Inputs", "Data Processing"
    
    List<Port> getInputPorts();
    List<Port> getOutputPorts();
    
    List<String> validate();
    void execute(ExecutionContext context);
}