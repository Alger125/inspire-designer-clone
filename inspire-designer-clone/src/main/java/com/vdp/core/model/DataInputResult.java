package com.vdp.core.model;

import java.util.Objects;

public final class DataInputResult {
    private final DataNode rootNode;

    public DataInputResult(DataNode rootNode) {
        this.rootNode = Objects.requireNonNull(rootNode, "rootNode");
    }

    public DataNode getRootNode() {
        return rootNode;
    }
}
