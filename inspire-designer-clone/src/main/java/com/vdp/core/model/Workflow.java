package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Workflow {
    private String id;
    private String name;
    private List<InspireModule> modules;

    public Workflow(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.modules = new ArrayList<>();
    }

    public void addModule(InspireModule module) {
        this.modules.add(module);
    }
    
    public List<InspireModule> getModules() {
        return modules;
    }
}