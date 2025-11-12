package com.ratewise.restcontrollers;

import com.ratewise.dto.TariffAdminRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TariffAdminControllerIT {
    
    @Autowired
    private TariffAdminController controller;
    
    private static Integer testId;

    @Test
    @Order(1)
    void testCreate() {
        TariffAdminRequest req = new TariffAdminRequest();
        req.exporterCode = "SG";
        req.importerCode = "US";
        req.hsCode = "010121";
        req.agreementCode = "MFN";
        req.ratePercent = new BigDecimal("5.0");
        req.validFrom = "2025-01-01";
        
        var response = controller.create(req);
        
        assertEquals(201, response.getStatusCode().value());
        testId = ((Number) response.getBody().get("id")).intValue();
    }
    
    @Test
    @Order(2)
    void testGet() {
        var response = controller.get(testId);
        assertEquals(200, response.getStatusCode().value());
    }
    
    @Test
    @Order(3)
    void testListAll() {
        var response = controller.listAll();
        assertEquals(200, response.getStatusCode().value());
    }
    
    @Test
    @Order(4)
    void testUpdate() {
        TariffAdminRequest req = new TariffAdminRequest();
        req.exporterCode = "SG";
        req.importerCode = "US";
        req.hsCode = "010121";
        req.agreementCode = "MFN";
        req.ratePercent = new BigDecimal("10.0");
        req.validFrom = "2025-01-01";
        
        var response = controller.update(testId, req);
        assertEquals(200, response.getStatusCode().value());
    }
    
    @Test
    @Order(5)
    void testDelete() {
        var response = controller.delete(testId);
        assertEquals(204, response.getStatusCode().value());
    }
}
