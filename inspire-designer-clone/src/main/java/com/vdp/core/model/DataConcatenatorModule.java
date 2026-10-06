package com.vdp.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;

public class DataConcatenatorModule implements InspireModule {
    private final String id = UUID.randomUUID().toString();
    private String name = "Data Concatenator";
    
    private int numberOfInputs = 2;
    private final List<Port> outputPorts = List.of(new Port("DataOutput", "Data Output", Port.PortType.DATA));
    private final List<Port> inputPorts = new ArrayList<>();

    public DataConcatenatorModule() {
        updatePorts();
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        this.name = name;
    }

    @Override public String getModuleFamily() { return "Data Processing"; }
    @Override public List<Port> getInputPorts() { return inputPorts; }
    @Override public List<Port> getOutputPorts() { return outputPorts; }

    public int getNumberOfInputs() { return numberOfInputs; }
    
    public void setNumberOfInputs(int numberOfInputs) {
        if (numberOfInputs < 1 || numberOfInputs > 100) {
            throw new IllegalArgumentException("Number of inputs must be between 1 and 100");
        }
        this.numberOfInputs = numberOfInputs;
        updatePorts();
    }
    
    private void updatePorts() {
        inputPorts.clear();
        for (int i = 1; i <= numberOfInputs; i++) {
            inputPorts.add(new Port("Input" + i, "Input " + i, Port.PortType.DATA));
        }
    }

    @Override
    public List<String> validate() {
        return Collections.emptyList();
    }

    @Override
    public void execute(ExecutionContext context) {
        Objects.requireNonNull(context, "context");

        DataNode outputRoot = DataNode.arrayNode("Records");
        
        for (int i = 1; i <= numberOfInputs; i++) {
            DataNode inputData = context.getData("Input" + i);
            if (inputData == null) {
                // If an input is not connected, we try to fall back to root for older workflows,
                if (i == 1 && numberOfInputs == 1) {
                   inputData = context.getRoot();
                }
            }
            
            if (inputData != null && inputData.getType() == DataNode.NodeType.ARRAY) {
                for (DataNode record : inputData.getChildren()) {
                    if (record.getType() == DataNode.NodeType.OBJECT) {
                        outputRoot.addChild(record.deepCopy());
                    }
                }
            }
        }
        
        context.setData("DataOutput", outputRoot);
        // For backwards compatibility
        context.setRoot(outputRoot.deepCopy());
    }
}
