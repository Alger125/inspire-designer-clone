package com.vdp.core.view.command;

import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.WorkflowCanvas;
import com.vdp.core.view.WorkflowNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteNodesCommand implements Command {
    private final WorkflowCanvas canvas;
    private final List<WorkflowNode> deletedNodes;
    private final List<WorkflowConnection> deletedConnections;
    private final Map<String, String> deletedModuleTypes;
    private final Runnable repaintCallback;

    public DeleteNodesCommand(WorkflowCanvas canvas, List<WorkflowNode> nodesToDelete, Runnable repaintCallback) {
        this.canvas = canvas;
        this.deletedNodes = new ArrayList<>(nodesToDelete);
        this.deletedConnections = new ArrayList<>();
        this.deletedModuleTypes = new HashMap<>();
        this.repaintCallback = repaintCallback;

        for (WorkflowNode node : deletedNodes) {
            String id = node.getModule().getId();
            for (WorkflowConnection conn : canvas.getWorkflow().getConnections()) {
                if ((conn.sourceModuleId().equals(id) || conn.targetModuleId().equals(id)) 
                        && !deletedConnections.contains(conn)) {
                    deletedConnections.add(conn);
                }
            }
            deletedModuleTypes.put(id, canvas.getModuleTypes().get(id));
        }
    }

    @Override
    public void execute() {
        for (WorkflowNode node : deletedNodes) {
            canvas.removeNodeDirectly(node);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        for (WorkflowNode node : deletedNodes) {
            canvas.addNodeDirectly(node, deletedModuleTypes.get(node.getModule().getId()));
        }
        for (WorkflowConnection conn : deletedConnections) {
            canvas.getWorkflow().restoreConnection(conn);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Delete Modules";
    }
}
