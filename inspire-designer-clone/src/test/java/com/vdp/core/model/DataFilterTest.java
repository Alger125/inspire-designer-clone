package com.vdp.core.model;

import com.vdp.core.controller.WorkflowController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;
import java.awt.Point;

class DataFilterTest {

    @Test
    void testMultipleCriteriaAND() {
        DataFilterModule filter = new DataFilterModule();
        filter.getCriteria().clear();
        filter.getCriteria().add(new DataFilterModule.FilterCriterion("Name", DataFilterModule.Condition.CONTAINS, "a", true));
        filter.getCriteria().add(new DataFilterModule.FilterCriterion("Age", DataFilterModule.Condition.BIGGER_THAN, "10", false));
        
        DataNode root = DataNode.arrayNode("root");
        
        DataNode p1 = DataNode.objectNode("p1");
        p1.addChild(DataNode.valueNode("Name", "Adam"));
        p1.addChild(DataNode.valueNode("Age", "12"));
        root.addChild(p1);

        DataNode p2 = DataNode.objectNode("p2");
        p2.addChild(DataNode.valueNode("Name", "Bob"));
        p2.addChild(DataNode.valueNode("Age", "15"));
        root.addChild(p2);
        
        DataNode p3 = DataNode.objectNode("p3");
        p3.addChild(DataNode.valueNode("Name", "Amanda"));
        p3.addChild(DataNode.valueNode("Age", "9"));
        root.addChild(p3);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setData("DataInput", root);
        
        filter.execute(ctx);
        
        DataNode matched = ctx.getData("Matched");
        DataNode elseRoot = ctx.getData("Else");
        
        assertEquals(1, matched.getChildren().size());
        assertEquals("Adam", matched.getChildren().get(0).getChild("Name").getValue());
        
        assertEquals(2, elseRoot.getChildren().size());
    }

    @Test
    void testInvertCondition() {
        DataFilterModule filter = new DataFilterModule();
        filter.getCriteria().get(0).setFilterValue("3");
        filter.getCriteria().get(0).setCondition(DataFilterModule.Condition.EQUAL_TO);
        filter.setInvertCondition(true);
        
        DataNode root = DataNode.arrayNode("root");
        for (int i=1; i<=4; i++) {
            DataNode p = DataNode.objectNode("p");
            p.addChild(DataNode.valueNode("Value", String.valueOf(i)));
            root.addChild(p);
        }
        
        ExecutionContext ctx = new ExecutionContext();
        ctx.setData("DataInput", root);
        
        filter.execute(ctx);
        assertEquals(3, ctx.getData("Matched").getChildren().size());
        assertEquals(1, ctx.getData("Else").getChildren().size());
    }

    @Test
    void testAllowMultipleValues() {
        DataFilterModule filter = new DataFilterModule();
        filter.getCriteria().get(0).setFilterValue("2, 4");
        filter.getCriteria().get(0).setCondition(DataFilterModule.Condition.EQUAL_TO);
        filter.setAllowMultipleValues(true);
        
        DataNode root = DataNode.arrayNode("root");
        for (int i=1; i<=5; i++) {
            DataNode p = DataNode.objectNode("p");
            p.addChild(DataNode.valueNode("Value", String.valueOf(i)));
            root.addChild(p);
        }
        
        ExecutionContext ctx = new ExecutionContext();
        ctx.setData("DataInput", root);
        
        filter.execute(ctx);
        assertEquals(2, ctx.getData("Matched").getChildren().size());
        assertEquals(3, ctx.getData("Else").getChildren().size());
    }

