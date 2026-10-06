package com.vdp.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DataConcatenatorTest {
    
    private DataConcatenatorModule concatenator;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        concatenator = new DataConcatenatorModule();
        context = new ExecutionContext();
    }

    @Test
    void testDefaultNumberOfInputs() {
        assertEquals(2, concatenator.getNumberOfInputs());
        assertEquals(2, concatenator.getInputPorts().size());
        assertEquals("Input1", concatenator.getInputPorts().get(0).getId());
        assertEquals("Input2", concatenator.getInputPorts().get(1).getId());
        assertEquals(1, concatenator.getOutputPorts().size());
        assertEquals("DataOutput", concatenator.getOutputPorts().get(0).getId());
    }

    @Test
    void testChangeNumberOfInputs() {
        concatenator.setNumberOfInputs(5);
        assertEquals(5, concatenator.getNumberOfInputs());
        assertEquals(5, concatenator.getInputPorts().size());
        assertEquals("Input1", concatenator.getInputPorts().get(0).getId());
        assertEquals("Input5", concatenator.getInputPorts().get(4).getId());
    }
    
    @Test
    void testValidation() {
        assertTrue(concatenator.validate().isEmpty());
    }

    @Test
    void testExecuteConcatenation() {
        // Prepare Input1
        DataNode input1 = DataNode.arrayNode("Records1");
        DataNode row1 = DataNode.objectNode("R1");
        row1.addChild(DataNode.valueNode("Id", "1"));
        row1.addChild(DataNode.valueNode("Name", "Alice"));
        input1.addChild(row1);
        
        // Prepare Input2
        DataNode input2 = DataNode.arrayNode("Records2");
        DataNode row2 = DataNode.objectNode("R2");
        row2.addChild(DataNode.valueNode("Id", "2"));
        row2.addChild(DataNode.valueNode("Name", "Bob"));
        input2.addChild(row2);
        
        context.setData("Input1", input1);
        context.setData("Input2", input2);
        
        concatenator.execute(context);
        
        DataNode output = context.getData("DataOutput");
        assertNotNull(output);
        assertEquals(DataNode.NodeType.ARRAY, output.getType());
        assertEquals(2, output.getChildren().size());
        
        assertEquals("1", output.getChildren().get(0).getChild("Id").getValue());
        assertEquals("Alice", output.getChildren().get(0).getChild("Name").getValue());
        
        assertEquals("2", output.getChildren().get(1).getChild("Id").getValue());
        assertEquals("Bob", output.getChildren().get(1).getChild("Name").getValue());
    }

    @Test
    void testExecuteWithMissingInput() {
        concatenator.setNumberOfInputs(3);
        
        // Prepare Input1
        DataNode input1 = DataNode.arrayNode("Records1");
        DataNode row1 = DataNode.objectNode("R1");
        row1.addChild(DataNode.valueNode("Id", "1"));
        input1.addChild(row1);
        
        // Input2 is missing
        
        // Prepare Input3
        DataNode input3 = DataNode.arrayNode("Records3");
        DataNode row3 = DataNode.objectNode("R3");
        row3.addChild(DataNode.valueNode("Id", "3"));
        input3.addChild(row3);
        
        context.setData("Input1", input1);
        context.setData("Input3", input3);
        
        concatenator.execute(context);
        
        DataNode output = context.getData("DataOutput");
        assertNotNull(output);
        assertEquals(2, output.getChildren().size());
        assertEquals("1", output.getChildren().get(0).getChild("Id").getValue());
        assertEquals("3", output.getChildren().get(1).getChild("Id").getValue());
    }
}
