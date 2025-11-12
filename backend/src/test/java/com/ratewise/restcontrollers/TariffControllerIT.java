package com.ratewise.restcontrollers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TariffControllerIT {
    
    @Autowired
    private TariffController controller;

    @Test
    void testLookup() {
        Map<String, Object> result = controller.lookup("SG", "US", "010121", "MFN");
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertEquals("SG", result.get("exporter_code"));
        assertEquals("US", result.get("importer_code"));
    }
    
    @Test
    void testListTariffsNoFilters() {
        List<Map<String, Object>> results = controller.listTariffs(null, null, null);
        
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }
    
    @Test
    void testListTariffsWithImporter() {
        List<Map<String, Object>> results = controller.listTariffs("US", null, null);
        
        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // Verify all have US as importer
        for (Map<String, Object> tariff : results) {
            assertEquals("US", tariff.get("importer_code"));
        }
    }
    
    @Test
    void testListTariffsTable() {
        List<Map<String, Object>> results = controller.listTariffsTable();
        
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }
}
