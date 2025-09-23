package com.pricegrid.RestControllers;

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
    private final JdbcTemplate jdbc;

    public TariffController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Example Request:
     * GET /api/tariffs/lookup?exporter=SG&importer=US&hsCode=010121&agreement=MFN
     *
     * Returns:
     * {
     *   "rate_percent": 10.0,
     *   "customs_basis": "FOB",
     *   "tax_type": "VAT",
     *   "tax_rate_percent": 13.0
     * }
     */

    @GetMapping("/lookup")
    public Map<String, Object> lookup(
            @RequestParam String exporter,
            @RequestParam String importer,
            @RequestParam String hsCode,
            @RequestParam String agreement) {
        
        // Step 1: Lookup tariff % and customs basis
        String tariffSql = """
            SELECT tariff_rates.rate_percent,
                   importer_country.customs_basis,
                   importer_country.id AS importer_id
            FROM tariff_rates

            JOIN hs_codes
              ON hs_codes.id = tariff_rates.hs_code_id

            JOIN countries AS exporter_country
              ON exporter_country.id = tariff_rates.exporter_id

            JOIN countries AS importer_country
              ON importer_country.id = tariff_rates.importer_id

            JOIN agreements
              ON agreements.id = tariff_rates.agreement_id

            WHERE exporter_country.country_code = ?
              AND importer_country.country_code = ?
              AND hs_codes.hs_code = ?
              AND agreements.agreement_code = ?
              AND (tariff_rates.valid_to IS NULL OR tariff_rates.valid_to >= CURRENT_DATE)

            ORDER BY tariff_rates.valid_from DESC
            LIMIT 1
        """;

        //returns a Map<String, Object> where String = column name and Object = column value, essentially like a dictionary of one row
        Map<String, Object> tariffRow = jdbc.queryForMap(
                tariffSql, exporter, importer, hsCode, agreement);

        // Step 2: Lookup current VAT/GST for importer (NOT EXPORTER)
        String taxSql = """
            SELECT tax_rules.tax_type,
                   tax_rules.rate_percent
            FROM tax_rules

            WHERE tax_rules.country_id = ?
              AND (tax_rules.valid_to IS NULL OR tax_rules.valid_to >= CURRENT_DATE)

            ORDER BY tax_rules.valid_from DESC
            LIMIT 1
        """;
        
        Map<String, Object> taxRow = jdbc.queryForMap(
                taxSql, tariffRow.get("importer_id"));

        // Step 3: Combine results into response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rate_percent", tariffRow.get("rate_percent"));     // import duty %
        response.put("customs_basis", tariffRow.get("customs_basis"));   // CIF or FOB
        response.put("tax_type", taxRow.get("tax_type"));                // VAT or GST
        response.put("tax_rate_percent", taxRow.get("rate_percent"));    // VAT/GST %
        return response;
    }

}
