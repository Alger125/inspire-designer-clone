package com.vdp.core.view;

import com.vdp.core.view.command.Command;

import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.WorkflowCanvas;
import com.vdp.core.view.WorkflowNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DeleteNodesCommand implements Command {
    private final WorkflowCanvas canvas;
    private final List<WorkflowNode> deletedNodes;
    private final List<WorkflowConnection> deletedConnections;
    private final List<Workflow.ConnectionMemento> mementos;
    private final Map<String, String> deletedModuleTypes;
    private final Runnable repaintCallback;

    public DeleteNodesCommand(WorkflowCanvas canvas, List<WorkflowNode> nodesToDelete, Runnable repaintCallback) {
        this.canvas = canvas;
        this.deletedNodes = new ArrayList<>(nodesToDelete);
        this.deletedConnections = new ArrayList<>();
        this.mementos = new ArrayList<>();
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
        if (mementos.isEmpty()) {
            for (WorkflowConnection conn : deletedConnections) {
                Workflow.ConnectionMemento memento = canvas.getWorkflow().removeConnection(conn);
                if (memento != null) {
                    mementos.add(memento);
                }
            }
        } else {
            for (WorkflowConnection conn : deletedConnections) {
                canvas.getWorkflow().removeConnection(conn);
            }
        }
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
        for (Workflow.ConnectionMemento memento : mementos) {
            canvas.getWorkflow().restoreConnection(memento);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Delete Modules";
    }
}
