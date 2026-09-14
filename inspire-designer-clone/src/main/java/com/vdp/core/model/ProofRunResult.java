package com.vdp.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable result returned by WorkflowController to any presentation layer. */
public final class ProofRunResult {
    private final List<ValidationMessage> validationMessages;
    private final List<LogEntry> logEntries;
    private final Map<String, ExecutionSnapshot> snapshots;
    private final String initialModuleId;

    public ProofRunResult(
            List<ValidationMessage> validationMessages,
            List<LogEntry> logEntries,
            Map<String, ExecutionSnapshot> snapshots,
            String initialModuleId) {
        this.validationMessages = List.copyOf(validationMessages);
        this.logEntries = List.copyOf(logEntries);
        this.snapshots = Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
        this.initialModuleId = initialModuleId;
    }

    public boolean isSuccessful() {
        return validationMessages.stream()
                .noneMatch(message -> message.severity() == ValidationMessage.Severity.ERROR)
                && !snapshots.isEmpty();
    }

    public List<ValidationMessage> getValidationMessages() { return validationMessages; }
    public List<LogEntry> getLogEntries() { return logEntries; }
    public Map<String, ExecutionSnapshot> getSnapshots() { return snapshots; }
    public Optional<String> getInitialModuleId() { return Optional.ofNullable(initialModuleId); }

    public List<ExecutionSnapshot> getSnapshotsInWorkflowOrder() {
        return new ArrayList<>(snapshots.values());
    }
}
