package com.vdp.core.model;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class Workflow {
    private final String id;
    private final String name;
    private final List<InspireModule> modules;
    private final List<WorkflowConnection> connections;

    public Workflow(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.modules = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    public void addModule(InspireModule module) {
        Objects.requireNonNull(module, "module");
        if (findModule(module.getId()) != null) {
            throw new IllegalArgumentException("Module already belongs to the workflow");
        }
        modules.add(module);
    }

    public void removeModule(InspireModule module) {
        if (module == null) return;
        connections.removeIf(connection ->
                connection.sourceModuleId().equals(module.getId())
                        || connection.targetModuleId().equals(module.getId()));
        modules.remove(module);
    }

    public String getId() { return id; }

    public String getName() { return name; }
    
    public List<InspireModule> getModules() {
        return List.copyOf(modules);
    }

    public List<WorkflowConnection> getConnections() {
        return List.copyOf(connections);
    }

    public WorkflowConnection connect(
            InspireModule source,
            Port sourcePort,
            InspireModule target,
            Port targetPort) {
        requireMember(source, "Source");
        requireMember(target, "Target");
        Objects.requireNonNull(sourcePort, "sourcePort");
        Objects.requireNonNull(targetPort, "targetPort");
        if (source == target || source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("A module cannot connect to itself");
        }
        if (!source.getOutputPorts().contains(sourcePort)) {
            throw new IllegalArgumentException("The source port must be an output port");
        }
        if (!target.getInputPorts().contains(targetPort)) {
            throw new IllegalArgumentException("The target port must be an input port");
        }
        if (sourcePort.getType() != targetPort.getType()) {
            throw new IllegalArgumentException("Connected ports must have the same data type");
        }

        WorkflowConnection connection = new WorkflowConnection(
                source.getId(), sourcePort.getId(), target.getId(), targetPort.getId());
        if (connections.contains(connection)) {
            throw new IllegalArgumentException("This connection already exists");
        }
        if (connections.stream().anyMatch(existing ->
                existing.targetPortId().equals(targetPort.getId()))) {
            throw new IllegalArgumentException("The input port already has a connection");
        }
        if (hasPath(target.getId(), source.getId())) {
            throw new IllegalArgumentException("The connection would create a cycle");
        }
        connections.add(connection);
        return connection;
    }

    public void removeConnection(WorkflowConnection connection) {
        connections.remove(connection);
    }

    public List<WorkflowConnection> getIncomingConnections(String moduleId) {
        return connections.stream()
                .filter(connection -> connection.targetModuleId().equals(moduleId))
                .toList();
    }

    public List<WorkflowConnection> getOutgoingConnections(String moduleId) {
        return connections.stream()
                .filter(connection -> connection.sourceModuleId().equals(moduleId))
                .toList();
    }

    public InspireModule findModule(String moduleId) {
        if (moduleId == null) return null;
        return modules.stream()
                .filter(module -> module.getId().equals(moduleId))
                .findFirst()
                .orElse(null);
    }

    /** Returns the executable order and fails if the graph contains a cycle. */
    public List<InspireModule> getTopologicalOrder() {
        Map<String, Integer> incomingCount = new LinkedHashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();
        for (InspireModule module : modules) {
            incomingCount.put(module.getId(), 0);
            outgoing.put(module.getId(), new ArrayList<>());
        }
        for (WorkflowConnection connection : connections) {
            if (!incomingCount.containsKey(connection.sourceModuleId())
                    || !incomingCount.containsKey(connection.targetModuleId())) {
                throw new IllegalStateException("A connection references a missing module");
            }
            incomingCount.computeIfPresent(
                    connection.targetModuleId(), (id, count) -> count + 1);
            outgoing.get(connection.sourceModuleId()).add(connection.targetModuleId());
        }

        Queue<String> ready = new ArrayDeque<>();
        incomingCount.forEach((id, count) -> {
            if (count == 0) ready.add(id);
        });
        List<InspireModule> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String moduleId = ready.remove();
            ordered.add(findModule(moduleId));
            for (String targetId : outgoing.get(moduleId)) {
                int remaining = incomingCount.computeIfPresent(
                        targetId, (id, count) -> count - 1);
                if (remaining == 0) ready.add(targetId);
            }
        }
        if (ordered.size() != modules.size()) {
            throw new IllegalStateException("The workflow contains a cycle");
        }
        return List.copyOf(ordered);
    }

    /** Checks that every required input port is connected exactly once. */
    public List<String> validateConnections() {
        List<String> errors = new ArrayList<>();
        for (InspireModule module : modules) {
            for (Port inputPort : module.getInputPorts()) {
                long count = connections.stream()
                        .filter(connection -> connection.targetModuleId().equals(module.getId())
                                && connection.targetPortId().equals(inputPort.getId()))
                        .count();
                if (count == 0) {
                    errors.add(module.getName() + ": input " + inputPort.getName()
                            + " is not connected");
                } else if (count > 1) {
                    errors.add(module.getName() + ": input " + inputPort.getName()
                            + " has multiple connections");
                }
            }
        }
        try {
            getTopologicalOrder();
        } catch (IllegalStateException exception) {
            errors.add(exception.getMessage());
        }
        return List.copyOf(errors);
    }

    private void requireMember(InspireModule module, String role) {
        Objects.requireNonNull(module, role.toLowerCase(java.util.Locale.ROOT));
        if (findModule(module.getId()) != module) {
            throw new IllegalArgumentException(role + " module does not belong to the workflow");
        }
    }

    private boolean hasPath(String startModuleId, String wantedModuleId) {
        Queue<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(startModuleId);
        while (!pending.isEmpty()) {
            String current = pending.remove();
            if (!visited.add(current)) continue;
            if (current.equals(wantedModuleId)) return true;
            for (WorkflowConnection connection : getOutgoingConnections(current)) {
                pending.add(connection.targetModuleId());
            }
        }
        return false;
    }
}
