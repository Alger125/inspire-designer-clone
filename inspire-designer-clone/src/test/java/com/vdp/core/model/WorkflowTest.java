package com.vdp.core.model;

import com.vdp.core.controller.WorkflowController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.List;
import java.util.Map;

class WorkflowTest {

    @Test
    void testCyclesAndDuplicateConnectionsPrevented() {
        Workflow workflow = new Workflow("Test");
        DataGeneratorModule gen1 = new DataGeneratorModule();
        DataFilterModule filter = new DataFilterModule();
        
        workflow.addModule(gen1);
        workflow.addModule(filter);

        Port genOut = gen1.getOutputPorts().get(0);
        Port filterIn = filter.getInputPorts().get(0);
        Port filterOut = filter.getOutputPorts().get(0);

        workflow.connect(gen1, genOut, filter, filterIn);
        
        // Duplicate connection
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
            workflow.connect(gen1, genOut, filter, filterIn)
        );
        assertEquals("The input port already has a connection", e1.getMessage());
        
        // Prevent cycles
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> 
            workflow.connect(filter, filterOut, gen1, gen1.getOutputPorts().get(0)) // Using any port to test cycle
        );
        assertEquals("The connection would create a cycle", e2.getMessage());
    }
    
    @Test
    void testParallelWorkflowsExecution() {
        Workflow workflow = new Workflow("Parallel");
        
        DataGeneratorModule g1 = new DataGeneratorModule(); g1.setTo(2);
        DataFilterModule f1 = new DataFilterModule();
        
        DataGeneratorModule g2 = new DataGeneratorModule(); g2.setTo(3);
        DataFilterModule f2 = new DataFilterModule();
        
        workflow.addModule(g1); workflow.addModule(f1);
        workflow.addModule(g2); workflow.addModule(f2);
        
        workflow.connect(g1, g1.getOutputPorts().get(0), f1, f1.getInputPorts().get(0));
        workflow.connect(g2, g2.getOutputPorts().get(0), f2, f2.getInputPorts().get(0));
        
        WorkflowController controller = new WorkflowController();
        ProofRunResult result = controller.runProof(workflow, g1.getId());
        
        assertTrue(result.messages().isEmpty(), "Should not have validation errors");
        assertEquals(2, result.snapshots().get(f1.getId()).getRecordCount());
        assertEquals(3, result.snapshots().get(f2.getId()).getRecordCount());
    }

    @Test
    void testGeneratorFilterSorter() {
        Workflow workflow = new Workflow("GFS");
        
        DataGeneratorModule gen = new DataGeneratorModule(); gen.setTo(5);
        DataFilterModule filter = new DataFilterModule();
        filter.getCriteria().get(0).setFilterValue("3");
        filter.getCriteria().get(0).setCondition(DataFilterModule.Condition.BIGGER_THAN);
        
        DataSorterModule sorter = new DataSorterModule();
        sorter.setFieldName("Value");
        sorter.setDirection(DataSorterModule.Direction.DESCENDING);
        
        workflow.addModule(gen);
        workflow.addModule(filter);
        workflow.addModule(sorter);
        
        workflow.connect(gen, gen.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        workflow.connect(filter, filter.getOutputPorts().get(0), sorter, sorter.getInputPorts().get(0));
        
        WorkflowController controller = new WorkflowController();
        ProofRunResult result = controller.runProof(workflow, sorter.getId());
        
        assertTrue(result.messages().isEmpty());
        // Records should be 4 and 5
        assertEquals(2, result.snapshots().get(sorter.getId()).getRecordCount());
    }
}
