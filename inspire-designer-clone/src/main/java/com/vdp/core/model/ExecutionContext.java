package com.vdp.core.model;

import java.util.Objects;

/**
 * Representa el contexto de ejecución, ahora basado en un árbol jerárquico.
 */
public class ExecutionContext {
    private DataNode root;

    public DataNode getRoot() {
        return root;
    }

    public void setRoot(DataNode root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public ExecutionContext copy() {
        ExecutionContext copy = new ExecutionContext();
        if (this.root != null) {
            copy.setRoot(this.root.deepCopy());
        }
        return copy;
    }
}
