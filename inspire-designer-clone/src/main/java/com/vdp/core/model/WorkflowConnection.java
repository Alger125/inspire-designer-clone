package com.vdp.core.model;

import java.util.Objects;

/** Immutable data-flow connection between one output port and one input port. */
public record WorkflowConnection(
        String sourceModuleId,
        String sourcePortId,
        String targetModuleId,
        String targetPortId) {

    public WorkflowConnection {
        requireText(sourceModuleId, "sourceModuleId");
        requireText(sourcePortId, "sourcePortId");
        requireText(targetModuleId, "targetModuleId");
        requireText(targetPortId, "targetPortId");
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}
