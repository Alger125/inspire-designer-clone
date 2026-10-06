package com.vdp.core.view;

import com.vdp.core.view.command.Command;

import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.WorkflowCanvas;

class DeleteConnectionCommand implements Command {
    private final WorkflowCanvas canvas;
    private final WorkflowConnection connection;
    private final Runnable repaintCallback;
    private Workflow.ConnectionMemento memento;

    public DeleteConnectionCommand(WorkflowCanvas canvas, WorkflowConnection connection, Runnable repaintCallback) {
        this.canvas = canvas;
        this.connection = connection;
        this.repaintCallback = repaintCallback;
    }

    @Override
    public void execute() {
        if (memento == null) {
            memento = canvas.getWorkflow().removeConnection(connection);
        } else {
            canvas.getWorkflow().removeConnection(connection);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        if (memento != null) {
            canvas.getWorkflow().restoreConnection(memento);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Delete Connection";
    }
}