    @Test
    void testSerializationOfCriteria() throws IOException {
        Workflow workflow = new Workflow("SerTest");
        DataFilterModule filter = new DataFilterModule();
        filter.getCriteria().clear();
        filter.getCriteria().add(new DataFilterModule.FilterCriterion("F1", DataFilterModule.Condition.EQUAL_TO, "V1", true));
        filter.getCriteria().add(new DataFilterModule.FilterCriterion("F2", DataFilterModule.Condition.CONTAINS, "V2", false));
        filter.setCreateElseOutput(false);
        filter.setInvertCondition(true);
        workflow.addModule(filter);

        Path tempFile = Files.createTempFile("inspire", ".json");
        Map<String, String> types = new HashMap<>();
        types.put(filter.getId(), "Data Filter");
        Map<String, Point> pts = new HashMap<>();

        WorkflowSerializer.save(workflow, types, pts, tempFile);

        WorkflowSerializer.LoadResult result = WorkflowSerializer.load(tempFile);
        DataFilterModule loaded = (DataFilterModule) result.workflow().getModules().get(0);

        assertEquals(2, loaded.getCriteria().size());
        assertEquals("F1", loaded.getCriteria().get(0).getFieldName());
        assertEquals("V2", loaded.getCriteria().get(1).getFilterValue());
        assertFalse(loaded.getCriteria().get(1).isIgnoreCase());
        assertFalse(loaded.isCreateElseOutput());
        assertTrue(loaded.isInvertCondition());
    }
    
    @Test
    void testLegacyConnectionLoading() throws IOException {
        String legacyJson = """
        {
          "version": 1,
          "name": "Legacy",
          "modules": [
            { "serialId": "m1", "type": "Data Filter", "x": 100, "y": 100, "config": {} },
            { "serialId": "m2", "type": "Data Filter", "x": 300, "y": 100, "config": {} }
          ],
          "connections": [
            {
              "sourceModuleId": "m1",
              "sourcePortName": "DataOutput",
              "targetModuleId": "m2",
              "targetPortName": "DataInput"
            }
          ]
        }
        """;
        
        Path tempFile = Files.createTempFile("legacy", ".json");
        Files.writeString(tempFile, legacyJson);
        
        WorkflowSerializer.LoadResult result = WorkflowSerializer.load(tempFile);
        Workflow wf = result.workflow();
        
        assertEquals(1, wf.getConnections().size(), "Connection should have been recovered using fallbacks");
        WorkflowConnection conn = wf.getConnections().get(0);
        
        InspireModule source = wf.findModule(conn.sourceModuleId());
        assertNotNull(source);
        
        assertTrue(source.getOutputPorts().stream().anyMatch(port -> port.getId().equals(conn.sourcePortId())));
        assertEquals("Matched", conn.sourcePortId());
    }

    @Test
    void testSerializationOfMatchedAndElseConnections() throws IOException {
        Workflow workflow = new Workflow("SerTest2");
        DataFilterModule filter = new DataFilterModule();
        DataSorterModule s1 = new DataSorterModule();
        DataSorterModule s2 = new DataSorterModule();
        
        workflow.addModule(filter);
        workflow.addModule(s1);
        workflow.addModule(s2);
        
        Port matchedPort = filter.getOutputPorts().stream().filter(p -> p.getId().equals("Matched")).findFirst().get();
        Port elsePort = filter.getOutputPorts().stream().filter(p -> p.getId().equals("Else")).findFirst().get();
        
        workflow.connect(filter, matchedPort, s1, s1.getInputPorts().get(0));
        workflow.connect(filter, elsePort, s2, s2.getInputPorts().get(0));
        
        Path tempFile = Files.createTempFile("inspire2", ".json");
        Map<String, String> types = new HashMap<>();
        types.put(filter.getId(), "Data Filter");
        types.put(s1.getId(), "Data Sorter");
        types.put(s2.getId(), "Data Sorter");
        
        WorkflowSerializer.save(workflow, types, new HashMap<>(), tempFile);
        
        WorkflowSerializer.LoadResult result = WorkflowSerializer.load(tempFile);
        Workflow wf = result.workflow();
        
        assertEquals(2, wf.getConnections().size());
        boolean hasMatched = wf.getConnections().stream().anyMatch(c -> c.sourcePortId().equals("Matched"));
        boolean hasElse = wf.getConnections().stream().anyMatch(c -> c.sourcePortId().equals("Else"));
        
        assertTrue(hasMatched);
        assertTrue(hasElse);
    }
}
