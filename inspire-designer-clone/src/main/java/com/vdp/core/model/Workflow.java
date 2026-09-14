package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Workflow {
    private final String id;
    private final String name;
    private final List<InspireModule> modules;

    public Workflow(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.modules = new ArrayList<>();
    }

    public void addModule(InspireModule module) {
        this.modules.add(module);
    }

    public void removeModule(InspireModule module) {
        this.modules.remove(module);
    }

    public String getId() { return id; }

    public String getName() { return name; }
    
    public List<InspireModule> getModules() {
        return modules;
    }
}
