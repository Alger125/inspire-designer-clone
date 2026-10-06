package com.vdp.core.view.command;

import com.vdp.core.view.WorkflowNode;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class MoveNodesCommand implements Command {
    private final Map<WorkflowNode, Point> initialPositions;
    private final Map<WorkflowNode, Point> finalPositions;
    private final Runnable repaintCallback;

    public MoveNodesCommand(Map<WorkflowNode, Point> initialPositions, Map<WorkflowNode, Point> finalPositions, Runnable repaintCallback) {
        this.initialPositions = new HashMap<>(initialPositions);
        this.finalPositions = new HashMap<>(finalPositions);
        this.repaintCallback = repaintCallback;
    }

    @Override
    public void execute() {
        for (Map.Entry<WorkflowNode, Point> entry : finalPositions.entrySet()) {
            entry.getKey().setLocation(entry.getValue());
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        for (Map.Entry<WorkflowNode, Point> entry : initialPositions.entrySet()) {
            entry.getKey().setLocation(entry.getValue());
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Move Modules";
    }
}
