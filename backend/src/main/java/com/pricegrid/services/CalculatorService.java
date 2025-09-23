package com.pricegrid.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * calculatorservice
 *
 * Contains business logic for tariff + tax calculations.
 * - Resolves country names → codes
 * - Fetches tariff & tax info
 * - Applies CIF/FOB rules
 * - Handles quantity, duty, tax, and total landed cost
 */

@Service
public class CalculatorService {
    private final JdbcTemplate jdbc;

    public CalculatorService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Resolve a country input into a standard ISO country code.
     * Accepts either full country names ("Singapore") or codes ("SG").
     * Returns the code (e.g. "SG").
     */
    private String resolveCountryCode(String input) {
        String sql = """
            SELECT country_code
            FROM countries
            WHERE LOWER(country_name) = LOWER(?) OR UPPER(country_code) = UPPER(?)
            LIMIT 1
        """;

        return jdbc.queryForObject(sql, String.class, input, input);
    }
    
    /**
    * Fetch tariff % and tax info for a given trade lane and HS code.
    * Returns: rate %, customs basis (CIF/FOB), tax type, tax rate %.
    * Called inside calculateLandedCost function later.
    */
    private Map<String, Object> getTariffAndTax(String exporter, String importer, String hsCode, String agreement) {
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

        // Query returns a map with column:value where column is the column name and value is the column value
        Map<String, Object> tariffRow = jdbc.queryForMap(
                tariffSql, exporter, importer, hsCode, agreement);

        // SQL to fetch current VAT/GST rate for importer country
        String taxSql = """
            SELECT tax_rules.tax_type,
                   tax_rules.rate_percent
            FROM tax_rules
            WHERE tax_rules.country_id = ?
              AND (tax_rules.valid_to IS NULL OR tax_rules.valid_to >= CURRENT_DATE)
            ORDER BY tax_rules.valid_from DESC
            LIMIT 1
        """;

        // Query returns a map with column:value where column is the column name and value is the column value
        Map<String, Object> taxRow = jdbc.queryForMap(
                taxSql, tariffRow.get("importer_id"));

        // Merge both tariff and tax info into a single result map        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rate_percent", tariffRow.get("rate_percent"));
        result.put("customs_basis", tariffRow.get("customs_basis"));
        result.put("tax_type", taxRow.get("tax_type"));
        result.put("tax_rate_percent", taxRow.get("rate_percent"));
        return result;
    }

    /**
     * Calculate landed cost.
     * Request body is passed as a Map (from JSON).
     * Steps:
     * 1. Normalize inputs (countries, quantity)
     * 2. Fetch tariff/tax info
     * 3. Compute customs value (CIF vs FOB)
     * 4. Compute duty and VAT/GST
     * 5. Return full breakdown
     *
     * Example input:
     * {
     *   "exporter": "Singapore",
     *   "importer": "United States",
     *   "hsCode": "010121",
     *   "agreement": "MFN",
     *   "goods_value": 1000.0,
     *   "quantity": 2,
     *   "freight": 50.0,
     *   "insurance": 100.0
     * }
     */
    public Map<String, Object> calculateLandedCost(Map<String, Object> request) {
        // 1. Extract inputs from request body
        String exporterInput = (String) request.get("exporter");
        String importerInput = (String) request.get("importer");
        String hsCode        = (String) request.get("hsCode");
        String agreement     = (String) request.get("agreement");

        double goodsValue = ((Number) request.get("goods_value")).doubleValue();
        double freight    = request.get("freight")   != null ? ((Number) request.get("freight")).doubleValue()   : 0.0;
        double insurance  = request.get("insurance") != null ? ((Number) request.get("insurance")).doubleValue() : 0.0;
        int quantity      = request.get("quantity")  != null ? ((Number) request.get("quantity")).intValue()    : 1;

        // 2. Resolve country names → ISO codes
        String exporter = resolveCountryCode(exporterInput);
        String importer = resolveCountryCode(importerInput);

        // 3. Adjust goods value by quantity
        double totalGoodsValue = goodsValue * quantity;

        // 4. Fetch duty rate, customs basis, and tax info
        Map<String, Object> tariffInfo = getTariffAndTax(exporter, importer, hsCode, agreement);

        double dutyRate     = ((Number) tariffInfo.get("rate_percent")).doubleValue();
        String customsBasis = (String) tariffInfo.get("customs_basis");
        String taxType      = (String) tariffInfo.get("tax_type");
        double taxRate      = ((Number) tariffInfo.get("tax_rate_percent")).doubleValue();

        // 5. Compute customs value depending on CIF vs FOB
        double customsValue = customsBasis.equalsIgnoreCase("CIF")
                ? totalGoodsValue + freight + insurance
                : totalGoodsValue;

        // 6. Compute duty (import tax on customs value)
        double duty = customsValue * (dutyRate / 100.0);

        // 7. Compute VAT/GST (applied on customs value + duty)
        double tax = (customsValue + duty) * (taxRate / 100.0);

        // 8. Build response map (what will be returned as JSON)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("exporter_input", exporterInput);
        response.put("importer_input", importerInput);
        response.put("exporter_code", exporter);
        response.put("importer_code", importer);
        response.put("hs_code", hsCode);
        response.put("agreement", agreement);
        response.put("customs_basis", customsBasis);
        response.put("rate_percent", dutyRate);
        response.put("customs_value", customsValue);
        response.put("duty", duty);
        response.put("tax_type", taxType);
        response.put("tax_rate_percent", taxRate);
        response.put("tax", tax);
        response.put("quantity", quantity);
        response.put("total_landed_cost", customsValue + duty + tax);
        return response;
    }   

}
