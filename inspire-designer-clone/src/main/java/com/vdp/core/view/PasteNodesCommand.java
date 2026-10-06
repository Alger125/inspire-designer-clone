package com.vdp.core.view;

import com.vdp.core.model.InspireModule;
import com.vdp.core.model.Port;
import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import com.vdp.core.view.command.Command;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vdp.core.model.WorkflowSerializer;

class PasteNodesCommand implements Command {
    private final WorkflowCanvas canvas;
    private final Runnable repaintCallback;

    private final List<WorkflowNode> pastedNodes = new ArrayList<>();
    private final Map<String, String> pastedModuleTypes = new HashMap<>();
    private final List<WorkflowConnection> pastedConnections = new ArrayList<>();
    private final List<Workflow.ConnectionMemento> mementos = new ArrayList<>();
    
    private boolean isFirstExecution = true;

    private final List<ConnectionArgs> connectionsToMake = new ArrayList<>();
    
    private record ConnectionArgs(InspireModule source, Port sPort, InspireModule target, Port tPort) {}

    public PasteNodesCommand(WorkflowCanvas canvas, List<WorkflowCanvas.ClipboardItem> items, List<WorkflowConnection> connections, int offset, Runnable repaintCallback) {
        this.canvas = canvas;
        this.repaintCallback = repaintCallback;

        Map<String, String> oldIdToNewId = new HashMap<>();

        for (WorkflowCanvas.ClipboardItem item : items) {
            InspireModule newModule = WorkflowSerializer.createModule(item.type, item.config);
            oldIdToNewId.put(item.originalId, newModule.getId());

            Color accent = canvas.isProcessingModule(item.type) ? InspireTheme.DATA_PROCESSING : InspireTheme.DATA_INPUT;
            boolean acceptsInput = canvas.isProcessingModule(item.type);
            WorkflowNode newNode = new WorkflowNode(item.type, newModule, accent, acceptsInput);

            newNode.setLocation(item.position.x + offset, item.position.y + offset);
            pastedNodes.add(newNode);
            pastedModuleTypes.put(newModule.getId(), item.type);
        }

        for (WorkflowConnection conn : connections) {
            String newSourceId = oldIdToNewId.get(conn.sourceModuleId());
            String newTargetId = oldIdToNewId.get(conn.targetModuleId());
            if (newSourceId != null && newTargetId != null) {
                InspireModule sourceModule = pastedNodes.stream().map(WorkflowNode::getModule).filter(m -> m.getId().equals(newSourceId)).findFirst().orElse(null);
                InspireModule targetModule = pastedNodes.stream().map(WorkflowNode::getModule).filter(m -> m.getId().equals(newTargetId)).findFirst().orElse(null);
                if (sourceModule != null && targetModule != null) {
                    Port sPort = sourceModule.getOutputPorts().stream().filter(p -> p.getId().equals(conn.sourcePortId())).findFirst().orElse(null);
                    Port tPort = targetModule.getInputPorts().stream().filter(p -> p.getId().equals(conn.targetPortId())).findFirst().orElse(null);
                    if (sPort != null && tPort != null) {
                        connectionsToMake.add(new ConnectionArgs(sourceModule, sPort, targetModule, tPort));
                    }
                }
            }
        }
    }

    @Override
    public void execute() {
        if (isFirstExecution) {
            for (WorkflowNode node : pastedNodes) {
                canvas.installNodeInteraction(node);
                canvas.addNodeDirectly(node, pastedModuleTypes.get(node.getModule().getId()));
            }
            for (ConnectionArgs args : connectionsToMake) {
                WorkflowConnection conn = canvas.getWorkflow().connect(args.source, args.sPort, args.target, args.tPort);
                pastedConnections.add(conn);
            }
            isFirstExecution = false;
        } else {
            for (WorkflowNode node : pastedNodes) {
                canvas.addNodeDirectly(node, pastedModuleTypes.get(node.getModule().getId()));
            }
            for (Workflow.ConnectionMemento memento : mementos) {
                canvas.getWorkflow().restoreConnection(memento);
            }
        }
        
        canvas.selectNodes(pastedNodes);
        
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public void undo() {
        mementos.clear();
        for (WorkflowConnection conn : pastedConnections) {
            Workflow.ConnectionMemento memento = canvas.getWorkflow().removeConnection(conn);
            if (memento != null) {
                mementos.add(memento);
            }
        }
        for (WorkflowNode node : pastedNodes) {
            canvas.removeNodeDirectly(node);
        }
        if (repaintCallback != null) repaintCallback.run();
    }

    @Override
    public String getName() {
        return "Paste Modules";
    }
}
