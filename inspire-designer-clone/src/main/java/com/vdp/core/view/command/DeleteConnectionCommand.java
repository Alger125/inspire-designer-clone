package com.vdp.core.view.command;

import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.WorkflowCanvas;

public class DeleteConnectionCommand implements Command {
    private final WorkflowCanvas canvas;
    private final WorkflowConnection connection;
    private final Runnable repaintCallback;

    public DeleteConnectionCommand(WorkflowCanvas canvas, WorkflowConnection connection, Runnable repaintCallback) {
        this.canvas = canvas;
        this.connection = connection;
        this.repaintCallback = repaintCallback;
    }

    @Override
    public void execute() {
        canvas.getWorkflow().removeConnection(connection);
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        canvas.getWorkflow().restoreConnection(connection);
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Delete Connection";
    }
}
