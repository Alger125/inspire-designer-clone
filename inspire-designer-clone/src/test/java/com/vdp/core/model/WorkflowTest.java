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
        DataFilterModule filter1 = new DataFilterModule();
        DataFilterModule filter2 = new DataFilterModule();
        
        workflow.addModule(filter1);
        workflow.addModule(filter2);

        Port f1Out = filter1.getOutputPorts().get(0);
        Port f2In = filter2.getInputPorts().get(0);
        
        workflow.connect(filter1, f1Out, filter2, f2In);
        
        // Duplicate connection (exact same source and target)
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
            workflow.connect(filter1, f1Out, filter2, f2In)
        );
        assertEquals("This connection already exists", e1.getMessage());
        
        // Prevent cycles
        Port f2Out = filter2.getOutputPorts().get(0);
        Port f1In = filter1.getInputPorts().get(0);
        
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> 
            workflow.connect(filter2, f2Out, filter1, f1In)
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
        
        assertTrue(result.getValidationMessages().isEmpty(), "Should not have validation errors");
        assertEquals(2, result.getSnapshots().get(f1.getId()).getRecordCount());
        assertEquals(3, result.getSnapshots().get(f2.getId()).getRecordCount());
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
        
        assertTrue(result.getValidationMessages().isEmpty());
        // Records should be 4 and 5
        assertEquals(2, result.getSnapshots().get(sorter.getId()).getRecordCount());
    }
    
    @Test
    void testDisableElseRemovesConnection() {
        Workflow workflow = new Workflow("Test");
        DataGeneratorModule gen = new DataGeneratorModule();
        DataFilterModule filter = new DataFilterModule();
        DataSorterModule sorter = new DataSorterModule();
        
        workflow.addModule(gen);
        workflow.addModule(filter);
        workflow.addModule(sorter);
        
        Port elsePort = filter.getOutputPorts().stream().filter(p -> p.getId().equals("Else")).findFirst().get();
        Port sorterIn = sorter.getInputPorts().get(0);
        
        workflow.connect(gen, gen.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        workflow.connect(filter, elsePort, sorter, sorterIn);
        assertEquals(2, workflow.getConnections().size());
        
        // Disable Else port
        filter.setCreateElseOutput(false);
        // Ensure connection is invalid now
        List<String> errors = workflow.validateConnections();
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Source port 'Else' not found on module")));
        
        // UI should call this to clean up
        workflow.cleanInvalidConnections();
        assertEquals(1, workflow.getConnections().size());
        assertEquals(gen.getId(), workflow.getConnections().get(0).sourceModuleId());
    }

    @Test
    void testMatchedAndElseToDifferentModules() {
        Workflow workflow = new Workflow("Routing");
        DataGeneratorModule gen = new DataGeneratorModule();
        gen.setTo(6);
        
        DataFilterModule filter = new DataFilterModule();
        filter.getCriteria().get(0).setFilterValue("3");
        filter.getCriteria().get(0).setCondition(DataFilterModule.Condition.BIGGER_THAN);
        
        DataSorterModule s1 = new DataSorterModule();
        DataSorterModule s2 = new DataSorterModule();
        
        workflow.addModule(gen);
        workflow.addModule(filter);
        workflow.addModule(s1);
        workflow.addModule(s2);
        
        Port matchedPort = filter.getOutputPorts().stream().filter(p -> p.getId().equals("Matched")).findFirst().get();
        Port elsePort = filter.getOutputPorts().stream().filter(p -> p.getId().equals("Else")).findFirst().get();
        
        workflow.connect(gen, gen.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        workflow.connect(filter, matchedPort, s1, s1.getInputPorts().get(0));
        workflow.connect(filter, elsePort, s2, s2.getInputPorts().get(0));
        
        assertEquals(3, workflow.getConnections().size());
        assertTrue(workflow.validateConnections().isEmpty());
        
        WorkflowController controller = new WorkflowController();
        ProofRunResult result = controller.runProof(workflow, gen.getId());
        
        assertTrue(result.getValidationMessages().isEmpty());
        // S1 gets Matched: 4, 5, 6 (3 records)
        assertEquals(3, result.getSnapshots().get(s1.getId()).getRecordCount());
        // S2 gets Else: 1, 2, 3 (3 records)
        assertEquals(3, result.getSnapshots().get(s2.getId()).getRecordCount());
    }

    @Test
    void testConnectWithNullArgumentsThrows() {
        Workflow workflow = new Workflow("Test");
        DataFilterModule filter = new DataFilterModule();
        workflow.addModule(filter);
        
        Port fOut = filter.getOutputPorts().get(0);
        
        Exception e = assertThrows(NullPointerException.class, () -> 
            workflow.connect(filter, fOut, null, null)
        );
        assertTrue(e.getMessage().contains("target"));
    }
}
