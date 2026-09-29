package com.vdp.core.model;

import java.math.BigDecimal;
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

    private String fieldName = "";
    private Direction direction = Direction.ASCENDING;

    public enum Direction {
        ASCENDING("Ascending"),
        DESCENDING("Descending");

        private final String displayName;

        Direction(String displayName) {
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
    public Direction getDirection() { return direction; }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName == null ? "" : fieldName;
    }

    public void setDirection(Direction direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (fieldName == null || fieldName.isBlank()) {
            errors.add("Field name cannot be blank");
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

        DataNode root = context.getRoot();
        if (root == null || root.getType() != DataNode.NodeType.ARRAY) {
            throw new IllegalStateException("DataSorter requires the root context to be an ARRAY node.");
        }

        List<DataNode> sorted = new ArrayList<>(root.getChildren());

        Comparator<DataNode> comparator = buildComparator();
        sorted.sort(comparator);

        root.getChildren().clear();
        for (DataNode node : sorted) {
            root.addChild(node);
        }
    }

    private int findFieldIndex(String[] columnNames) {
        for (int index = 0; index < columnNames.length; index++) {
            if (columnNames[index].equalsIgnoreCase(fieldName.trim())) return index;
        }
        return -1;
    }

    private Comparator<DataNode> buildComparator() {
        Comparator<DataNode> comparator = (node1, node2) -> {
            DataNode child1 = node1.getChild(fieldName);
            DataNode child2 = node2.getChild(fieldName);
            String val1 = child1 != null && child1.getValue() != null ? child1.getValue() : "";
            String val2 = child2 != null && child2.getValue() != null ? child2.getValue() : "";
            
            // Try numeric sort first, fallback to string sort
            try {
                BigDecimal d1 = new BigDecimal(val1.trim());
                BigDecimal d2 = new BigDecimal(val2.trim());
                return d1.compareTo(d2);
            } catch (NumberFormatException e) {
                return String.CASE_INSENSITIVE_ORDER.compare(val1, val2);
            }
        };
        return direction == Direction.DESCENDING ? comparator.reversed() : comparator;
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }
}
