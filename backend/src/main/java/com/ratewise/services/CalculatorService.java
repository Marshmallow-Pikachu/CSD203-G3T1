package com.ratewise.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.sql.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException;


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

    private static double round2DP(double value) {
        return BigDecimal.valueOf(value)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
    }

    private static final DateTimeFormatter SG_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Try to resolve a country to its ISO alpha-2 code.
     * Returns the code (e.g., "SG") or null if not found/blank.
     * Never throws for invalid input (lenient UX).
     */
    private String resolveCountryCode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null; // lenient: let caller decide how to message this
        }
        String input = raw.trim();

        final String byCodeSql = """
            SELECT country_code
            FROM countries
            WHERE UPPER(country_code) = UPPER(?)
            LIMIT 1
        """;

        final String byNameSql = """
            SELECT country_code
            FROM countries
            WHERE LOWER(country_name) = LOWER(?)
            LIMIT 1
        """;

        try {
            if (isIsoAlpha2(input)) {
                return jdbc.queryForObject(byCodeSql, String.class, input);
            } else {
                return jdbc.queryForObject(byNameSql, String.class, input);
            }
        } catch (EmptyResultDataAccessException e) {
            return null; // lenient: unknown country
        }
    }

    private boolean isIsoAlpha2(String s) {
        return s.length() == 2 && s.chars().allMatch(Character::isLetter);
    }
    
    /**
    * Fetch tariff % and tax info for a given trade lane and HS code.
    * Returns: rate %, customs basis (CIF/FOB), tax type, tax rate %.
    * Called inside calculateLandedCost function later.
    */
    private Map<String, Object> getTariffAndTax(String exporter, String importer, String hsCode, String agreement, LocalDate startDate, LocalDate endDate) {
        // Overlap window we will bind to SQL
        Date sqlStart = Date.valueOf(startDate);
        Date sqlEnd   = Date.valueOf(endDate);

        String tariffSql = """
            SELECT tr.rate_percent,
                  ic.customs_basis,
                  ic.id AS importer_id
            FROM tariff_rates tr

            JOIN hs_codes hc    ON hc.id = tr.hs_code_id
            JOIN countries ec   ON ec.id = tr.exporter_id
            JOIN countries ic   ON ic.id = tr.importer_id
            JOIN agreements ag  ON ag.id = tr.agreement_id

            WHERE ec.country_code = ?
              AND ic.country_code = ?
              AND hc.hs_code = ?
              AND ag.agreement_code = ?
              AND tr.valid_from <= ?
              AND (tr.valid_to IS NULL OR tr.valid_to >= ?)

            ORDER BY tr.valid_from DESC
            LIMIT 1
        """;

        // Use query(...) so we can handle "no rows" cleanly
        var tariffs = jdbc.queryForList(
            tariffSql,
            exporter, importer, hsCode, agreement,
            sqlEnd,  // valid_from <= end
            sqlStart // valid_to   >= start (or NULL)
        );

        if (tariffs.isEmpty()) {
            throw new IllegalStateException(
                "No tariff rate found for the given lane/HS/agreement within the date range " +
                startDate + " to " + endDate + "."
            );
        }

        Map<String, Object> tariffRow = tariffs.get(0);

        String taxSql = """
            SELECT tr.tax_type,
                  tr.rate_percent
            FROM tax_rules tr

            WHERE tr.country_id = ?
              AND tr.valid_from <= ?
              AND (tr.valid_to IS NULL OR tr.valid_to >= ?)

            ORDER BY tr.valid_from DESC
            LIMIT 1
        """;

        var taxes = jdbc.queryForList(
            taxSql,
            tariffRow.get("importer_id"),
            sqlEnd,
            sqlStart
        );

        if (taxes.isEmpty()) {
            throw new IllegalStateException(
                "No tax rule found for importer within the date range " +
                startDate + " to " + endDate + "."
            );
        }

        Map<String, Object> taxRow = taxes.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rate_percent",       tariffRow.get("rate_percent"));
        result.put("customs_basis",      tariffRow.get("customs_basis"));
        result.put("tax_type",           taxRow.get("tax_type"));
        result.put("tax_rate_percent",   taxRow.get("rate_percent"));
        return result;
    }


    /**
    {
      "exporter": "Singapore",
      "importer": "United States",
      "hsCode": "010121",
      "agreement": "MFN",
      "goods_value": 1000,
      "quantity": 2,
      "freight": 50,
      "insurance": 100,
      "start_date": "01/09/2025",
      "end_date": "30/09/2025"
    }
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

        //parsing date
        LocalDate startDate = null;
        LocalDate endDate   = null;
        try {
            if (request.get("start_date") != null) {
                startDate = LocalDate.parse((String) request.get("start_date"), SG_DATE_FORMAT);
            }
            if (request.get("end_date") != null) {
                endDate = LocalDate.parse((String) request.get("end_date"), SG_DATE_FORMAT);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use DD/MM/YYYY.", e);
        }        

        if (startDate == null && endDate == null) {
            // Fully optional: if neither is sent, use 'today' for both.
            startDate = LocalDate.now();
            endDate   = startDate;
        } else if (startDate == null) {
            startDate = endDate;
        } else if (endDate == null) {
            endDate = startDate;
        }        
        // incase user swaps dates
        if (startDate.isAfter(endDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate   = tmp;
        }        

        // 2. Resolve country names → ISO codes
        String exporter = resolveCountryCode(exporterInput);
        String importer = resolveCountryCode(importerInput);

        if (exporter == null || importer == null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("error", "Invalid country input.");
            response.put("exporter_input", exporterInput);
            response.put("importer_input", importerInput);
            response.put("hint", "Use ISO alpha-2 (e.g., \"SG\") or the exact country name.");
            // You can add a flag the frontend can check
            response.put("ok", false);
            return response; // keep UX smooth (no exception)
        }
     

        // 3. Adjust goods value by quantity
        double totalGoodsValue = goodsValue * quantity;

        // 4. Fetch duty rate, customs basis, and tax info
        Map<String, Object> tariffInfo = getTariffAndTax(exporter, importer, hsCode, agreement, startDate, endDate);

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
        response.put("total_landed_cost", round2DP(customsValue + duty + tax));
        return response;
    }   

}
