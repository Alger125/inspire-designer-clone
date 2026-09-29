package com.vdp.core.model;

import com.vdp.core.model.DataSorterModule.ComparisonType;
import com.vdp.core.model.DataSorterModule.Direction;
import com.vdp.core.model.DataSorterModule.NullOrder;
import com.vdp.core.model.DataSorterModule.SortCriterion;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DataSorterTest {

    private ExecutionContext createTestContext(Object[][] data, String[] fields) {
        DataNode root = DataNode.arrayNode("root");
        for (Object[] row : data) {
            DataNode record = DataNode.objectNode("record");
            for (int i = 0; i < fields.length; i++) {
                record.addChild(DataNode.valueNode(fields[i], row[i] == null ? null : row[i].toString()));
            }
            root.addChild(record);
        }
        ExecutionContext context = new ExecutionContext();
        context.setData("DataInput", root);
        return context;
    }

    // ── Number ascending and descending ───────────────────────────────────────

    @Test
    void testNumberAscendingAndDescending() {
        Object[][] data = { {5}, {1}, {10}, {2} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));

        ExecutionContext ctxAsc = createTestContext(data, fields);
        sorter.execute(ctxAsc);
        List<DataNode> resAsc = ctxAsc.getData("DataOutput").getChildren();
        assertEquals("1",  resAsc.get(0).getChild("val").getValue());
        assertEquals("2",  resAsc.get(1).getChild("val").getValue());
        assertEquals("5",  resAsc.get(2).getChild("val").getValue());
        assertEquals("10", resAsc.get(3).getChild("val").getValue());

        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.DESCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));

        ExecutionContext ctxDesc = createTestContext(data, fields);
        sorter.execute(ctxDesc);
        List<DataNode> resDesc = ctxDesc.getData("DataOutput").getChildren();
        assertEquals("10", resDesc.get(0).getChild("val").getValue());
        assertEquals("5",  resDesc.get(1).getChild("val").getValue());
        assertEquals("2",  resDesc.get(2).getChild("val").getValue());
        assertEquals("1",  resDesc.get(3).getChild("val").getValue());
    }

    // ── Text ascending AND descending ─────────────────────────────────────────

    @Test
    void testTextAscendingAndDescending() {
        Object[][] data = { {"Banana"}, {"apple"}, {"Cherry"} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();

        // ASC + ignoreCase
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, true, NullOrder.LAST));
        ExecutionContext ctxAsc = createTestContext(data, fields);
        sorter.execute(ctxAsc);
        List<DataNode> resAsc = ctxAsc.getData("DataOutput").getChildren();
        assertEquals("apple",  resAsc.get(0).getChild("val").getValue());
        assertEquals("Banana", resAsc.get(1).getChild("val").getValue());
        assertEquals("Cherry", resAsc.get(2).getChild("val").getValue());

        // DESC + ignoreCase
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.DESCENDING, ComparisonType.TEXT, true, NullOrder.LAST));
        ExecutionContext ctxDesc = createTestContext(data, fields);
        sorter.execute(ctxDesc);
        List<DataNode> resDesc = ctxDesc.getData("DataOutput").getChildren();
        assertEquals("Cherry", resDesc.get(0).getChild("val").getValue());
        assertEquals("Banana", resDesc.get(1).getChild("val").getValue());
        assertEquals("apple",  resDesc.get(2).getChild("val").getValue());

        // ASC + case-sensitive
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, false, NullOrder.LAST));
        ExecutionContext ctxCase = createTestContext(data, fields);
        sorter.execute(ctxCase);
        List<DataNode> resCase = ctxCase.getData("DataOutput").getChildren();
        // Upper before lower in ASCII: B, C, a
        assertEquals("Banana", resCase.get(0).getChild("val").getValue());
        assertEquals("Cherry", resCase.get(1).getChild("val").getValue());
        assertEquals("apple",  resCase.get(2).getChild("val").getValue());
    }

    // ── Multiple criteria with stability ──────────────────────────────────────

    @Test
    void testMultipleCriteriaAndStability() {
        // Two records are fully tied on both sort keys to verify stable order
        Object[][] data = {
            {"Mexico",    25, "A"},
            {"Argentina", 20, "B"},
            {"Mexico",    18, "C"},
            {"Argentina", 20, "D"},  // tied with B on Country+Age
            {"Argentina", 17, "E"}
        };
        String[] fields = {"Country", "Age", "Id"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("Country", Direction.ASCENDING, ComparisonType.TEXT, false, NullOrder.LAST));
        sorter.getCriteria().add(new SortCriterion("Age", Direction.DESCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));

        ExecutionContext context = createTestContext(data, fields);
        sorter.execute(context);
        List<DataNode> res = context.getData("DataOutput").getChildren();

        // Argentina 20 (B), Argentina 20 (D), Argentina 17 (E), Mexico 25 (A), Mexico 18 (C)
        assertEquals("Argentina", res.get(0).getChild("Country").getValue());
        assertEquals("20",        res.get(0).getChild("Age").getValue());
        assertEquals("B",         res.get(0).getChild("Id").getValue()); // B before D (stable)

        assertEquals("Argentina", res.get(1).getChild("Country").getValue());
        assertEquals("20",        res.get(1).getChild("Age").getValue());
        assertEquals("D",         res.get(1).getChild("Id").getValue()); // D after B (stable)

        assertEquals("Argentina", res.get(2).getChild("Country").getValue());
        assertEquals("17",        res.get(2).getChild("Age").getValue());

        assertEquals("Mexico",    res.get(3).getChild("Country").getValue());
        assertEquals("25",        res.get(3).getChild("Age").getValue());

        assertEquals("Mexico",    res.get(4).getChild("Country").getValue());
        assertEquals("18",        res.get(4).getChild("Age").getValue());
    }

    // ── NullOrder FIRST and LAST with both ASC and DESC ───────────────────────

    @Test
    void testNullOrderLastAscending() {
        Object[][] data = { {"B"}, {null}, {"A"}, {""}, {"  "} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, true, NullOrder.LAST));

        ExecutionContext ctx = createTestContext(data, fields);
        sorter.execute(ctx);
        List<DataNode> res = ctx.getData("DataOutput").getChildren();
        // Non-blanks sorted first, then all blanks at the end
        assertEquals("A", res.get(0).getChild("val").getValue());
        assertEquals("B", res.get(1).getChild("val").getValue());
        // Remaining 3 are blanks (null, "", "  ") — all at end
        assertEquals(5, res.size());
    }

    @Test
    void testNullOrderFirstAscending() {
        Object[][] data = { {"B"}, {null}, {"A"}, {""} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, true, NullOrder.FIRST));

        ExecutionContext ctx = createTestContext(data, fields);
        sorter.execute(ctx);
        List<DataNode> res = ctx.getData("DataOutput").getChildren();
        // Blanks first, then non-blanks sorted ascending
        assertEquals("A", res.get(2).getChild("val").getValue());
        assertEquals("B", res.get(3).getChild("val").getValue());
    }

    @Test
    void testNullOrderLastDescending() {
        // LAST + DESC: blanks still at the end, non-blanks sorted descending
        Object[][] data = { {"A"}, {null}, {"C"}, {"B"} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.DESCENDING, ComparisonType.TEXT, true, NullOrder.LAST));

        ExecutionContext ctx = createTestContext(data, fields);
        sorter.execute(ctx);
        List<DataNode> res = ctx.getData("DataOutput").getChildren();
        assertEquals("C", res.get(0).getChild("val").getValue());
        assertEquals("B", res.get(1).getChild("val").getValue());
        assertEquals("A", res.get(2).getChild("val").getValue());
        assertNull(res.get(3).getChild("val").getValue()); // null at end
    }

    @Test
    void testNullOrderFirstDescending() {
        // FIRST + DESC: blanks at the beginning, non-blanks sorted descending
        Object[][] data = { {"A"}, {null}, {"C"}, {"B"} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.DESCENDING, ComparisonType.TEXT, true, NullOrder.FIRST));

        ExecutionContext ctx = createTestContext(data, fields);
        sorter.execute(ctx);
        List<DataNode> res = ctx.getData("DataOutput").getChildren();
        assertNull(res.get(0).getChild("val").getValue()); // null at start
        assertEquals("C", res.get(1).getChild("val").getValue());
        assertEquals("B", res.get(2).getChild("val").getValue());
        assertEquals("A", res.get(3).getChild("val").getValue());
    }

    // ── AUTO resolves all-numeric to NUMBER ───────────────────────────────────

    @Test
    void testAutoAllNumeric() {
        // All values are numeric → AUTO resolves to NUMBER (not lexicographic)
        Object[][] data = { {2}, {10}, {1}, {3} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.AUTO, false, NullOrder.LAST));

        ExecutionContext ctx = createTestContext(data, fields);
        sorter.execute(ctx);
        List<DataNode> res = ctx.getData("DataOutput").getChildren();
        // Numeric order, not lexicographic (where "10" < "2")
        assertEquals("1",  res.get(0).getChild("val").getValue());
        assertEquals("2",  res.get(1).getChild("val").getValue());
        assertEquals("3",  res.get(2).getChild("val").getValue());
        assertEquals("10", res.get(3).getChild("val").getValue());
    }

    // ── AUTO resolves mixed data to TEXT ───────────────────────────────────────

    @Test
    void testAutoMixedData() {
        // At least one non-numeric → AUTO resolves to TEXT for the whole column
        Object[][] data = { {"2"}, {"10"}, {"ABC"}, {"3"} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.AUTO, true, NullOrder.LAST));

        ExecutionContext ctx = createTestContext(data, fields);
        sorter.execute(ctx);
        List<DataNode> res = ctx.getData("DataOutput").getChildren();
        // Lexicographic TEXT order (case-insensitive): "10", "2", "3", "ABC"
        assertEquals("10",  res.get(0).getChild("val").getValue());
        assertEquals("2",   res.get(1).getChild("val").getValue());
        assertEquals("3",   res.get(2).getChild("val").getValue());
        assertEquals("ABC", res.get(3).getChild("val").getValue());
    }

    // ── NUMBER with invalid value throws ──────────────────────────────────────

    @Test
    void testNumberWithInvalidValueThrows() {
        Object[][] data = { {"5"}, {"ABC"}, {"3"} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));

        ExecutionContext ctx = createTestContext(data, fields);
        Exception e = assertThrows(IllegalStateException.class, () -> sorter.execute(ctx));
        assertTrue(e.getMessage().contains("val"));
        assertTrue(e.getMessage().contains("ABC"));
    }

    // ── Input immutability ────────────────────────────────────────────────────

    @Test
    void testInputNotModified() {
        Object[][] data = { {3}, {1}, {2} };
        String[] fields = {"val"};

        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));

        ExecutionContext ctx = createTestContext(data, fields);
        DataNode inputBefore = ctx.getData("DataInput");
        // Record the original order
        String first  = inputBefore.getChildren().get(0).getChild("val").getValue();
        String second = inputBefore.getChildren().get(1).getChild("val").getValue();
        String third  = inputBefore.getChildren().get(2).getChild("val").getValue();

        sorter.execute(ctx);

        // Original DataInput must not be modified
        DataNode inputAfter = ctx.getData("DataInput");
        assertEquals(first,  inputAfter.getChildren().get(0).getChild("val").getValue());
        assertEquals(second, inputAfter.getChildren().get(1).getChild("val").getValue());
        assertEquals(third,  inputAfter.getChildren().get(2).getChild("val").getValue());

        // But DataOutput is sorted
        List<DataNode> output = ctx.getData("DataOutput").getChildren();
        assertEquals("1", output.get(0).getChild("val").getValue());
        assertEquals("2", output.get(1).getChild("val").getValue());
        assertEquals("3", output.get(2).getChild("val").getValue());
    }

    // ── Serialization: new format round-trip ──────────────────────────────────

    @Test
    void testSerializationNewFormat() throws java.io.IOException {
        Workflow wf = new Workflow("TestWF");
        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("f1", Direction.DESCENDING, ComparisonType.NUMBER, true, NullOrder.FIRST));
        sorter.getCriteria().add(new SortCriterion("f2", Direction.ASCENDING, ComparisonType.TEXT, false, NullOrder.LAST));
        wf.addModule(sorter);

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("wf", ".json");
        WorkflowSerializer.save(wf, java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), tempFile);

        WorkflowSerializer.LoadResult loadedRes = WorkflowSerializer.load(tempFile);
        DataSorterModule loaded = (DataSorterModule) loadedRes.workflow().getModules().get(0);

        assertEquals(2, loaded.getCriteria().size());

        assertEquals("f1", loaded.getCriteria().get(0).getFieldName());
        assertEquals(Direction.DESCENDING, loaded.getCriteria().get(0).getDirection());
        assertEquals(ComparisonType.NUMBER, loaded.getCriteria().get(0).getComparisonType());
        assertTrue(loaded.getCriteria().get(0).isIgnoreCase());
        assertEquals(NullOrder.FIRST, loaded.getCriteria().get(0).getNullOrder());

        assertEquals("f2", loaded.getCriteria().get(1).getFieldName());
        assertEquals(Direction.ASCENDING, loaded.getCriteria().get(1).getDirection());
        assertEquals(ComparisonType.TEXT, loaded.getCriteria().get(1).getComparisonType());
        assertFalse(loaded.getCriteria().get(1).isIgnoreCase());
        assertEquals(NullOrder.LAST, loaded.getCriteria().get(1).getNullOrder());

        java.nio.file.Files.delete(tempFile);
    }

    // ── Serialization: legacy format with serialId / config wrapper ───────────

    @Test
    void testSerializationLegacyFormat() throws java.io.IOException {
        String legacyJson = "{\n" +
            "  \"name\" : \"Legacy\",\n" +
            "  \"modules\" : [ {\n" +
            "    \"serialId\" : \"m1\",\n" +
            "    \"type\" : \"Data Sorter\",\n" +
            "    \"x\" : 100,\n" +
            "    \"y\" : 100,\n" +
            "    \"config\" : {\n" +
            "      \"name\" : \"Sorter\",\n" +
            "      \"fieldName\" : \"LegacyField\",\n" +
            "      \"direction\" : \"DESCENDING\"\n" +
            "    }\n" +
            "  } ],\n" +
            "  \"connections\" : [ ]\n" +
            "}";

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("legacy", ".json");
        java.nio.file.Files.writeString(tempFile, legacyJson);

        WorkflowSerializer.LoadResult loadedRes = WorkflowSerializer.load(tempFile);
        DataSorterModule loaded = (DataSorterModule) loadedRes.workflow().getModules().get(0);

        assertEquals(1, loaded.getCriteria().size());
        assertEquals("LegacyField", loaded.getCriteria().get(0).getFieldName());
        assertEquals(Direction.DESCENDING, loaded.getCriteria().get(0).getDirection());
        assertEquals(ComparisonType.AUTO, loaded.getCriteria().get(0).getComparisonType());
        assertEquals(NullOrder.LAST, loaded.getCriteria().get(0).getNullOrder());
        assertTrue(loaded.getCriteria().get(0).isIgnoreCase());

        java.nio.file.Files.delete(tempFile);
    }
}
