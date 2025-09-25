package com.ratewise.restcontrollers;
import com.ratewise.services.TariffService;

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
    public Map<String, Object> lookup(
            @RequestParam String exporter,
            @RequestParam String importer,
            @RequestParam String hsCode,
            @RequestParam String agreement) {
        return tariffService.getTariffInfo(exporter, importer, hsCode, agreement);
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
