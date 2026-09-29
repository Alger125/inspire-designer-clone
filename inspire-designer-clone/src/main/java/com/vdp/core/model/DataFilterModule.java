package com.vdp.core.model;

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
    
    private final List<Port> inputPorts = List.of(
            new Port("DataInput", "Data Input", Port.PortType.DATA)
    );
    private final List<Port> outputPorts = List.of(
            new Port("Matched", "Matched", Port.PortType.DATA),
            new Port("Else", "Else", Port.PortType.DATA)
    );

    public enum Condition {
        NONE("None"),
        EQUAL_TO("Equal to"),
        CONTAINS("Contains"),
        BEGINS_WITH("Begins with"),
        SMALLER_THAN("Smaller than"),
        BIGGER_THAN("Bigger than");

        private final String displayName;
        Condition(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public static class FilterCriterion {
        private String fieldName = "";
        private Condition condition = Condition.NONE;
        private String filterValue = "";
        private boolean ignoreCase = true;
        
        public FilterCriterion() {}
        public FilterCriterion(String field, Condition cond, String val, boolean ignoreCase) {
            this.fieldName = field;
            this.condition = cond;
            this.filterValue = val;
            this.ignoreCase = ignoreCase;
        }

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public Condition getCondition() { return condition; }
        public void setCondition(Condition condition) { this.condition = condition; }
        public String getFilterValue() { return filterValue; }
        public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
        public boolean isIgnoreCase() { return ignoreCase; }
        public void setIgnoreCase(boolean ignoreCase) { this.ignoreCase = ignoreCase; }
    }

    private final List<FilterCriterion> criteria = new ArrayList<>();
    private boolean invertCondition = false;
    private boolean allowMultipleValues = false;

    public DataFilterModule() {
        // Default single criterion for backward compatibility UI
        criteria.add(new FilterCriterion("Value", Condition.NONE, "", true));
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        this.name = name;
    }

    @Override public String getModuleFamily() { return "Data Processing"; }
    @Override public List<Port> getInputPorts() { return inputPorts; }
    @Override public List<Port> getOutputPorts() { return outputPorts; }

    // --- Backward compatibility getters and setters for the UI ---
    public String getFieldName() { return criteria.get(0).getFieldName(); }
    public void setFieldName(String fieldName) { criteria.get(0).setFieldName(fieldName); }
    public Condition getCondition() { return criteria.get(0).getCondition(); }
    public void setCondition(Condition condition) { criteria.get(0).setCondition(condition); }
    public String getFilterValue() { return criteria.get(0).getFilterValue(); }
    public void setFilterValue(String filterValue) { criteria.get(0).setFilterValue(filterValue); }
    // -------------------------------------------------------------

    public List<FilterCriterion> getCriteria() { return criteria; }
    public boolean isInvertCondition() { return invertCondition; }
    public void setInvertCondition(boolean invertCondition) { this.invertCondition = invertCondition; }
    public boolean isAllowMultipleValues() { return allowMultipleValues; }
    public void setAllowMultipleValues(boolean allowMultipleValues) { this.allowMultipleValues = allowMultipleValues; }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (FilterCriterion criterion : criteria) {
            if (criterion.condition == Condition.NONE) continue;
            if (criterion.fieldName == null || criterion.fieldName.isBlank()) {
                errors.add("Field name cannot be blank");
            }
            if (criterion.filterValue == null || criterion.filterValue.isBlank()) {
                errors.add("Filter value cannot be blank for field " + criterion.fieldName);
            }
            if ((criterion.condition == Condition.BIGGER_THAN || criterion.condition == Condition.SMALLER_THAN)
                    && criterion.filterValue != null && !criterion.filterValue.isBlank()) {
                try {
                    new BigDecimal(criterion.filterValue.trim());
                } catch (NumberFormatException exception) {
                    errors.add("Filter value must be numeric for " + criterion.condition + " on " + criterion.fieldName);
                }
            }
        }
        return List.copyOf(errors);
    }

    @Override
    public void execute(ExecutionContext context) {
        Objects.requireNonNull(context, "context");
        List<String> errors = validate();
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));

        // Attempt to read from specific "DataInput", fallback to root for older workflows
        DataNode inputData = context.getData("DataInput");
        if (inputData == null) inputData = context.getRoot();
        
        if (inputData == null || inputData.getType() != DataNode.NodeType.ARRAY) {
            throw new IllegalStateException("DataFilter requires the input context to be an ARRAY node.");
        }

        DataNode matchedRoot = DataNode.arrayNode(null);
        DataNode elseRoot = DataNode.arrayNode(null);

        for (DataNode record : inputData.getChildren()) {
            if (record.getType() != DataNode.NodeType.OBJECT) continue;
            
            boolean matchesAll = true;
            for (FilterCriterion criterion : criteria) {
                if (criterion.condition == Condition.NONE) continue;
                
                DataNode fieldNode = record.getChild(criterion.fieldName);
                // Si el registro no tiene el campo, en el Inspire manual esto puede ser False.
                String cellValue = (fieldNode != null && fieldNode.getValue() != null) ? fieldNode.getValue() : "";
                
                if (!matches(cellValue, criterion)) {
                    matchesAll = false;
                    break;
                }
            }
            
            boolean finalDecision = invertCondition ? !matchesAll : matchesAll;
            if (finalDecision) {
                matchedRoot.addChild(record.deepCopy());
            } else {
                elseRoot.addChild(record.deepCopy());
            }
        }
        
        context.setData("Matched", matchedRoot);
        context.setData("Else", elseRoot);
        
        // For backwards compatibility until all UI is updated, set root to matched
        context.setRoot(matchedRoot.deepCopy());
    }

    private boolean matches(String cellValue, FilterCriterion criterion) {
        String val = criterion.ignoreCase ? cellValue.toLowerCase() : cellValue;
        String filterVal = criterion.ignoreCase ? criterion.filterValue.toLowerCase() : criterion.filterValue;

        return switch (criterion.condition) {
            case NONE -> true;
            case EQUAL_TO -> textValues(filterVal).stream().anyMatch(val::equals);
            case CONTAINS -> textValues(filterVal).stream().anyMatch(val::contains);
            case BEGINS_WITH -> textValues(filterVal).stream().anyMatch(val::startsWith);
            case SMALLER_THAN -> number(cellValue).compareTo(number(criterion.filterValue)) < 0;
            case BIGGER_THAN -> number(cellValue).compareTo(number(criterion.filterValue)) > 0;
        };
    }

    private List<String> textValues(String filterValue) {
        if (!allowMultipleValues) return List.of(filterValue);
        return Arrays.stream(filterValue.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    private BigDecimal number(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Value '" + value + "' is not numeric", exception);
        }
    }
}
