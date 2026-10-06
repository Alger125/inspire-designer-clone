package com.vdp.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Point;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes and deserializes a {@link Workflow} plus canvas node positions
 * to / from a human-readable JSON file (.json).
 *
 * <p>JSON format version 1:</p>
 * <pre>
 * {
 *   "version": 1,
 *   "name": "My Workflow",
 *   "modules": [ { "serialId", "type", "x", "y", "config": { ... } } ],
 *   "connections": [ { "sourceModuleId", "sourcePortName",
 *                       "targetModuleId", "targetPortName" } ]
 * }
 * </pre>
 */
public final class WorkflowSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int FORMAT_VERSION = 1;

    private WorkflowSerializer() {}

    // ── Result returned by load() ─────────────────────────────────────────────

    /**
     * Everything the canvas needs to reconstruct itself after loading a file.
     *
     * @param workflow      the fully connected workflow model
     * @param moduleTypes   moduleId → palette-type string ("Data Filter", etc.)
     * @param nodePositions moduleId → canvas (x, y) coordinates
     */
    public record LoadResult(
            Workflow workflow,
            Map<String, String> moduleTypes,
            Map<String, Point> nodePositions
    ) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Writes the workflow and canvas layout to {@code file} (creates or overwrites).
     *
     * @param workflow      the model to persist
     * @param moduleTypes   moduleId → type string (maintained by the canvas)
     * @param nodePositions moduleId → screen position (queried from canvas nodes)
     * @param file          destination path (should have .json extension)
     */
    public static void save(
            Workflow workflow,
            Map<String, String> moduleTypes,
            Map<String, Point> nodePositions,
            Path file) throws IOException {

        ObjectNode root = MAPPER.createObjectNode();
        root.put("version", FORMAT_VERSION);
        root.put("name", workflow.getName());

        ArrayNode modulesArray = root.putArray("modules");
        for (InspireModule module : workflow.getModules()) {
            ObjectNode moduleNode = MAPPER.createObjectNode();
            moduleNode.put("serialId", module.getId());
            moduleNode.put("type", moduleTypes.getOrDefault(module.getId(), ""));
            Point pos = nodePositions.getOrDefault(module.getId(), new Point(100, 100));
            moduleNode.put("x", pos.x);
            moduleNode.put("y", pos.y);
            moduleNode.set("config", serializeConfig(module));
            modulesArray.add(moduleNode);
        }

        ArrayNode connectionsArray = root.putArray("connections");
        for (WorkflowConnection conn : workflow.getConnections()) {
            InspireModule source = workflow.findModule(conn.sourceModuleId());
            InspireModule target = workflow.findModule(conn.targetModuleId());
            if (source == null || target == null) continue;
            Port sourcePort = portById(source.getOutputPorts(), conn.sourcePortId());
            Port targetPort = portById(target.getInputPorts(), conn.targetPortId());
            if (sourcePort == null || targetPort == null) continue;

            ObjectNode connNode = MAPPER.createObjectNode();
            connNode.put("sourceModuleId", conn.sourceModuleId());
            connNode.put("sourcePortName", sourcePort.getName());
            connNode.put("targetModuleId", conn.targetModuleId());
            connNode.put("targetPortName", targetPort.getName());
            connectionsArray.add(connNode);
        }

        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
    }

    /**
     * Reads a workflow from {@code file} and returns a {@link LoadResult}
     * ready to hand to the canvas.
     */
    public static LoadResult load(Path file) throws IOException {
        JsonNode root = MAPPER.readTree(file.toFile());

        String workflowName = root.path("name").asText("Workflow");
        Workflow workflow = new Workflow(workflowName);

        Map<String, String> moduleTypes   = new LinkedHashMap<>();
        Map<String, Point>  nodePositions = new LinkedHashMap<>();

        // serialId (from file) → newly created module (real UUID will differ)
        Map<String, InspireModule> bySerialId = new LinkedHashMap<>();

        for (JsonNode moduleNode : root.path("modules")) {
            String serialId = moduleNode.path("serialId").asText();
            String type     = moduleNode.path("type").asText();
            int x           = moduleNode.path("x").asInt(100);
            int y           = moduleNode.path("y").asInt(100);
            JsonNode cfg    = moduleNode.path("config");
            ObjectNode config = cfg.isObject() ? (ObjectNode) cfg : MAPPER.createObjectNode();

            InspireModule module = createModule(type, config);
            if (module == null) continue;

            workflow.addModule(module);
            bySerialId.put(serialId, module);
            moduleTypes.put(module.getId(), type);
            nodePositions.put(module.getId(), new Point(x, y));
        }

        for (JsonNode connNode : root.path("connections")) {
            String srcSerial     = connNode.path("sourceModuleId").asText();
            String srcPortName   = connNode.path("sourcePortName").asText();
            String tgtSerial     = connNode.path("targetModuleId").asText();
            String tgtPortName   = connNode.path("targetPortName").asText();

            InspireModule source = bySerialId.get(srcSerial);
            InspireModule target = bySerialId.get(tgtSerial);
            if (source == null || target == null) continue;

            Port srcPort = portByName(source.getOutputPorts(), srcPortName);
            if (srcPort == null && "DataOutput".equals(srcPortName)) {
                srcPort = portByName(source.getOutputPorts(), "Matched");
            }
            
            Port tgtPort = portByName(target.getInputPorts(), tgtPortName);
            if (tgtPort == null && "DataInput".equals(tgtPortName)) {
                tgtPort = portByName(target.getInputPorts(), "Data Input");
            }
            if (tgtPort == null && "DataInput".equals(tgtPortName)) {
                tgtPort = portByName(target.getInputPorts(), "DataInput");
            }
            if (srcPort == null || tgtPort == null) continue;

            try {
                workflow.connect(source, srcPort, target, tgtPort);
            } catch (IllegalArgumentException ignored) {
                // skip invalid / duplicate connections
            }
        }

        return new LoadResult(workflow, moduleTypes, nodePositions);
    }

    // ── Config serialization ──────────────────────────────────────────────────

    private static ObjectNode serializeConfig(InspireModule module) {
        ObjectNode config = MAPPER.createObjectNode();
        config.put("name", module.getName());

        if (module instanceof DataGeneratorModule m) {
            config.put("arrayName", m.getArrayName());
            config.put("from", m.getFrom());
            config.put("to", m.getTo());

        } else if (module instanceof HttpJsonDataInputModule m) {
            config.put("endpointUrl",         m.getEndpointUrl() == null ? "" : m.getEndpointUrl());
            config.put("rootArrayName",        m.getRootArrayName());
            config.put("jsonArrayPath",        m.getJsonArrayPath());
            config.put("timeoutMilliseconds",  m.getTimeoutMilliseconds());
            ObjectNode headers = config.putObject("headers");
            m.getHeaders().forEach(headers::put);

        } else if (module instanceof DataInputModule m) {
            config.put("rootArrayName",              m.getRootArrayName());
            config.put("inputFilePath",              m.getInputFilePath() == null ? "" : m.getInputFilePath());
            config.put("fileType",                   m.getFileType().name());
            config.put("textEncoding",               m.getTextEncoding());
            config.put("fieldSeparator",             m.getFieldSeparator());
            config.put("textQualifier",              m.getTextQualifier());
            config.put("skipFirstLines",             m.getSkipFirstLines());
            config.put("useFirstRecordAsFieldNames", m.isUseFirstRecordAsFieldNames());

        } else if (module instanceof DataFilterModule m) {
            config.put("invertCondition",    m.isInvertCondition());
            config.put("allowMultipleValues", m.isAllowMultipleValues());
            config.put("createElseOutput", m.isCreateElseOutput());
            
            ArrayNode critArray = config.putArray("criteria");
            for (DataFilterModule.FilterCriterion c : m.getCriteria()) {
                ObjectNode cn = MAPPER.createObjectNode();
                cn.put("fieldName", c.getFieldName());
                cn.put("condition", c.getCondition().name());
                cn.put("filterValue", c.getFilterValue());
                cn.put("ignoreCase", c.isIgnoreCase());
                critArray.add(cn);
            }
            // Write legacy fields for older parsers
            if (!m.getCriteria().isEmpty()) {
                DataFilterModule.FilterCriterion first = m.getCriteria().get(0);
                config.put("fieldName", first.getFieldName());
                config.put("condition", first.getCondition().name());
                config.put("filterValue", first.getFilterValue());
            }

        } else if (module instanceof DataConcatenatorModule m) {
            config.put("numberOfInputs", m.getNumberOfInputs());
        } else if (module instanceof DataSorterModule m) {
            ArrayNode critArray = config.putArray("criteria");
            for (DataSorterModule.SortCriterion c : m.getCriteria()) {
                ObjectNode cn = MAPPER.createObjectNode();
                cn.put("fieldName", c.getFieldName());
                cn.put("direction", c.getDirection().name());
                cn.put("comparisonType", c.getComparisonType().name());
                cn.put("ignoreCase", c.isIgnoreCase());
                cn.put("nullOrder", c.getNullOrder().name());
                critArray.add(cn);
            }
            if (!m.getCriteria().isEmpty()) {
                DataSorterModule.SortCriterion first = m.getCriteria().get(0);
                config.put("fieldName", first.getFieldName());
                config.put("direction", first.getDirection().name());
            }
        }

        return config;
    }

    // ── Config deserialization ────────────────────────────────────────────────

    private static InspireModule createModule(String type, ObjectNode config) {
        return switch (type) {

            case "Data Generator" -> {
                DataGeneratorModule m = new DataGeneratorModule();
                applyName(m::setName, config);
                if (config.has("arrayName")) m.setArrayName(config.get("arrayName").asText());
                if (config.has("from"))      m.setFrom(config.get("from").asInt());
                if (config.has("to"))        m.setTo(config.get("to").asInt());
                yield m;
            }

            case "Data Input" -> {
                DataInputModule m = new DataInputModule();
                applyName(m::setName, config);
                if (config.has("rootArrayName")) m.setRootArrayName(config.get("rootArrayName").asText());
                if (config.has("inputFilePath")) m.setInputFilePath(config.get("inputFilePath").asText());
                if (config.has("fileType")) {
                    try { m.setFileType(DataInputModule.FileType.valueOf(config.get("fileType").asText())); }
                    catch (IllegalArgumentException ignored) {}
                }
                if (config.has("textEncoding"))              m.setTextEncoding(config.get("textEncoding").asText());
                if (config.has("fieldSeparator"))            m.setFieldSeparator(config.get("fieldSeparator").asText());
                if (config.has("textQualifier"))             m.setTextQualifier(config.get("textQualifier").asText());
                if (config.has("skipFirstLines"))            m.setSkipFirstLines(config.get("skipFirstLines").asInt());
                if (config.has("useFirstRecordAsFieldNames")) m.setUseFirstRecordAsFieldNames(config.get("useFirstRecordAsFieldNames").asBoolean());
                yield m;
            }

            case "HTTP JSON Input" -> {
                HttpJsonDataInputModule m = new HttpJsonDataInputModule();
                applyName(m::setName, config);
                if (config.has("endpointUrl"))         m.setEndpointUrl(config.get("endpointUrl").asText());
                if (config.has("rootArrayName"))       m.setRootArrayName(config.get("rootArrayName").asText());
                if (config.has("jsonArrayPath"))       m.setJsonArrayPath(config.get("jsonArrayPath").asText());
                if (config.has("timeoutMilliseconds")) m.setTimeoutMilliseconds(config.get("timeoutMilliseconds").asInt());
                if (config.has("headers")) {
                    config.get("headers").fields().forEachRemaining(
                            entry -> m.addHeader(entry.getKey(), entry.getValue().asText()));
                }
                yield m;
            }

            case "Data Filter" -> {
                DataFilterModule m = new DataFilterModule();
                applyName(m::setName, config);
                if (config.has("invertCondition"))    m.setInvertCondition(config.get("invertCondition").asBoolean());
                if (config.has("allowMultipleValues")) m.setAllowMultipleValues(config.get("allowMultipleValues").asBoolean());
                if (config.has("createElseOutput")) m.setCreateElseOutput(config.get("createElseOutput").asBoolean());
                
                m.getCriteria().clear();
                if (config.has("criteria") && config.get("criteria").isArray()) {
                    for (JsonNode cn : config.get("criteria")) {
                        DataFilterModule.FilterCriterion c = new DataFilterModule.FilterCriterion();
                        if (cn.has("fieldName")) c.setFieldName(cn.get("fieldName").asText());
                        if (cn.has("condition")) {
                            try { c.setCondition(DataFilterModule.Condition.valueOf(cn.get("condition").asText())); }
                            catch (IllegalArgumentException ignored) {}
                        }
                        if (cn.has("filterValue")) c.setFilterValue(cn.get("filterValue").asText());
                        if (cn.has("ignoreCase")) c.setIgnoreCase(cn.get("ignoreCase").asBoolean());
                        m.getCriteria().add(c);
                    }
                } else {
                    // Legacy fallback
                    DataFilterModule.FilterCriterion c = new DataFilterModule.FilterCriterion();
                    if (config.has("fieldName")) c.setFieldName(config.get("fieldName").asText());
                    if (config.has("condition")) {
                        try { c.setCondition(DataFilterModule.Condition.valueOf(config.get("condition").asText())); }
                        catch (IllegalArgumentException ignored) {}
                    }
                    if (config.has("filterValue")) c.setFilterValue(config.get("filterValue").asText());
                    m.getCriteria().add(c);
                }
                yield m;
            }

            case "Data Concatenator" -> {
                DataConcatenatorModule m = new DataConcatenatorModule();
                applyName(m::setName, config);
                if (config.has("numberOfInputs")) {
                    m.setNumberOfInputs(config.get("numberOfInputs").asInt());
                }
                yield m;
            }

            case "Data Sorter" -> {
                DataSorterModule m = new DataSorterModule();
                applyName(m::setName, config);
                m.getCriteria().clear();
                
                if (config.has("criteria") && config.get("criteria").isArray()) {
                    for (JsonNode cn : config.get("criteria")) {
                        DataSorterModule.Direction dir = DataSorterModule.Direction.ASCENDING;
                        if (cn.has("direction")) {
                            try { dir = DataSorterModule.Direction.valueOf(cn.get("direction").asText()); } catch (Exception ignored) {}
                        }
                        DataSorterModule.ComparisonType compType = DataSorterModule.ComparisonType.AUTO;
                        if (cn.has("comparisonType")) {
                            try { compType = DataSorterModule.ComparisonType.valueOf(cn.get("comparisonType").asText()); } catch (Exception ignored) {}
                        }
                        DataSorterModule.NullOrder nullOrder = DataSorterModule.NullOrder.LAST;
                        if (cn.has("nullOrder")) {
                            try { nullOrder = DataSorterModule.NullOrder.valueOf(cn.get("nullOrder").asText()); } catch (Exception ignored) {}
                        }
                        
                        m.getCriteria().add(new DataSorterModule.SortCriterion(
                            cn.has("fieldName") ? cn.get("fieldName").asText() : "",
                            dir,
                            compType,
                            cn.has("ignoreCase") ? cn.get("ignoreCase").asBoolean() : true,
                            nullOrder
                        ));
                    }
                } else {
                    DataSorterModule.Direction dir = DataSorterModule.Direction.ASCENDING;
                    if (config.has("direction")) {
                        try { dir = DataSorterModule.Direction.valueOf(config.get("direction").asText()); }
                        catch (IllegalArgumentException ignored) {}
                    }
                    m.getCriteria().add(new DataSorterModule.SortCriterion(
                        config.has("fieldName") ? config.get("fieldName").asText() : "",
                        dir,
                        DataSorterModule.ComparisonType.AUTO,
                        true,
                        DataSorterModule.NullOrder.LAST
                    ));
                }
                yield m;
            }

            default -> null; // unknown type — skip silently
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void applyName(java.util.function.Consumer<String> setter, JsonNode config) {
        if (config.has("name")) {
            String n = config.get("name").asText();
            if (!n.isBlank()) {
                try { setter.accept(n); } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private static Port portById(java.util.List<Port> ports, String id) {
        return ports.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst().orElse(null);
    }

    private static Port portByName(java.util.List<Port> ports, String name) {
        return ports.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst().orElse(null);
    }
}
