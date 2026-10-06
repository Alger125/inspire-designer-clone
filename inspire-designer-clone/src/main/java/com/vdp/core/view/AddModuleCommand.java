package com.vdp.core.view;

import com.vdp.core.view.command.Command;

public class AddModuleCommand implements Command {
    private final WorkflowCanvas canvas;
    private final WorkflowNode node;
    private final String type;
    private final Runnable repaintCallback;

    public AddModuleCommand(WorkflowCanvas canvas, WorkflowNode node, String type, Runnable repaintCallback) {
        this.canvas = canvas;
        this.node = node;
        this.type = type;
        this.repaintCallback = repaintCallback;
    }

    @Override
    public void execute() {
        canvas.addNodeDirectly(node, type);
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        canvas.removeNodeDirectly(node);
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Add Module";
    }
}
