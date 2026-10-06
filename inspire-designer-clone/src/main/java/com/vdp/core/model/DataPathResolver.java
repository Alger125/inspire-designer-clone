package com.vdp.core.model;

import java.util.Objects;

/**
 * Resolves dot-separated data paths like "Customer.Address.City" against a DataNode.
 * 
 * Part of FASE 4 — UNIFICAR DATA PATHS.
 */
public final class DataPathResolver {

    private DataPathResolver() {
        // Utility class
    }

    /**
     * Resolves the given dot-separated path against the context node.
     * 
     * @param context the root node for the path (usually the record object)
     * @param path the dot-separated path, e.g. "Customer.Address.City"
     * @return the resolved DataNode, or null if the path does not exist or context is null
     */
    public static DataNode resolve(DataNode context, String path) {
        if (context == null || path == null || path.isBlank()) {
            return null;
        }

        String[] parts = path.split("\\.");
        DataNode current = context;

        for (String part : parts) {
            if (current == null) return null;

            if (current.getType() == DataNode.NodeType.OBJECT) {
                current = current.getChild(part);
            } else if (current.getType() == DataNode.NodeType.ARRAY) {
                // If it's an array, Inspire Designer often defaults to looking inside the first element,
                // but for strict path resolution, an array child could be a specific syntax (like arr[0]).
                // For now, if we ask for a field on an array, we'll try to find a child with that name
                // in the array node itself (unlikely) or fail.
                current = current.getChild(part);
            } else {
                // Value node cannot have children
                return null;
            }
        }

        return current;
    }

    /**
     * Resolves the path and returns its value as a string.
     * 
     * @return the value of the resolved node, or an empty string if not found.
     */
    public static String resolveValue(DataNode context, String path) {
        DataNode resolved = resolve(context, path);
        return (resolved != null && resolved.getValue() != null) ? resolved.getValue() : "";
    }
}
