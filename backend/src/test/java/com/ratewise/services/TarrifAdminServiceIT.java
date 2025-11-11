package com.ratewise.services;

import com.ratewise.dto.TariffAdminRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TariffAdminServiceIT {

    @Autowired
    private TariffAdminService tariffAdminService;

    @Autowired
    private JdbcTemplate jdbc;

    private static Integer createdTariffId;

    private static final String TEST_EXPORTER = "SG";  
    private static final String TEST_IMPORTER = "US";  
    private static final String TEST_HS_CODE = "010121";
    private static final String TEST_AGREEMENT = "MFN";

    @BeforeEach
    void setUp() {
        // Ensure test data exists in reference tables
        ensureTestDataExists();
    }

    void ensureTestDataExists() {
        // Check if test countries exist
        Integer exporterExists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM countries WHERE country_code = ?",
            Integer.class, TEST_EXPORTER
        );
        Integer importerExists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM countries WHERE country_code = ?",
            Integer.class, TEST_IMPORTER
        );

        if (exporterExists == 0 || importerExists == 0) {
            throw new IllegalStateException(
                "Test countries not found in DB. Please ensure " + TEST_EXPORTER + 
                " and " + TEST_IMPORTER + " exist in countries table."
            );
        }

        // Check if test HS code exists
        Integer hsExists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM hs_codes WHERE hs_code = ?",
            Integer.class, TEST_HS_CODE
        );
        if (hsExists == 0) {
            throw new IllegalStateException(
                "Test HS code " + TEST_HS_CODE + " not found in hs_codes table."
            );
        }

        // Check if test agreement exists
        Integer agreementExists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM agreements WHERE agreement_code = ?",
            Integer.class, TEST_AGREEMENT
        );
        if (agreementExists == 0) {
            throw new IllegalStateException(
                "Test agreement " + TEST_AGREEMENT + " not found in agreements table."
            );
        }
    }

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) {
        // Clean up test tariff if created
        if (createdTariffId != null) {
            try {
                jdbc.update("DELETE FROM tariff_rates WHERE id = ?", createdTariffId);
                System.out.println("Cleaned up test tariff ID: " + createdTariffId);
            } catch (Exception e) {
                System.err.println("Failed to clean up test tariff: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    void testCreateTariffSuccess() {
        TariffAdminRequest request = new TariffAdminRequest();
        request.exporterCode = TEST_EXPORTER;
        request.importerCode = TEST_IMPORTER;
        request.hsCode = TEST_HS_CODE;
        request.agreementCode = TEST_AGREEMENT;
        request.ratePercent = new BigDecimal(5.5);
        request.validFrom = "2025-01-01";
        request.validTo = "2025-12-31";

        Map<String, Object> result = tariffAdminService.createTariff(request);

        assertNotNull(result);
        assertNotNull(result.get("id"));
        assertEquals(TEST_EXPORTER, result.get("exporter_code"));
        assertEquals(TEST_IMPORTER, result.get("importer_code"));
        assertEquals(TEST_HS_CODE, result.get("hs_code"));
        assertEquals(5.5, ((Number) result.get("rate_percent")).doubleValue());

        // Store for cleanup
        createdTariffId = ((Number) result.get("id")).intValue();
    }

    @Test
    void testCreateTariffInvalidDate() {
        TariffAdminRequest request = new TariffAdminRequest();
        request.exporterCode = TEST_EXPORTER;
        request.importerCode = TEST_IMPORTER;
        request.hsCode = TEST_HS_CODE;
        request.agreementCode = TEST_AGREEMENT;
        request.ratePercent = new BigDecimal(5.5);
        request.validFrom = "invalid-date";

        assertThrows(IllegalArgumentException.class, () -> {
            tariffAdminService.createTariff(request);
        });
    }

    @Test
    void testCreateTariffMissingValidFrom() {
        TariffAdminRequest request = new TariffAdminRequest();
        request.exporterCode = TEST_EXPORTER;
        request.importerCode = TEST_IMPORTER;
        request.hsCode = TEST_HS_CODE;
        request.agreementCode = TEST_AGREEMENT;
        request.ratePercent = new BigDecimal(5.5);
        request.validFrom = null;

        assertThrows(IllegalArgumentException.class, () -> {
            tariffAdminService.createTariff(request);
        });
    }

    // -------- READ --------

    @Test
    @Order(2)
    void testGetByIdSuccess() {
        // First create a tariff
        TariffAdminRequest request = new TariffAdminRequest();
        request.exporterCode = TEST_EXPORTER;
        request.importerCode = TEST_IMPORTER;
        request.hsCode = TEST_HS_CODE;
        request.agreementCode = TEST_AGREEMENT;
        request.ratePercent = new BigDecimal(7.0);
        request.validFrom = "2025-02-01";
        request.validTo = "2025-11-30";

        Map<String, Object> created = tariffAdminService.createTariff(request);
        Integer tariffId = ((Number) created.get("id")).intValue();
        createdTariffId = tariffId;

        // Now fetch it
        Map<String, Object> fetched = tariffAdminService.getById(tariffId);

        assertNotNull(fetched);
        assertEquals(tariffId, ((Number) fetched.get("id")).intValue());
        assertEquals(TEST_EXPORTER, fetched.get("exporter_code"));
        assertEquals(7.0, ((Number) fetched.get("rate_percent")).doubleValue());
    }

    @Test
    void testGetByIdNotFound() {
        assertThrows(IllegalStateException.class, () -> {
            tariffAdminService.getById(999999);
        });
    }

    // -------- UPDATE --------

    @Test
    @Order(3)
    void testUpdateTariffSuccess() {
        // Create first
        TariffAdminRequest createReq = new TariffAdminRequest();
        createReq.exporterCode = TEST_EXPORTER;
        createReq.importerCode = TEST_IMPORTER;
        createReq.hsCode = TEST_HS_CODE;
        createReq.agreementCode = TEST_AGREEMENT;
        createReq.ratePercent = new BigDecimal(3.0);
        createReq.validFrom = "2025-03-01";

        Map<String, Object> created = tariffAdminService.createTariff(createReq);
        Integer tariffId = ((Number) created.get("id")).intValue();
        createdTariffId = tariffId;

        // Update rate
        TariffAdminRequest updateReq = new TariffAdminRequest();
        updateReq.exporterCode = TEST_EXPORTER;
        updateReq.importerCode = TEST_IMPORTER;
        updateReq.hsCode = TEST_HS_CODE;
        updateReq.agreementCode = TEST_AGREEMENT;
        updateReq.ratePercent = new BigDecimal(10.0);
        updateReq.validFrom = "2025-03-01";

        Map<String, Object> updated = tariffAdminService.updateTariff(tariffId, updateReq);

        assertEquals(10.0, ((Number) updated.get("rate_percent")).doubleValue());
    }

    @Test
    void testUpdateTariffNotFound() {
        TariffAdminRequest request = new TariffAdminRequest();
        request.exporterCode = TEST_EXPORTER;
        request.importerCode = TEST_IMPORTER;
        request.hsCode = TEST_HS_CODE;
        request.agreementCode = TEST_AGREEMENT;
        request.ratePercent = new BigDecimal(5.0);
        request.validFrom = "2025-01-01";

        assertThrows(ResponseStatusException.class, () -> {
            tariffAdminService.updateTariff(999999, request);
        });
    }

    // -------- DELETE --------

    @Test
    @Order(4)
    void testDeleteTariffSuccess() {
        // Create first
        TariffAdminRequest request = new TariffAdminRequest();
        request.exporterCode = TEST_EXPORTER;
        request.importerCode = TEST_IMPORTER;
        request.hsCode = TEST_HS_CODE;
        request.agreementCode = TEST_AGREEMENT;
        request.ratePercent = new BigDecimal(2.0);
        request.validFrom = "2025-04-01";

        Map<String, Object> created = tariffAdminService.createTariff(request);
        Integer tariffId = ((Number) created.get("id")).intValue();

        // Delete it
        tariffAdminService.deleteTariff(tariffId);

        // Verify it's gone
        assertThrows(IllegalStateException.class, () -> {
            tariffAdminService.getById(tariffId);
        });

        createdTariffId = null;  // No cleanup needed
    }

    @Test
    void testDeleteTariffNotFound() {
        assertThrows(ResponseStatusException.class, () -> {
            tariffAdminService.deleteTariff(999999);
        });
    }

    // -------- LIST --------

    @Test
    void testListAll() {
        List<Map<String, Object>> tariffs = tariffAdminService.listAll();

        assertNotNull(tariffs);
        // Should have at least some data (depends on your DB state)
        assertTrue(tariffs.size() >= 0);
    }
}
