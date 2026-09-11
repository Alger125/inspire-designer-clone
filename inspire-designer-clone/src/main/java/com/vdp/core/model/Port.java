package com.vdp.core.model;

import java.util.UUID;

public class Port {
    private String id;
    private String name;
    private PortType type;
    
    public enum PortType {
        DATA, SHEET
    }

    public Port(String name, PortType type) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public PortType getType() { return type; }
}