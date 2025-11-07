package com.ratewise.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;


// test the queries and effective date
@SpringBootTest
class CalculatorServiceIT {
    
    @Autowired
    private CalculatorService calculatorService;
    
    @Test
    void calculateLandedCost_ShouldReturnCorrectRate_WithRealData() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "Singapore");
        request.put("importer", "United States");
        request.put("hsCode", "010121");
        request.put("agreement", "MFN");
        request.put("goods_value", 1000.0);
        request.put("freight", 50.0);
        request.put("insurance", 100.0);
        request.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result = calculatorService.calculateLandedCost(request);
        
        assertTrue((Boolean) result.get("ok"));
        assertEquals(17.76, ((Number) result.get("rate_percent")).doubleValue(), 0.01);
        assertEquals("FOB", result.get("customs_basis"));
        assertEquals(1000.0, ((Number) result.get("customs_value")).doubleValue(), 0.01);
        assertNotNull(result.get("total_landed_cost"));
    }

    // test failing
    @Test
    void calculateLandedCost_ShouldReturnCorrectRate_WithDateInFuture() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "Singapore");
        request.put("importer", "United States");
        request.put("hsCode", "010121");
        request.put("agreement", "MFN");
        request.put("goods_value", 1000.0);
        request.put("freight", 50.0);
        request.put("insurance", 100.0);
        request.put("effectiveDate", "2026-10-28"); //2026 future
        
        Map<String, Object> result = calculatorService.calculateLandedCost(request);
        
        assertTrue((Boolean) result.get("ok"));
        assertEquals(17.76, ((Number) result.get("rate_percent")).doubleValue(), 0.01);
        assertEquals("FOB", result.get("customs_basis"));
        assertEquals(1000.0, ((Number) result.get("customs_value")).doubleValue(), 0.01);
        assertNotNull(result.get("total_landed_cost"));
    }

    @Test
    void calculateLandedCost_ShouldReturnDifferentRates_ForDifferentDates() {
        // Test: Same country pair/HScode/agreement but different dates → different rates
        
        // 13th Jan 2025 13.85% last day of tariff 
        Map<String, Object> request1 = new LinkedHashMap<>();
        request1.put("exporter", "Singapore");
        request1.put("importer", "United States");
        request1.put("hsCode", "010121");
        request1.put("agreement", "MFN");
        request1.put("goods_value", 1000.0);
        request1.put("effectiveDate", "2025-01-13");
        
        Map<String, Object> result1 = calculatorService.calculateLandedCost(request1);
        double rate1 = ((Number) result1.get("rate_percent")).doubleValue();
        
        // 14th Jan 2025 New day of increased tariff applied
        Map<String, Object> request2 = new LinkedHashMap<>();
        request2.put("exporter", "Singapore");
        request2.put("importer", "United States");
        request2.put("hsCode", "010121");
        request2.put("agreement", "MFN");
        request2.put("goods_value", 1000.0);
        request2.put("effectiveDate", "2025-01-14");
        
        Map<String, Object> result2 = calculatorService.calculateLandedCost(request2);
        double rate2 = ((Number) result2.get("rate_percent")).doubleValue();
        
        // Rates should differ
        assertNotEquals(rate1, rate2, "Rates should differ for different dates");
    }

    @Test
    void calculateLandedCost_ShouldReturnCorrectRate_WhenAgreementChanges() {
        // Test: Same pairing HScode but different agreement 
        
        // MFN agreement
        Map<String, Object> request1 = new LinkedHashMap<>();
        request1.put("exporter", "Singapore");
        request1.put("importer", "Japan");
        request1.put("hsCode", "010121");
        request1.put("agreement", "MFN");
        request1.put("goods_value", 1000.0);
        request1.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result1 = calculatorService.calculateLandedCost(request1);
        assertTrue((Boolean) result1.get("ok"));
        // expected is 6.5% tariff (based on seed data)
        assertEquals(6.5, ((Number) result1.get("rate_percent")).doubleValue(), 0.01);
        
        // Different agreement (if data exists, e.g., "USMCA")
        Map<String, Object> request2 = new LinkedHashMap<>();
        request2.put("exporter", "Singapore");
        request2.put("importer", "Japan");
        request2.put("hsCode", "010121");
        request2.put("agreement", "CPTPP");  // Different agreement
        request2.put("goods_value", 1000.0);
        request2.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result2 = calculatorService.calculateLandedCost(request2);
        
        // Both should succeed but may have different rates
        assertTrue((Boolean) result2.get("ok"));
        // expected CPTPP is 4.0 tariff
        assertEquals(4.0, ((Number) result2.get("rate_percent")).doubleValue(), 0.01);
        
        // assertNotEquals(((Number) result1.get("rate_percent")).doubleValue(), ((Number) result2.get("rate_percent")).doubleValue());
    }

    @Test
    void calculateLandedCost_ShouldReturnError_WhenNegativeValues() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "Singapore");
        request.put("importer", "United States");
        request.put("hsCode", "010121");
        request.put("agreement", "MFN");
        request.put("goods_value", -1000.0);  // negative value 
        request.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result = calculatorService.calculateLandedCost(request);
        
        assertFalse((Boolean) result.get("ok"));
        assertTrue(((String) result.get("error")).contains("negative"));
    }

    @Test
    void calculateLandedCost_ShouldReturnError_WhenInvalidCountry() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "InvalidCountry123");
        request.put("importer", "United States");
        request.put("hsCode", "010121");
        request.put("agreement", "MFN");
        request.put("goods_value", 1000.0);
        request.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result = calculatorService.calculateLandedCost(request);
        
        assertFalse((Boolean) result.get("ok"));
        assertTrue(((String) result.get("error")).contains("Invalid country"));
    }

    // this test doesnt pass as of 11 nov 2025. Service throws an error 
    // [ERROR]   CalculatorServiceIT.calculateLandedCost_ShouldReturnError_WhenInvalidHSCode:152 ? 
    // IllegalState No tariff rate found for the given lane/HS/agreement within the requested date range. 
    // Note: open-ended rates are only considered valid through today.


    @Test
    void calculateLandedCost_ShouldReturnError_WhenInvalidHSCode() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "Singapore");
        request.put("importer", "United States");
        request.put("hsCode", "999999");  // Non-existent
        request.put("agreement", "MFN");
        request.put("goods_value", 1000.0);
        request.put("effectiveDate", "2025-10-28");
        
        // Map<String, Object> result = calculatorService.calculateLandedCost(request);
            
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> calculatorService.calculateLandedCost(request),
            "Should throw IllegalStateException for invalid HS code"
        );
        assertTrue(exception.getMessage().contains("No tariff rate found"));
        assertTrue(exception.getMessage().contains("HS"));
    }

    @Test
    void calculateLandedCost_ShouldReturnError_WhenAgreementMissing() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "Singapore");
        request.put("importer", "United States");
        request.put("hsCode", "010121");
        // NO agreement
        request.put("goods_value", 1000.0);
        request.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result = calculatorService.calculateLandedCost(request);
        
        assertFalse((Boolean) result.get("ok"));
        assertTrue(((String) result.get("error")).contains("agreement"));
    }
@Test
    void calculateLandedCost_ShouldHandle_ZeroFreightAndInsurance() {
        // Test no freight insurance (defaults to 0)
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("exporter", "Singapore");
        request.put("importer", "United States");
        request.put("hsCode", "010121");
        request.put("agreement", "MFN");
        request.put("goods_value", 1000.0);
        // NO freight or insurance
        request.put("effectiveDate", "2025-10-28");
        
        Map<String, Object> result = calculatorService.calculateLandedCost(request);
        
        assertTrue((Boolean) result.get("ok"));
        assertEquals(1000.0, ((Number) result.get("customs_value")).doubleValue(), 0.01);
    }
}


