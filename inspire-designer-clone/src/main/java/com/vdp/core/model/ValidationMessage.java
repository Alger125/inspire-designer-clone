package com.vdp.core.model;

public record ValidationMessage(
        String moduleId,
        String moduleName,
        Severity severity,
        String message) {

    public enum Severity {
        ERROR,
        WARNING
    }

    public static ValidationMessage error(
            InspireModule module,
            String message) {

        return new ValidationMessage(
                module.getId(),
                module.getName(),
                Severity.ERROR,
                message
        );
    }

    public static ValidationMessage workflowError(String message) {
        return new ValidationMessage(
                "",
                "Workflow",
                Severity.ERROR,
                message
        );
    }
}