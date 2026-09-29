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
}
