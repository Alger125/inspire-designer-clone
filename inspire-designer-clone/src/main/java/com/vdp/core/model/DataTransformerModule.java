package com.vdp.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data Transformer behavior inspired by Inspire Designer 14 manual, section 5.21.
 *
 * <p>Every mapping produces one target field per record. The expression may be:</p>
 * <ul>
 *   <li>a literal text ({@code Hello}),</li>
 *   <li>a field reference ({@code {Name}}), possibly mixed with text
 *       ({@code {First} {Last}}),</li>
 *   <li>a function wrapping the above: {@code UPPER(..)}, {@code LOWER(..)},
 *       {@code TRIM(..)}, {@code LENGTH(..)}.</li>
 * </ul>
 */
public final class DataTransformerModule implements InspireModule {

    private final String id = UUID.randomUUID().toString();
    private String name = "DataTransformer1";
    private final List<Port> inputPorts =
            List.of(new Port("DataInput", Port.PortType.DATA));
    private final List<Port> outputPorts =
            List.of(new Port("DataOutput", Port.PortType.DATA));

    private final List<FieldMapping> mappings = new ArrayList<>();
    /** When true, source fields without a mapping are copied unchanged. */
    private boolean keepUnmappedFields;

    /** One row of the mapping table: expression/source -> target field. */
    public static final class FieldMapping {
        private String targetField;
        private String expression;

        public FieldMapping(String targetField, String expression) {
            this.targetField = targetField == null ? "" : targetField;
            this.expression = expression == null ? "" : expression;
        }

        public String getTargetField() { return targetField; }
        public void setTargetField(String targetField) {
            this.targetField = targetField == null ? "" : targetField;
        }
        public String getExpression() { return expression; }
        public void setExpression(String expression) {
            this.expression = expression == null ? "" : expression;
        }
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be blank");
        }
        this.name = name;
    }

    @Override public String getModuleFamily() { return "Data Processing"; }
    @Override public List<Port> getInputPorts() { return inputPorts; }
    @Override public List<Port> getOutputPorts() { return outputPorts; }

    public List<FieldMapping> getMappings() { return mappings; }
    public boolean isKeepUnmappedFields() { return keepUnmappedFields; }
    public void setKeepUnmappedFields(boolean keepUnmappedFields) {
        this.keepUnmappedFields = keepUnmappedFields;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (mappings.isEmpty()) {
            errors.add("At least one field mapping is required");
        }
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < mappings.size(); i++) {
            FieldMapping m = mappings.get(i);
            if (m.getTargetField().isBlank()) {
                errors.add("Target field cannot be blank in mapping " + (i + 1));
            } else if (seen.contains(m.getTargetField())) {
                errors.add("Duplicate target field '" + m.getTargetField() + "'");
            } else {
                seen.add(m.getTargetField());
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
        DataNode input = context.getData("DataInput");
        if (input == null) input = context.getRoot();
        if (input == null || input.getType() != DataNode.NodeType.ARRAY) {
            throw new IllegalStateException(
                    "DataTransformer requires an ARRAY node at input 'DataInput'.");
        }

        DataNode output = DataNode.arrayNode(input.getName());
        for (DataNode record : input.getChildren()) {
            DataNode target = DataNode.objectNode(record.getName());
            if (keepUnmappedFields) {
                for (DataNode child : record.getChildren()) {
                    if (mappings.stream().noneMatch(
                            m -> m.getTargetField().equals(child.getName()))) {
                        target.addChild(child.deepCopy());
                    }
                }
            }
            for (FieldMapping mapping : mappings) {
                target.addChild(DataNode.valueNode(
                        mapping.getTargetField(), evaluate(mapping.getExpression(), record)));
            }
            output.addChild(target);
        }
        context.setData("DataOutput", output);
        context.setRoot(output.deepCopy());
    }

    /** Evaluates an expression against a single record. Package-visible for tests. */
    static String evaluate(String expression, DataNode record) {
        return com.vdp.core.expression.ExpressionEngine.evaluate(expression, record);
    }
}
