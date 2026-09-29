package com.vdp.core.model;

import java.util.Objects;

/** Deep immutable copy of one module's execution context. */
public final class ExecutionSnapshot {
    private final String moduleId;
    private final String moduleName;
    private final DataNode rootNode;

    private ExecutionSnapshot(
            String moduleId,
            String moduleName,
            DataNode rootNode) {
        this.moduleId = Objects.requireNonNull(moduleId, "moduleId");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.rootNode = rootNode != null ? rootNode.deepCopy() : null;
    }

    public static ExecutionSnapshot from(InspireModule module, ExecutionContext context) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(context, "context");
        DataNode rootNode = context.getRoot();
        if (rootNode == null) {
            throw new IllegalStateException("Execution did not produce a root data node");
        }
        return new ExecutionSnapshot(
                module.getId(),
                module.getName(),
                rootNode);
    }

    public String getModuleId() { return moduleId; }
    public String getModuleName() { return moduleName; }
    
    /** Returns a deep copy of the root node from this snapshot. */
    public DataNode getRootNode() { 
        return rootNode != null ? rootNode.deepCopy() : null; 
    }
    
    public int getRecordCount() { 
        if (rootNode != null && rootNode.getType() == DataNode.NodeType.ARRAY) {
            return rootNode.getChildren().size();
        }
        return 0;
    }
}
