package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Common lifecycle for all data input modules. */
public abstract class BaseDataInputModule implements InspireModule {
    private final String id = UUID.randomUUID().toString();
    private String name;

    private final List<Port> outputPorts =
            List.of(new Port("DataOutput", Port.PortType.DATA));

    protected BaseDataInputModule(String name) {
        setName(name);
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be blank");
        }

        this.name = name;
    }

    @Override
    public final String getModuleFamily() {
        return "Data Inputs";
    }

    @Override
    public final List<Port> getInputPorts() {
        return List.of();
    }

    @Override
    public final List<Port> getOutputPorts() {
        return outputPorts;
    }

    @Override
    public final List<String> validate() {
        List<String> errors = new ArrayList<>();
        validateConfiguration(errors);
        return List.copyOf(errors);
    }

    @Override
    public final void execute(ExecutionContext context) {
        Objects.requireNonNull(context, "context");

        List<String> errors = validate();

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }

        try {
            DataInputResult result = readData();

            context.replaceData(
                    result.getRootArrayName(),
                    result.getColumnNames(),
                    result.getColumnTypes(),
                    result.getRecords()
            );

        } catch (RuntimeException exception) {
            throw exception;

        } catch (Exception exception) {
            String detail = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();

            throw new IllegalStateException(
                    "Could not execute " + name + ": " + detail,
                    exception
            );
        }
    }

    protected abstract void validateConfiguration(List<String> errors);

    protected abstract DataInputResult readData() throws Exception;
}