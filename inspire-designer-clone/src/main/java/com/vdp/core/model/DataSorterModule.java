package com.vdp.core.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Data Sorter behavior from Inspire Designer 14 manual, section 5.14. */
public final class DataSorterModule implements InspireModule {

    private final String id = UUID.randomUUID().toString();
    private String name = "DataSorter1";
    private final List<Port> inputPorts =
            List.of(new Port("DataInput", Port.PortType.DATA));
    private final List<Port> outputPorts =
            List.of(new Port("DataOutput", Port.PortType.DATA));

    private final List<SortCriterion> criteria = new ArrayList<>();

    public enum Direction {
        ASCENDING("Ascending"),
        DESCENDING("Descending");

        private final String displayName;
        Direction(String displayName) { this.displayName = displayName; }
        @Override
        public String toString() { return displayName; }
    }

    public enum ComparisonType {
        AUTO("Auto"),
        NUMBER("Number"),
        TEXT("Text");

        private final String displayName;
        ComparisonType(String displayName) { this.displayName = displayName; }
        @Override
        public String toString() { return displayName; }
    }

    public enum NullOrder {
        FIRST("First"),
        LAST("Last");

        private final String displayName;
        NullOrder(String displayName) { this.displayName = displayName; }
        @Override
        public String toString() { return displayName; }
    }

    public static final class SortCriterion {
        private String fieldName;
        private Direction direction;
        private ComparisonType comparisonType;
        private boolean ignoreCase;
        private NullOrder nullOrder;

        public SortCriterion(String fieldName, Direction direction, ComparisonType comparisonType, boolean ignoreCase, NullOrder nullOrder) {
            this.fieldName = fieldName == null ? "" : fieldName;
            this.direction = direction == null ? Direction.ASCENDING : direction;
            this.comparisonType = comparisonType == null ? ComparisonType.AUTO : comparisonType;
            this.ignoreCase = ignoreCase;
            this.nullOrder = nullOrder == null ? NullOrder.LAST : nullOrder;
        }

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName == null ? "" : fieldName; }
        
        public Direction getDirection() { return direction; }
        public void setDirection(Direction direction) { this.direction = direction == null ? Direction.ASCENDING : direction; }
        
        public ComparisonType getComparisonType() { return comparisonType; }
        public void setComparisonType(ComparisonType comparisonType) { this.comparisonType = comparisonType == null ? ComparisonType.AUTO : comparisonType; }
        
        public boolean isIgnoreCase() { return ignoreCase; }
        public void setIgnoreCase(boolean ignoreCase) { this.ignoreCase = ignoreCase; }
        
        public NullOrder getNullOrder() { return nullOrder; }
        public void setNullOrder(NullOrder nullOrder) { this.nullOrder = nullOrder == null ? NullOrder.LAST : nullOrder; }
    }

    public DataSorterModule() {
        // Start with one empty criterion by default
        criteria.add(new SortCriterion("", Direction.ASCENDING, ComparisonType.AUTO, true, NullOrder.LAST));
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

    public List<SortCriterion> getCriteria() { return criteria; }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (criteria.isEmpty()) {
            errors.add("At least one sort criterion is required");
        }
        for (int i = 0; i < criteria.size(); i++) {
            SortCriterion c = criteria.get(i);
            if (c.getFieldName() == null || c.getFieldName().isBlank()) {
                errors.add("Field name cannot be blank in criterion " + (i + 1));
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

        DataNode inputData = context.getData("DataInput");
        if (inputData == null && context.getRoot() != null) {
            inputData = context.getRoot(); // Fallback for testing/old workflows
        }

        if (inputData == null || inputData.getType() != DataNode.NodeType.ARRAY) {
            throw new IllegalStateException("DataSorter requires an ARRAY node at input 'DataInput'.");
        }

        // Do not mutate original input
        List<DataNode> sortedList = new ArrayList<>(inputData.getChildren());

        Comparator<DataNode> compositeComparator = buildCompositeComparator();
        sortedList.sort(compositeComparator);

        DataNode sortedRoot = DataNode.arrayNode(inputData.getName());
        for (DataNode node : sortedList) {
            sortedRoot.addChild(node.deepCopy());
        }

        context.setData("DataOutput", sortedRoot);
        context.setRoot(sortedRoot.deepCopy());
    }

    private Comparator<DataNode> buildCompositeComparator() {
        if (criteria.isEmpty()) return (n1, n2) -> 0;
        
        Comparator<DataNode> composite = buildSingleComparator(criteria.get(0));
        for (int i = 1; i < criteria.size(); i++) {
            composite = composite.thenComparing(buildSingleComparator(criteria.get(i)));
        }
        return composite;
    }

    private Comparator<DataNode> buildSingleComparator(SortCriterion crit) {
        Comparator<DataNode> comparator = (node1, node2) -> {
            DataNode child1 = node1.getChild(crit.getFieldName());
            DataNode child2 = node2.getChild(crit.getFieldName());
            
            String val1 = child1 != null ? child1.getValue() : null;
            String val2 = child2 != null ? child2.getValue() : null;
            
            boolean null1 = val1 == null || val1.isEmpty();
            boolean null2 = val2 == null || val2.isEmpty();
            
            if (null1 && null2) return 0;
            if (null1) return crit.getNullOrder() == NullOrder.FIRST ? -1 : 1;
            if (null2) return crit.getNullOrder() == NullOrder.FIRST ? 1 : -1;
            
            ComparisonType type = crit.getComparisonType();
            
            if (type == ComparisonType.AUTO) {
                if (isNumeric(val1) && isNumeric(val2)) {
                    type = ComparisonType.NUMBER;
                } else {
                    type = ComparisonType.TEXT;
                }
            }
            
            if (type == ComparisonType.NUMBER) {
                try {
                    BigDecimal d1 = new BigDecimal(val1.trim());
                    BigDecimal d2 = new BigDecimal(val2.trim());
                    return d1.compareTo(d2);
                } catch (NumberFormatException e) {
                    // Fallback if parsing fails despite type being NUMBER
                    return compareText(val1, val2, crit.isIgnoreCase());
                }
            } else {
                return compareText(val1, val2, crit.isIgnoreCase());
            }
        };
        
        return crit.getDirection() == Direction.DESCENDING ? comparator.reversed() : comparator;
    }
    
    private boolean isNumeric(String str) {
        try {
            new BigDecimal(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private int compareText(String val1, String val2, boolean ignoreCase) {
        if (ignoreCase) {
            return String.CASE_INSENSITIVE_ORDER.compare(val1, val2);
        } else {
            return val1.compareTo(val2);
        }
    }
}
