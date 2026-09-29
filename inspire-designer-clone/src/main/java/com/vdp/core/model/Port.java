package com.vdp.core.model;

import java.util.UUID;

public class Port {
    private final String id;
    private final String name;
    private final PortType type;
    
    public enum PortType {
        DATA, SHEET
    }

    public Port(String id, String name, PortType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Port(String name, PortType type) {
        this.id = name;
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public PortType getType() { return type; }
}
