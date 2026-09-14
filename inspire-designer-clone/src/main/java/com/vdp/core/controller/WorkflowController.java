package com.vdp.core.controller;

import com.vdp.core.model.ExecutionContext;
import com.vdp.core.model.ExecutionSnapshot;
import com.vdp.core.model.InspireModule;
import com.vdp.core.model.LogEntry;
import com.vdp.core.model.ProofRunResult;
import com.vdp.core.model.ValidationMessage;
import com.vdp.core.model.Workflow;
import com.vdp.core.model.WorkflowConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorkflowController {

    public List<ValidationMessage> validateWorkflow(Workflow workflow) {
        Objects.requireNonNull(workflow, "workflow");

        List<ValidationMessage> messages = new ArrayList<>();
        List<InspireModule> modules = workflow.getModules();

        if (modules.isEmpty()) {
            messages.add(
                    ValidationMessage.workflowError(
                            "Add at least one module before validating the workflow."
                    )
            );

            return List.copyOf(messages);
        }

        validateModules(modules, messages);

        for (String error : workflow.validateConnections()) {
            messages.add(
                    ValidationMessage.workflowError(error)
            );
        }

        return List.copyOf(messages);
    }

    public ProofRunResult runProof(
            Workflow workflow,
            String preferredModuleId) {

        Objects.requireNonNull(workflow, "workflow");

        List<ValidationMessage> messages = new ArrayList<>();
        List<LogEntry> logs = new ArrayList<>();

        Map<String, ExecutionSnapshot> snapshots =
                new LinkedHashMap<>();

        messages.addAll(validateWorkflow(workflow));

        if (hasErrors(messages)) {
            return new ProofRunResult(
                    messages,
                    logs,
                    snapshots,
                    preferredModuleId
            );
        }

        Map<String, ExecutionContext> moduleOutputs =
                new LinkedHashMap<>();

        /*
         * Ejecuta todos los módulos en el orden real del grafo:
         * Data Generator antes que Data Filter.
         */
        for (InspireModule module
                : workflow.getTopologicalOrder()) {

            try {
                ExecutionContext context =
                        inputContextFor(
                                workflow,
                                module,
                                moduleOutputs
                        );

                module.execute(context);

                /*
                 * Guardamos una copia para que el siguiente módulo
                 * reciba estos datos sin modificar el resultado anterior.
                 */
                moduleOutputs.put(
                        module.getId(),
                        context.copy()
                );

                ExecutionSnapshot snapshot =
                        ExecutionSnapshot.from(
                                module,
                                context
                        );

                snapshots.put(
                        module.getId(),
                        snapshot
                );

                logs.add(
                        LogEntry.status(
                                module.getName()
                                + " executed: "
                                + snapshot.getRecordCount()
                                + " records"
                        )
                );

            } catch (RuntimeException exception) {
                String detail =
                        exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();

                messages.add(
                        ValidationMessage.error(
                                module,
                                "Execution failed: " + detail
                        )
                );

                logs.add(
                        LogEntry.error(
                                module.getName()
                                + " failed: "
                                + detail
                        )
                );
            }
        }

        String initialModuleId =
                snapshots.containsKey(preferredModuleId)
                ? preferredModuleId
                : snapshots.keySet()
                        .stream()
                        .findFirst()
                        .orElse(null);

        return new ProofRunResult(
                messages,
                logs,
                snapshots,
                initialModuleId
        );
    }

    private ExecutionContext inputContextFor(
            Workflow workflow,
            InspireModule module,
            Map<String, ExecutionContext> moduleOutputs) {

        List<WorkflowConnection> incoming =
                workflow.getIncomingConnections(
                        module.getId()
                );

        /*
         * Los módulos sin entrada, como Data Generator,
         * comienzan con un contexto vacío.
         */
        if (incoming.isEmpty()) {
            return new ExecutionContext();
        }

        if (incoming.size() > 1) {
            throw new IllegalStateException(
                    "Multiple inputs are not supported by "
                    + module.getName()
            );
        }

        WorkflowConnection connection =
                incoming.get(0);

        ExecutionContext sourceContext =
                moduleOutputs.get(
                        connection.sourceModuleId()
                );

        if (sourceContext == null) {
            throw new IllegalStateException(
                    "The upstream module did not produce data for "
                    + module.getName()
            );
        }

        /*
         * Data Filter recibe una copia de los datos
         * producidos por Data Generator.
         */
        return sourceContext.copy();
    }

    private void validateModules(
            List<InspireModule> modules,
            List<ValidationMessage> messages) {

        for (InspireModule module : modules) {
            try {
                for (String error : module.validate()) {
                    messages.add(
                            ValidationMessage.error(
                                    module,
                                    error
                            )
                    );
                }

            } catch (RuntimeException exception) {
                String detail =
                        exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();

                messages.add(
                        ValidationMessage.error(
                                module,
                                "Validation failed unexpectedly: "
                                + detail
                        )
                );
            }
        }
    }

    private boolean hasErrors(
            List<ValidationMessage> messages) {

        return messages.stream()
                .anyMatch(message ->
                        message.severity()
                        == ValidationMessage.Severity.ERROR
                );
    }
}