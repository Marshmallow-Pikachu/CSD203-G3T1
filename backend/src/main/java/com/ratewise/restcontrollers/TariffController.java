package com.ratewise.restcontrollers;

import com.ratewise.dto.TariffListItem;

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
            (Double) result.get("rate_percent"),
            (String) result.get("customs_basis")
        );
        return ResponseEntity.ok(response);
    }

    // Tariff dashboard listing
    @GetMapping("/list")
    public ResponseEntity<List<TariffListItem>> listTariffs(
            @RequestParam(required = false) String importer,
            @RequestParam(required = false) String exporter,
            @RequestParam(required = false) String agreement) {

        List<Map<String,Object>> rows = tariffService.listTariffs(importer, exporter, agreement);

        List<TariffListItem> dtos = rows.stream().map(row -> new TariffListItem(
            (String) row.get("hs_code"),
            (String) row.get("hs_description"),
            (String) row.get("exporter_code"),
            (String) row.get("exporter_name"),
            (String) row.get("importer_code"),
            (String) row.get("importer_name"),
            (String) row.get("agreement_code"),
            (String) row.get("agreement_name"),
            toDouble(row.get("rate_percent")),
            (String) row.get("customs_basis")
        )).toList();

        return ResponseEntity.ok(dtos);
    }
    private static Double toDouble(Object o) {
        return o == null ? null : ((Number)o).doubleValue();
    }
}
