package com.vdp.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Test
    void testNumberAscendingAndDescending() {
        Object[][] data = {
            {5}, {1}, {10}, {2}
        };
        String[] fields = {"val"};
        
        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));
        
        ExecutionContext contextAsc = createTestContext(data, fields);
        sorter.execute(contextAsc);
        List<DataNode> resAsc = contextAsc.getData("DataOutput").getChildren();
        assertEquals("1", resAsc.get(0).getChild("val").getValue());
        assertEquals("2", resAsc.get(1).getChild("val").getValue());
        assertEquals("5", resAsc.get(2).getChild("val").getValue());
        assertEquals("10", resAsc.get(3).getChild("val").getValue());
        
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.DESCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));
        
        ExecutionContext contextDesc = createTestContext(data, fields);
        sorter.execute(contextDesc);
        List<DataNode> resDesc = contextDesc.getData("DataOutput").getChildren();
        assertEquals("10", resDesc.get(0).getChild("val").getValue());
        assertEquals("5", resDesc.get(1).getChild("val").getValue());
        assertEquals("2", resDesc.get(2).getChild("val").getValue());
        assertEquals("1", resDesc.get(3).getChild("val").getValue());
    }

    @Test
    void testTextAscendingAndDescending() {
        Object[][] data = {
            {"Banana"}, {"apple"}, {"Cherry"}
        };
        String[] fields = {"val"};
        
        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        // Ignore case true
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, true, NullOrder.LAST));
        
        ExecutionContext contextAsc = createTestContext(data, fields);
        sorter.execute(contextAsc);
        List<DataNode> resAsc = contextAsc.getData("DataOutput").getChildren();
        assertEquals("apple", resAsc.get(0).getChild("val").getValue());
        assertEquals("Banana", resAsc.get(1).getChild("val").getValue());
        assertEquals("Cherry", resAsc.get(2).getChild("val").getValue());
        
        // Ignore case false
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, false, NullOrder.LAST));
        ExecutionContext contextAscCase = createTestContext(data, fields);
        sorter.execute(contextAscCase);
        List<DataNode> resAscCase = contextAscCase.getData("DataOutput").getChildren();
        // B, C, a (Upper case before lower case in ASCII)
        assertEquals("Banana", resAscCase.get(0).getChild("val").getValue());
        assertEquals("Cherry", resAscCase.get(1).getChild("val").getValue());
        assertEquals("apple", resAscCase.get(2).getChild("val").getValue());
    }

    @Test
    void testMultipleCriteriaAndStability() {
        Object[][] data = {
            {"Mexico", 25, "A"},
            {"Argentina", 20, "B"},
            {"Mexico", 18, "C"},
            {"Argentina", 17, "D"}
        };
        String[] fields = {"Country", "Age", "Id"};
        
        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("Country", Direction.ASCENDING, ComparisonType.TEXT, false, NullOrder.LAST));
        sorter.getCriteria().add(new SortCriterion("Age", Direction.DESCENDING, ComparisonType.NUMBER, false, NullOrder.LAST));
        
        ExecutionContext context = createTestContext(data, fields);
        sorter.execute(context);
        List<DataNode> res = context.getData("DataOutput").getChildren();
        
        assertEquals("Argentina", res.get(0).getChild("Country").getValue());
        assertEquals("20", res.get(0).getChild("Age").getValue());
        
        assertEquals("Argentina", res.get(1).getChild("Country").getValue());
        assertEquals("17", res.get(1).getChild("Age").getValue());
        
        assertEquals("Mexico", res.get(2).getChild("Country").getValue());
        assertEquals("25", res.get(2).getChild("Age").getValue());
        
        assertEquals("Mexico", res.get(3).getChild("Country").getValue());
        assertEquals("18", res.get(3).getChild("Age").getValue());
    }

    @Test
    void testNullOrder() {
        Object[][] data = {
            {"B"}, {null}, {"A"}, {""}
        };
        String[] fields = {"val"};
        
        DataSorterModule sorter = new DataSorterModule();
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, true, NullOrder.LAST));
        
        ExecutionContext contextLast = createTestContext(data, fields);
        sorter.execute(contextLast);
        List<DataNode> resLast = contextLast.getData("DataOutput").getChildren();
        assertEquals("A", resLast.get(0).getChild("val").getValue());
        assertEquals("B", resLast.get(1).getChild("val").getValue());
        // null vs empty logic: our impl considers empty string and null equivalently empty.
        // Thus they are stable in their relative order (null was at index 1, "" at index 3 originally)
        assertNull(resLast.get(2).getChild("val").getValue());
        assertEquals("", resLast.get(3).getChild("val").getValue());
        
        sorter.getCriteria().clear();
        sorter.getCriteria().add(new SortCriterion("val", Direction.ASCENDING, ComparisonType.TEXT, true, NullOrder.FIRST));
        
        ExecutionContext contextFirst = createTestContext(data, fields);
        sorter.execute(contextFirst);
        List<DataNode> resFirst = contextFirst.getData("DataOutput").getChildren();
        assertNull(resFirst.get(0).getChild("val").getValue());
        assertEquals("", resFirst.get(1).getChild("val").getValue());
        assertEquals("A", resFirst.get(2).getChild("val").getValue());
        assertEquals("B", resFirst.get(3).getChild("val").getValue());
    }

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

    @Test
    void testSerializationLegacyFormat() throws java.io.IOException {
        String legacyJson = "{\n" +
            "  \"name\" : \"Legacy\",\n" +
            "  \"modules\" : [ {\n" +
            "    \"type\" : \"Data Sorter\",\n" +
            "    \"id\" : \"m1\",\n" +
            "    \"name\" : \"Sorter\",\n" +
            "    \"fieldName\" : \"LegacyField\",\n" +
            "    \"direction\" : \"DESCENDING\"\n" +
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
