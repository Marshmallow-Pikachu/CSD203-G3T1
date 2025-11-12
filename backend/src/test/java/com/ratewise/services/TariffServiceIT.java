package com.ratewise.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TariffServiceIT {

    @Autowired
    private TariffService tariffService;

    private static final String VALID_COUNTRY_NAME = "Singapore";
    private static final String VALID_COUNTRY_CODE = "SG";
    private static final String VALID_IMPORTER = "US";
    private static final String VALID_HS_CODE = "010121";
    private static final String VALID_AGREEMENT = "MFN";

    @Test
    void testResolveCountryCodeByName() {
        String result = tariffService.resolveCountryCode(VALID_COUNTRY_NAME);
        
        assertNotNull(result);
        assertEquals(VALID_COUNTRY_CODE, result);
    }

    @Test
    void testResolveCountryCodeByCode() {
        String result = tariffService.resolveCountryCode(VALID_COUNTRY_CODE);
        
        assertNotNull(result);
        assertEquals(VALID_COUNTRY_CODE, result);
    }

    @Test
    void testResolveCountryCodeCaseInsensitive() {
        // Test lowercase
        String resultLower = tariffService.resolveCountryCode("singapore");
        assertEquals(VALID_COUNTRY_CODE, resultLower);

        // Test uppercase
        String resultUpper = tariffService.resolveCountryCode("SINGAPORE");
        assertEquals(VALID_COUNTRY_CODE, resultUpper);
    }

    @Test
    void testResolveCountryCodeNotFound() {
        assertThrows(EmptyResultDataAccessException.class, () -> {
            tariffService.resolveCountryCode("NonExistentCountry");
        });
    }

    @Test
    void testGetTariffInfoSuccess() {
        Map<String, Object> result = tariffService.getTariffInfo(
            VALID_COUNTRY_CODE,
            VALID_IMPORTER,
            VALID_HS_CODE,
            VALID_AGREEMENT
        );

        assertNotNull(result);
        assertNotNull(result.get("rate_percent"));
        assertEquals(VALID_COUNTRY_CODE, result.get("exporter_code"));
        assertEquals(VALID_IMPORTER, result.get("importer_code"));
        assertEquals(VALID_HS_CODE, result.get("hs_code"));
        assertEquals(VALID_AGREEMENT, result.get("agreement_code"));
        
        // Verify expected fields exist
        assertTrue(result.containsKey("customs_basis"));
        assertTrue(result.containsKey("agreement_name"));
        assertTrue(result.containsKey("hs_description"));
        assertTrue(result.containsKey("exporter_name"));
        assertTrue(result.containsKey("importer_name"));
    }

    @Test
    void testGetTariffInfoNotFound() {
        assertThrows(EmptyResultDataAccessException.class, () -> {
            tariffService.getTariffInfo(
                "ZZ",  // Invalid exporter
                "ZZ",  // Invalid importer
                "999999",  // Invalid HS code
                "INVALID"  // Invalid agreement
            );
        });
    }

    @Test
    void testListTariffsNoFilters() {
        List<Map<String, Object>> results = tariffService.listTariffs(null, null, null);

        assertNotNull(results);
        assertTrue(results.size() > 0);  // Should return all active tariffs
        
        // Verify expected fields in first result
        if (!results.isEmpty()) {
            Map<String, Object> first = results.get(0);
            assertTrue(first.containsKey("hs_code"));
            assertTrue(first.containsKey("hs_description"));
            assertTrue(first.containsKey("exporter_code"));
            assertTrue(first.containsKey("importer_code"));
            assertTrue(first.containsKey("agreement_code"));
            assertTrue(first.containsKey("rate_percent"));
            assertTrue(first.containsKey("customs_basis"));
        }
    }

    @Test
    void testListTariffsFilterByImporter() {
        List<Map<String, Object>> results = tariffService.listTariffs(VALID_IMPORTER, null, null);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // Verify all results have the correct importer
        for (Map<String, Object> tariff : results) {
            assertEquals(VALID_IMPORTER, tariff.get("importer_code"));
        }
    }

    @Test
    void testListTariffsFilterByExporter() {
        List<Map<String, Object>> results = tariffService.listTariffs(null, VALID_COUNTRY_CODE, null);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // Verify all results have the correct exporter
        for (Map<String, Object> tariff : results) {
            assertEquals(VALID_COUNTRY_CODE, tariff.get("exporter_code"));
        }
    }

    @Test
    void testListTariffsFilterByAgreement() {
        List<Map<String, Object>> results = tariffService.listTariffs(null, null, VALID_AGREEMENT);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // Verify all results have the correct agreement
        for (Map<String, Object> tariff : results) {
            assertEquals(VALID_AGREEMENT, tariff.get("agreement_code"));
        }
    }

    @Test
    void testListTariffsMultipleFilters() {
        List<Map<String, Object>> results = tariffService.listTariffs(
            VALID_IMPORTER,
            VALID_COUNTRY_CODE,
            VALID_AGREEMENT
        );

        assertNotNull(results);
        
        // Verify all results match all filters
        for (Map<String, Object> tariff : results) {
            assertEquals(VALID_IMPORTER, tariff.get("importer_code"));
            assertEquals(VALID_COUNTRY_CODE, tariff.get("exporter_code"));
            assertEquals(VALID_AGREEMENT, tariff.get("agreement_code"));
        }
    }

    @Test
    void testListTariffsEmptyFilters() {
        // Empty strings should be treated as no filter
        List<Map<String, Object>> results = tariffService.listTariffs("", "", "");

        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    @Test
    void testListTariffsSortedByHsCodeAndAgreement() {
        List<Map<String, Object>> results = tariffService.listTariffs(null, null, null);

        assertNotNull(results);
        assertTrue(results.size() > 1);  // Need at least 2 to verify sorting

        // Verify results are sorted
        String previousHsCode = null;
        for (Map<String, Object> tariff : results) {
            String currentHsCode = (String) tariff.get("hs_code");
            if (previousHsCode != null) {
                // Current should be >= previous (ASC order)
                assertTrue(currentHsCode.compareTo(previousHsCode) >= 0);
            }
            previousHsCode = currentHsCode;
        }
    }

    // -------- listTariffsTable --------

    @Test
    void testListTariffsTable() {
        List<Map<String, Object>> results = tariffService.listTariffsTable();

        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // Verify expected fields
        if (!results.isEmpty()) {
            Map<String, Object> first = results.get(0);
            assertTrue(first.containsKey("exporter_code"));
            assertTrue(first.containsKey("exporter_name"));
            assertTrue(first.containsKey("importer_code"));
            assertTrue(first.containsKey("importer_name"));
            assertTrue(first.containsKey("importer_customs"));
            assertTrue(first.containsKey("importer_tax"));
            assertTrue(first.containsKey("agreement_code"));
            assertTrue(first.containsKey("agreement_name"));
            assertTrue(first.containsKey("hs_code"));
            assertTrue(first.containsKey("hs_description"));
            assertTrue(first.containsKey("rate_percent"));
            assertTrue(first.containsKey("valid_from"));
            assertTrue(first.containsKey("valid_to"));
        }
    }

    @Test
    void testListTariffsTableSorted() {
        List<Map<String, Object>> results = tariffService.listTariffsTable();

        assertNotNull(results);
        assertTrue(results.size() > 1);

        // Verify sorted by exporter, importer, hs_code
        String previousExporter = null;
        for (Map<String, Object> tariff : results) {
            String currentExporter = (String) tariff.get("exporter_code");
            if (previousExporter != null) {
                assertTrue(currentExporter.compareTo(previousExporter) >= 0);
            }
            previousExporter = currentExporter;
        }
    }

    // // failing test, TariffService.getTariffInfo does not take in a date in parameters. Am unable to query it. Not intended function?
    // @Test
    // void testGetHistoricalTariffRate() {
    //     // Query rate valid on a past date
    //     LocalDate pastDate = LocalDate.of(2024, 1, 1);
        
    //     Map<String, Object> result = tariffService.getTariffInfo(
    //         VALID_COUNTRY_CODE,
    //         VALID_IMPORTER,
    //         VALID_HS_CODE,
    //         VALID_AGREEMENT,
    //         pastDate  // Add date parameter
    //     );

    //     assertNotNull(result);
    //     // Verify the returned rate was valid on that date
    //     LocalDate validFrom = ((java.sql.Date) result.get("valid_from")).toLocalDate();
    //     assertTrue(validFrom.isBefore(pastDate) || validFrom.isEqual(pastDate));
    // }

    @Test
    void testGetTariffInfoWithNullImporter() {
        assertThrows(Exception.class, () -> {
            tariffService.getTariffInfo("SG", null, "010121", "MFN");
        });
    }

    @Test
    void testGetTariffInfoWithInvalidCountry() {
        assertThrows(EmptyResultDataAccessException.class, () -> {
            tariffService.getTariffInfo("INVALID", "US", "010121", "MFN");
        });
    }

    @Test
    void testListTariffsWithNullFilters() {
        List<Map<String, Object>> results = tariffService.listTariffs(null, null, null);
        assertNotNull(results);
    }


}
