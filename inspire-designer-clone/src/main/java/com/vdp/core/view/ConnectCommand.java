package com.vdp.core.view;

import com.vdp.core.model.InspireModule;
import com.vdp.core.model.Port;
import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.command.Command;

class ConnectCommand implements Command {
    private final WorkflowCanvas canvas;
    private final InspireModule source;
    private final Port sourcePort;
    private final InspireModule target;
    private final Port targetPort;
    private final Runnable repaintCallback;
    private Workflow.ConnectionMemento memento;
    private WorkflowConnection connection; // Need this to pass to removeConnection

    public ConnectCommand(WorkflowCanvas canvas, InspireModule source, Port sourcePort, InspireModule target, Port targetPort, Runnable repaintCallback) {
        this.canvas = canvas;
        this.source = source;
        this.sourcePort = sourcePort;
        this.target = target;
        this.targetPort = targetPort;
        this.repaintCallback = repaintCallback;
    }

    @Override
    public void execute() {
        if (memento == null) {
            connection = canvas.getWorkflow().connect(source, sourcePort, target, targetPort);
        } else {
            canvas.getWorkflow().restoreConnection(memento);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        if (connection != null) {
            memento = canvas.getWorkflow().removeConnection(connection);
            if (repaintCallback != null) repaintCallback.run();
        }
    }

    @Override
    public String getName() {
        return "Connect Modules";
    }
}
