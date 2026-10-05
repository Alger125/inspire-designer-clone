package com.vdp.core.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vdp.core.controller.WorkflowController;
import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataTransformerTest {

    private static DataNode record(String... kv) {
        DataNode r = DataNode.objectNode("Record");
        for (int i = 0; i < kv.length; i += 2) r.addChild(DataNode.valueNode(kv[i], kv[i + 1]));
        return r;
    }

    private static DataNode run(DataTransformerModule m, DataNode... records) {
        DataNode root = DataNode.arrayNode("Records");
        for (DataNode r : records) root.addChild(r);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setData("DataInput", root);
        m.execute(ctx);
        return ctx.getData("DataOutput");
    }

    @Test
    void mapsFieldsLiteralsAndFunctions() {
        DataTransformerModule m = new DataTransformerModule();
        m.getMappings().add(new DataTransformerModule.FieldMapping("Full", "{First} {Last}"));
        m.getMappings().add(new DataTransformerModule.FieldMapping("Shout", "UPPER({First})"));
        m.getMappings().add(new DataTransformerModule.FieldMapping("Len", "LENGTH({Last})"));
        m.getMappings().add(new DataTransformerModule.FieldMapping("Const", "X"));

        DataNode out = run(m, record("First", "ana", "Last", "Lopez"));
        DataNode r = out.getChildren().get(0);
        assertEquals("ana Lopez", r.getChild("Full").getValue());
        assertEquals("ANA", r.getChild("Shout").getValue());
        assertEquals("5", r.getChild("Len").getValue());
        assertEquals("X", r.getChild("Const").getValue());
        assertNull(r.getChild("First"), "unmapped fields are dropped by default");
    }

    @Test
    void keepsUnmappedFieldsWhenRequested() {
        DataTransformerModule m = new DataTransformerModule();
        m.setKeepUnmappedFields(true);
        m.getMappings().add(new DataTransformerModule.FieldMapping("Name", "LOWER({Name})"));
        DataNode r = run(m, record("Name", "BOB", "Age", "3")).getChildren().get(0);
        assertEquals("bob", r.getChild("Name").getValue());
        assertEquals("3", r.getChild("Age").getValue());
    }

    @Test
    void missingFieldBecomesEmptyAndValidationCatchesProblems() {
        DataTransformerModule m = new DataTransformerModule();
        assertFalse(m.validate().isEmpty());
        m.getMappings().add(new DataTransformerModule.FieldMapping("A", "{Nope}"));
        m.getMappings().add(new DataTransformerModule.FieldMapping("A", "x"));
        assertTrue(m.validate().stream().anyMatch(s -> s.contains("Duplicate")));
        m.getMappings().remove(1);
        assertEquals("", run(m, record("B", "1")).getChildren().get(0).getChild("A").getValue());
    }

    @Test
    void worksInsideWorkflowAndSurvivesSerialization() throws Exception {
        Workflow wf = new Workflow("T");
        DataGeneratorModule gen = new DataGeneratorModule();
        gen.setTo(3);
        DataTransformerModule tr = new DataTransformerModule();
        tr.getMappings().add(new DataTransformerModule.FieldMapping("Id", "N{Value}"));
        wf.addModule(gen);
        wf.addModule(tr);
        wf.connect(gen, gen.getOutputPorts().get(0), tr, tr.getInputPorts().get(0));

        ProofRunResult result = new WorkflowController().runProof(wf, tr.getId());
        assertTrue(result.getValidationMessages().isEmpty());
        assertEquals(3, result.getSnapshots().get(tr.getId()).getRecordCount());

        Path file = Files.createTempFile("wf", ".json");
        WorkflowSerializer.save(wf,
                Map.of(gen.getId(), "Data Generator", tr.getId(), "Data Transformer"),
                Map.of(gen.getId(), new Point(1, 1), tr.getId(), new Point(2, 2)), file);
        Workflow loaded = WorkflowSerializer.load(file).workflow();
        Files.deleteIfExists(file);
        DataTransformerModule back = loaded.getModules().stream()
                .filter(DataTransformerModule.class::isInstance)
                .map(DataTransformerModule.class::cast).findFirst().orElseThrow();
        assertEquals("N{Value}", back.getMappings().get(0).getExpression());
        assertEquals(1, loaded.getConnections().size());
    }
}
