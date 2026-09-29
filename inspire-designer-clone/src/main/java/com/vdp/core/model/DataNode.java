package com.vdp.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representa la estructura jerárquica de datos en Inspire Designer.
 */
public class DataNode {
    public enum NodeType {
        OBJECT, ARRAY, VALUE
    }

    private final NodeType type;
    private String name;
    private String value; // Utilizado solo si type == VALUE
    private final List<DataNode> children = new ArrayList<>();
    // Mapa interno para búsquedas rápidas en nodos tipo OBJECT
    private final Map<String, DataNode> childMap = new LinkedHashMap<>();

    private DataNode(NodeType type, String name) {
        this.type = Objects.requireNonNull(type, "type");
        this.name = name != null ? name : "";
    }

    public static DataNode objectNode(String name) {
        return new DataNode(NodeType.OBJECT, name);
    }

    public static DataNode arrayNode(String name) {
        return new DataNode(NodeType.ARRAY, name);
    }

    public static DataNode valueNode(String name, String value) {
        DataNode node = new DataNode(NodeType.VALUE, name);
        node.setValue(value);
        return node;
    }

    public NodeType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<DataNode> getChildren() {
        return children;
    }

    public void addChild(DataNode child) {
        Objects.requireNonNull(child, "child");
        children.add(child);
        if (type == NodeType.OBJECT) {
            childMap.put(child.getName(), child);
        }
    }

    public void removeChild(DataNode child) {
        if (child == null) return;
        children.remove(child);
        if (type == NodeType.OBJECT) {
            childMap.remove(child.getName());
        }
    }

    public DataNode getChild(String childName) {
        if (type == NodeType.OBJECT) {
            return childMap.get(childName);
        }
        for (DataNode child : children) {
            if (childName.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    public DataNode deepCopy() {
        DataNode copy = new DataNode(this.type, this.name);
        copy.setValue(this.value);
        for (DataNode child : this.children) {
            copy.addChild(child.deepCopy());
        }
        return copy;
    }
}
