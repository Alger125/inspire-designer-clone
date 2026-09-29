package com.vdp.core.model;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Data Filter behavior from Inspire Designer 14 manual, section 5.13. */
public final class DataFilterModule implements InspireModule {
    private final String id = UUID.randomUUID().toString();
    private String name = "DataFilter1";
    private final List<Port> inputPorts =
            List.of(new Port("DataInput", Port.PortType.DATA));
    private final List<Port> outputPorts =
            List.of(new Port("DataOutput", Port.PortType.DATA));

    private String fieldName = "Value";
    private Condition condition = Condition.NONE;
    private String filterValue = "";
    private boolean invertCondition;
    private boolean allowMultipleValues;

    public enum Condition {
        NONE("None"),
        EQUAL_TO("Equal to"),
        CONTAINS("Contains"),
        BEGINS_WITH("Begins with"),
        SMALLER_THAN("Smaller than"),
        BIGGER_THAN("Bigger than");

        private final String displayName;

        Condition(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be blank");
        }
        this.name = name;
    }

    @Override
    public String getModuleFamily() { return "Data Processing"; }

    @Override
    public List<Port> getInputPorts() { return inputPorts; }

    @Override
    public List<Port> getOutputPorts() { return outputPorts; }

    public String getFieldName() { return fieldName; }
    public Condition getCondition() { return condition; }
    public String getFilterValue() { return filterValue; }
    public boolean isInvertCondition() { return invertCondition; }
    public boolean isAllowMultipleValues() { return allowMultipleValues; }

    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public void setCondition(Condition condition) {
        this.condition = Objects.requireNonNull(condition, "condition");
    }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
    public void setInvertCondition(boolean invertCondition) {
        this.invertCondition = invertCondition;
    }
    public void setAllowMultipleValues(boolean allowMultipleValues) {
        this.allowMultipleValues = allowMultipleValues;
    }

    @Override
    public List<String> validate() {
        if (condition == Condition.NONE) return List.of();
        List<String> errors = new ArrayList<>();
        if (fieldName == null || fieldName.isBlank()) {
            errors.add("Field name cannot be blank");
        }
        if (filterValue == null || filterValue.isBlank()) {
            errors.add("Filter value cannot be blank");
        }
        if ((condition == Condition.BIGGER_THAN || condition == Condition.SMALLER_THAN)
                && filterValue != null && !filterValue.isBlank()) {
            try {
                new BigDecimal(filterValue.trim());
            } catch (NumberFormatException exception) {
                errors.add("Filter value must be numeric for " + condition);
            }
        }
        return List.copyOf(errors);
    }

    @Override
    public void execute(ExecutionContext context) {
        Objects.requireNonNull(context, "context");
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }
        if (condition == Condition.NONE) return;

        DataNode root = context.getRoot();
        if (root == null || root.getType() != DataNode.NodeType.ARRAY) {
            throw new IllegalStateException("DataFilter requires the root context to be an ARRAY node.");
        }

        List<DataNode> filteredRecords = new ArrayList<>();
        
        for (DataNode record : root.getChildren()) {
            if (record.getType() != DataNode.NodeType.OBJECT) {
                continue; // Skip non-objects
            }
            
            DataNode fieldNode = record.getChild(fieldName);
            if (fieldNode == null) {
                throw new IllegalStateException("A record does not contain field '" + fieldName + "'");
            }

            boolean matches = matches(fieldNode.getValue());
            if (invertCondition ? !matches : matches) {
                filteredRecords.add(record.deepCopy());
            }
        }
        
        // Replace children of the root array with the filtered ones
        root.getChildren().clear();
        for (DataNode filtered : filteredRecords) {
            root.addChild(filtered);
        }
    }

    private int findFieldIndex(String[] columnNames) {
        for (int index = 0; index < columnNames.length; index++) {
            if (columnNames[index].equalsIgnoreCase(fieldName.trim())) return index;
        }
        return -1;
    }

    private boolean matches(String cellValue) {
        String value = cellValue == null ? "" : cellValue;
        return switch (condition) {
            case NONE -> true;
            case EQUAL_TO -> textValues().stream().anyMatch(value::equals);
            case CONTAINS -> textValues().stream().anyMatch(value::contains);
            case BEGINS_WITH -> textValues().stream().anyMatch(value::startsWith);
            case SMALLER_THAN -> number(value).compareTo(number(filterValue)) < 0;
            case BIGGER_THAN -> number(value).compareTo(number(filterValue)) > 0;
        };
    }

    private List<String> textValues() {
        if (!allowMultipleValues) return List.of(filterValue);
        return Arrays.stream(filterValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private BigDecimal number(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Value '" + value + "' is not numeric", exception);
        }
    }
}
