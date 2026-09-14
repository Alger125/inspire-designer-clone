package com.vdp.core.model;

import java.time.Instant;

public record LogEntry(
        Instant timestamp,
        Severity severity,
        String description,
        String code) {

    public enum Severity {
        STATUS,
        WARNING,
        ERROR
    }

    public static LogEntry status(String description) {
        return new LogEntry(
                Instant.now(),
                Severity.STATUS,
                description,
                "CLONE-PROOF-001"
        );
    }

    public static LogEntry error(String description) {
        return new LogEntry(
                Instant.now(),
                Severity.ERROR,
                description,
                "CLONE-PROOF-002"
        );
    }
}