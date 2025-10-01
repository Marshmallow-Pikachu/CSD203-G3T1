package com.ratewise.restcontrollers;

import com.ratewise.dto.TariffLookupResponse;
import com.ratewise.services.TariffService;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * TariffController
 * Provides a lookup endpoint for import duty + VAT/GST.
 */

@RestController
@RequestMapping("/api/v1/tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    // Single tariff lookup
    @GetMapping("/lookup")
    public ResponseEntity<TariffLookupResponse> lookup(
            @RequestParam String exporter,
            @RequestParam String importer,
            @RequestParam String hsCode, 
            @RequestParam String agreement) {
        Map<String, Object> result = tariffService.getTariffInfo(exporter, importer, hsCode, agreement);
        
        System.out.println(result);
        TariffLookupResponse response = new TariffLookupResponse(
            (boolean) result.get("ok"),
            (String) result.get("hs_code"),
            (String) result.get("hs_description"),
            (String) result.get("exporter_code"),
            (String) result.get("exporter_name"),
            (String) result.get("importer_code"),
            (String) result.get("importer_name"),
            (String) result.get("agreement_code"),
            (String) result.get("agreement_name"),
            (Number) result.get("rate_percent"),
            (String) result.get("customs_basis")
        );
        return ResponseEntity.ok(response);
    }

    // Tariff dashboard listing
    @GetMapping("/list")
    public List<Map<String, Object>> listTariffs(
            @RequestParam(required = false) String importer,
            @RequestParam(required = false) String exporter,
            @RequestParam(required = false) String agreement) {
        return tariffService.listTariffs(importer, exporter, agreement);
    }
}
