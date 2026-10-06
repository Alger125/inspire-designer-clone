package com.vdp.core.view;

import com.vdp.core.model.DataGeneratorModule;
import com.vdp.core.model.DataFilterModule;
import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.command.CommandManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandSystemTest {

    private WorkflowCanvas canvas;
    private CommandManager commandManager;
    private DataGeneratorModule generator;
    private DataFilterModule filter;

    @BeforeEach
    void setUp() {
        canvas = new WorkflowCanvas(n -> {}, msg -> {});
        canvas.newWorkflow("Test Workflow");
        commandManager = canvas.getCommandManager();

        generator = new DataGeneratorModule();
        filter = new DataFilterModule();
    }

    @Test
    void testMoveSingleNodeUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());
        assertNotNull(node);

        Point initial = new Point(10, 10);
        Point target = new Point(50, 50);
        node.setLocation(target);

        MoveNodesCommand cmd = new MoveNodesCommand(
                Map.of(node, initial),
                Map.of(node, target),
                null
        );

        commandManager.executeCommand(cmd);
        assertEquals(target, node.getLocation());

        commandManager.undo();
        assertEquals(initial, node.getLocation());

        commandManager.redo();
        assertEquals(target, node.getLocation());
    }

    @Test
    void test0pxMovementCreatesNoCommand() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());

        java.awt.event.MouseAdapter listener = (java.awt.event.MouseAdapter) node.getMouseListeners()[0];
        
        java.awt.event.MouseEvent pressEvent = new java.awt.event.MouseEvent(node, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, 10, 1, false);
        listener.mousePressed(pressEvent);

        java.awt.event.MouseEvent releaseEvent = new java.awt.event.MouseEvent(node, java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 10, 10, 1, false);
        listener.mouseReleased(releaseEvent);

        assertEquals(0, commandManager.getUndoStackSize());
    }

    @Test
    void testUndoNewCommandClearsRedoStack() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());

        MoveNodesCommand cmd1 = new MoveNodesCommand(Map.of(node, new Point(0,0)), Map.of(node, new Point(10,10)), null);
        MoveNodesCommand cmd2 = new MoveNodesCommand(Map.of(node, new Point(10,10)), Map.of(node, new Point(20,20)), null);

        commandManager.executeCommand(cmd1);
        commandManager.undo();
        
        assertTrue(commandManager.canRedo());

        commandManager.executeCommand(cmd2);
        assertFalse(commandManager.canRedo());
    }
    
    @Test
    void testNewWorkflowClearsStacks() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());
        MoveNodesCommand cmd = new MoveNodesCommand(Map.of(node, new Point(0,0)), Map.of(node, new Point(10,10)), null);
        commandManager.executeCommand(cmd);
        
        assertTrue(commandManager.canUndo());
        canvas.newWorkflow("Another");
        assertFalse(commandManager.canUndo());
    }
    
    @Test
    void testDeleteSingleNodeUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());
        
        DeleteNodesCommand cmd = new DeleteNodesCommand(canvas, List.of(node), null);
        commandManager.executeCommand(cmd);
        
        assertNull(canvas.findNodeById(generator.getId()));
        assertFalse(canvas.getWorkflow().getModules().contains(generator));
        
        commandManager.undo();
        assertNotNull(canvas.findNodeById(generator.getId()));
        assertTrue(canvas.getWorkflow().getModules().contains(generator));
        
        commandManager.redo();
        assertNull(canvas.findNodeById(generator.getId()));
    }
    
    @Test
    void testDeleteWithConnections() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        canvas.addNodeDirectly(new WorkflowNode("Data Filter", filter, null, true), "Data Filter");
        
        WorkflowConnection conn = canvas.getWorkflow().connect(generator, generator.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        
        WorkflowNode filterNode = canvas.findNodeById(filter.getId());
        
        DeleteNodesCommand cmd = new DeleteNodesCommand(canvas, List.of(filterNode), null);
        commandManager.executeCommand(cmd);
        
        assertFalse(canvas.getWorkflow().getConnections().contains(conn));
        
        commandManager.undo();
        assertTrue(canvas.getWorkflow().getConnections().stream()
            .anyMatch(c -> c.sourceModuleId().equals(generator.getId()) && c.targetModuleId().equals(filter.getId())));
    }
    @Test
    void testMoveMultipleNodesUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        canvas.addNodeDirectly(new WorkflowNode("Data Filter", filter, null, true), "Data Filter");
        WorkflowNode node1 = canvas.findNodeById(generator.getId());
        WorkflowNode node2 = canvas.findNodeById(filter.getId());
        
        Point initial1 = new Point(10, 10);
        Point initial2 = new Point(20, 20);
        Point target1 = new Point(50, 50);
        Point target2 = new Point(60, 60);

        MoveNodesCommand cmd = new MoveNodesCommand(
                Map.of(node1, initial1, node2, initial2),
                Map.of(node1, target1, node2, target2),
                null
        );

        commandManager.executeCommand(cmd);
        assertEquals(target1, node1.getLocation());
        assertEquals(target2, node2.getLocation());

        commandManager.undo();
        assertEquals(initial1, node1.getLocation());
        assertEquals(initial2, node2.getLocation());

        commandManager.redo();
        assertEquals(target1, node1.getLocation());
        assertEquals(target2, node2.getLocation());
    }

    @Test
    void testDeleteMultipleNodesUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        canvas.addNodeDirectly(new WorkflowNode("Data Filter", filter, null, true), "Data Filter");
        WorkflowNode node1 = canvas.findNodeById(generator.getId());
        WorkflowNode node2 = canvas.findNodeById(filter.getId());

        DeleteNodesCommand cmd = new DeleteNodesCommand(canvas, List.of(node1, node2), null);
        commandManager.executeCommand(cmd);

        assertNull(canvas.findNodeById(generator.getId()));
        assertNull(canvas.findNodeById(filter.getId()));

        commandManager.undo();
        assertNotNull(canvas.findNodeById(generator.getId()));
        assertNotNull(canvas.findNodeById(filter.getId()));

        commandManager.redo();
        assertNull(canvas.findNodeById(generator.getId()));
        assertNull(canvas.findNodeById(filter.getId()));
    }

    @Test
    void testDeleteConnectionUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        canvas.addNodeDirectly(new WorkflowNode("Data Filter", filter, null, true), "Data Filter");
        WorkflowConnection conn = canvas.getWorkflow().connect(generator, generator.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));

        DeleteConnectionCommand cmd = new DeleteConnectionCommand(canvas, conn, null);
        commandManager.executeCommand(cmd);
        
        assertFalse(canvas.getWorkflow().getConnections().contains(conn));

        commandManager.undo();
        assertTrue(canvas.getWorkflow().getConnections().stream().anyMatch(c -> c.sourceModuleId().equals(generator.getId()) && c.targetModuleId().equals(filter.getId())));

        commandManager.redo();
        assertFalse(canvas.getWorkflow().getConnections().stream().anyMatch(c -> c.sourceModuleId().equals(generator.getId()) && c.targetModuleId().equals(filter.getId())));
    }

    @Test
    void testOpenWorkflowClearsUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());
        commandManager.executeCommand(new MoveNodesCommand(Map.of(node, new Point(0,0)), Map.of(node, new Point(10,10)), null));
        assertTrue(commandManager.canUndo());

        canvas.loadFrom(new com.vdp.core.model.WorkflowSerializer.LoadResult(new Workflow("New"), Map.of(), Map.of()));
        assertFalse(commandManager.canUndo());
        assertFalse(commandManager.canRedo());
    }

    @Test
    void testMultipleUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        WorkflowNode node = canvas.findNodeById(generator.getId());

        Point p1 = new Point(0, 0);
        Point p2 = new Point(10, 10);
        Point p3 = new Point(20, 20);

        commandManager.executeCommand(new MoveNodesCommand(Map.of(node, p1), Map.of(node, p2), null));
        commandManager.executeCommand(new MoveNodesCommand(Map.of(node, p2), Map.of(node, p3), null));

        assertEquals(p3, node.getLocation());

        commandManager.undo();
        assertEquals(p2, node.getLocation());

        commandManager.undo();
        assertEquals(p1, node.getLocation());

        commandManager.redo();
        assertEquals(p2, node.getLocation());

        commandManager.redo();
        assertEquals(p3, node.getLocation());
    }

    @Test
    void testAddModuleCommandUndoRedo() {
        WorkflowNode node = new WorkflowNode("Data Generator", generator, null, false);
        AddModuleCommand cmd = new AddModuleCommand(canvas, node, "Data Generator", null);
        
        commandManager.executeCommand(cmd);
        assertNotNull(canvas.findNodeById(generator.getId()));
        
        commandManager.undo();
        assertNull(canvas.findNodeById(generator.getId()));

        commandManager.redo();
        assertNotNull(canvas.findNodeById(generator.getId()));
        assertSame(node, canvas.findNodeById(generator.getId())); // Should be same instance
    }

    @Test
    void testConnectCommandUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        canvas.addNodeDirectly(new WorkflowNode("Data Filter", filter, null, true), "Data Filter");

        ConnectCommand cmd = new ConnectCommand(canvas, generator, generator.getOutputPorts().get(0), filter, filter.getInputPorts().get(0), null);
        commandManager.executeCommand(cmd);

        assertEquals(1, canvas.getWorkflow().getConnections().size());

        commandManager.undo();
        assertEquals(0, canvas.getWorkflow().getConnections().size());

        commandManager.redo();
        assertEquals(1, canvas.getWorkflow().getConnections().size());
    }

    @Test
    void testPasteNodesCommandUndoRedo() {
        canvas.addNodeDirectly(new WorkflowNode("Data Generator", generator, null, false), "Data Generator");
        canvas.addNodeDirectly(new WorkflowNode("Data Filter", filter, null, true), "Data Filter");
        WorkflowConnection conn = canvas.getWorkflow().connect(generator, generator.getOutputPorts().get(0), filter, filter.getInputPorts().get(0));
        
        canvas.selectNodes(List.of(canvas.findNodeById(generator.getId()), canvas.findNodeById(filter.getId())));
        canvas.copySelectedNodes();

        PasteNodesCommand cmd = new PasteNodesCommand(canvas, new java.util.ArrayList<>(canvas.getClipboardItems()), new java.util.ArrayList<>(canvas.getClipboardConnections()), 30, null);
        commandManager.executeCommand(cmd);

        assertEquals(4, canvas.getWorkflow().getModules().size());
        assertEquals(2, canvas.getWorkflow().getConnections().size());

        List<WorkflowNode> currentNodes = new java.util.ArrayList<>();
        for (java.awt.Component c : canvas.getComponents()) {
            if (c instanceof WorkflowNode wn) currentNodes.add(wn);
        }

        commandManager.undo();
        assertEquals(2, canvas.getWorkflow().getModules().size());
        assertEquals(1, canvas.getWorkflow().getConnections().size());

        commandManager.redo();
        assertEquals(4, canvas.getWorkflow().getModules().size());
        assertEquals(2, canvas.getWorkflow().getConnections().size());

        List<WorkflowNode> newNodes = new java.util.ArrayList<>();
        for (java.awt.Component c : canvas.getComponents()) {
            if (c instanceof WorkflowNode wn) newNodes.add(wn);
        }

        assertTrue(currentNodes.containsAll(newNodes) && newNodes.containsAll(currentNodes));
    }
}