package com.vdp.core.view;

import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.DataSorterModule;
import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowCanvasCopyPasteTest {

    private WorkflowCanvas canvas;

    @BeforeEach
    void setUp() {
        canvas = new WorkflowCanvas(node -> {}, msg -> {});
    }

    private void addModuleToCanvas(com.vdp.core.model.InspireModule module, int x, int y, String type) {
        WorkflowNode node = new WorkflowNode(type, module, java.awt.Color.GRAY, true);
        node.setLocation(x, y);
        canvas.getWorkflow().addModule(module);
        canvas.registerModuleType(module.getId(), type);
        canvas.add(node);
    }

    private WorkflowNode getNodeForId(String id) {
        for (Component c : canvas.getComponents()) {
            if (c instanceof WorkflowNode node && node.getModule().getId().equals(id)) {
                return node;
            }
        }
        return null;
    }

    @Test
    void testCopySingleModule() {
        DataFilterModule filter = new DataFilterModule();
        filter.setName("TestFilter");
        addModuleToCanvas(filter, 100, 100, "Data Filter");
        
        WorkflowNode node = getNodeForId(filter.getId());
        canvas.selectOnly(node);
        
        canvas.copySelectedNodes();
        canvas.pasteClipboardNodes();
        
        assertEquals(2, canvas.getWorkflow().getModules().size());
        
        // Find the new module
        WorkflowNode newNode = null;
        for (Component c : canvas.getComponents()) {
            if (c instanceof WorkflowNode n && !n.getModule().getId().equals(filter.getId())) {
                newNode = n;
            }
        }
        
        assertNotNull(newNode);
        assertNotEquals(filter.getId(), newNode.getModule().getId());
        assertEquals("TestFilter", newNode.getModule().getName());
        assertEquals(new Point(130, 130), newNode.getLocation());
        
        // Check new node is selected
        assertEquals(newNode.getModule().getId(), canvas.getSelectedModuleId());
    }

    @Test
    void testCopyMultipleModulesNoConnections() {
        DataFilterModule f1 = new DataFilterModule();
        DataFilterModule f2 = new DataFilterModule();
        addModuleToCanvas(f1, 100, 100, "Data Filter");
        addModuleToCanvas(f2, 200, 200, "Data Filter");
        
        canvas.selectOnly(getNodeForId(f1.getId()));
        canvas.toggleSelection(getNodeForId(f2.getId()));
        
        canvas.copySelectedNodes();
        canvas.pasteClipboardNodes();
        
        assertEquals(4, canvas.getWorkflow().getModules().size());
    }

    @Test
    void testCopyGroupWithInternalConnections() {
        DataGeneratorModule gen = new DataGeneratorModule();
        DataFilterModule filter = new DataFilterModule();
        DataSorterModule sorter = new DataSorterModule();
        
        addModuleToCanvas(gen, 10, 10, "Data Generator");
        addModuleToCanvas(filter, 100, 100, "Data Filter");
        addModuleToCanvas(sorter, 200, 200, "Data Sorter");
        
        canvas.getWorkflow().connect(gen, gen.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        canvas.getWorkflow().connect(filter, filter.getOutputPorts().get(0), sorter, sorter.getInputPorts().get(0));
        
        assertEquals(2, canvas.getWorkflow().getConnections().size());
        
        // Select all
        canvas.selectOnly(getNodeForId(gen.getId()));
        canvas.toggleSelection(getNodeForId(filter.getId()));
        canvas.toggleSelection(getNodeForId(sorter.getId()));
        
        canvas.copySelectedNodes();
        canvas.pasteClipboardNodes();
        
        assertEquals(6, canvas.getWorkflow().getModules().size());
        assertEquals(4, canvas.getWorkflow().getConnections().size());
    }

    @Test
    void testCopyPartialGroup_ShouldNotCopyExternalConnections() {
        DataGeneratorModule gen = new DataGeneratorModule();
        DataFilterModule filter = new DataFilterModule();
        DataSorterModule sorter = new DataSorterModule();
        
        addModuleToCanvas(gen, 10, 10, "Data Generator");
        addModuleToCanvas(filter, 100, 100, "Data Filter");
        addModuleToCanvas(sorter, 200, 200, "Data Sorter");
        
        canvas.getWorkflow().connect(gen, gen.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        canvas.getWorkflow().connect(filter, filter.getOutputPorts().get(0), sorter, sorter.getInputPorts().get(0));
        
        // Select only Filter + Sorter
        canvas.selectOnly(getNodeForId(filter.getId()));
        canvas.toggleSelection(getNodeForId(sorter.getId()));
        
        canvas.copySelectedNodes();
        canvas.pasteClipboardNodes();
        
        // Only internal connection Filter->Sorter should be copied
        // Total connections should be 3 (2 original, 1 copied)
        assertEquals(3, canvas.getWorkflow().getConnections().size());
        
        // Check that generator is not connected to the new filter
        long connectionsFromGenerator = canvas.getWorkflow().getOutgoingConnections(gen.getId()).size();
        assertEquals(1, connectionsFromGenerator);
    }
}
