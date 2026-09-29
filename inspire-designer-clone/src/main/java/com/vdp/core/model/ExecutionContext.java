package com.vdp.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representa el contexto de ejecución, soportando múltiples puertos (Matched, Else, etc.).
 */
public class ExecutionContext {
    private final Map<String, DataNode> dataByPort = new HashMap<>();

    public void setData(String portId, DataNode node) {
        dataByPort.put(Objects.requireNonNull(portId, "portId"), node);
    }

    public DataNode getData(String portId) {
        return dataByPort.get(portId);
    }

    // Por compatibilidad temporal mientras refactorizamos
    public DataNode getRoot() {
        if (dataByPort.containsKey("DATA")) {
            return dataByPort.get("DATA");
        }
        if (dataByPort.containsKey("Matched")) {
            return dataByPort.get("Matched");
        }
        if (dataByPort.containsKey("DataInput")) {
            return dataByPort.get("DataInput");
        }
        return dataByPort.values().stream().findFirst().orElse(null);
    }

    public void setRoot(DataNode root) {
        setData("DATA", Objects.requireNonNull(root, "root"));
    }

    public ExecutionContext copy() {
        ExecutionContext copy = new ExecutionContext();
        for (Map.Entry<String, DataNode> entry : dataByPort.entrySet()) {
            copy.setData(entry.getKey(), entry.getValue() != null ? entry.getValue().deepCopy() : null);
        }
        return copy;
    }
}
