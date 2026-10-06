package com.vdp.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DataPathResolverTest {

    @Test
    void testResolveSingleLevel() {
        DataNode record = DataNode.objectNode("Record");
        record.addChild(DataNode.valueNode("Name", "Erick"));
        
        DataNode resolved = DataPathResolver.resolve(record, "Name");
        assertNotNull(resolved);
        assertEquals("Erick", resolved.getValue());
    }

    @Test
    void testResolveNestedPath() {
        DataNode customer = DataNode.objectNode("Customer");
        DataNode address = DataNode.objectNode("Address");
        address.addChild(DataNode.valueNode("City", "CDMX"));
        customer.addChild(address);
        
        DataNode record = DataNode.objectNode("Record");
        record.addChild(customer);

        DataNode resolved = DataPathResolver.resolve(record, "Customer.Address.City");
        assertNotNull(resolved);
        assertEquals("CDMX", resolved.getValue());
        assertEquals("CDMX", DataPathResolver.resolveValue(record, "Customer.Address.City"));
    }

    @Test
    void testResolveNonExistentPath() {
        DataNode record = DataNode.objectNode("Record");
        record.addChild(DataNode.valueNode("Name", "Erick"));
        
        assertNull(DataPathResolver.resolve(record, "Age"));
        assertEquals("", DataPathResolver.resolveValue(record, "Age"));
        
        assertNull(DataPathResolver.resolve(record, "Customer.Address.City"));
        assertEquals("", DataPathResolver.resolveValue(record, "Customer.Address.City"));
    }
}
