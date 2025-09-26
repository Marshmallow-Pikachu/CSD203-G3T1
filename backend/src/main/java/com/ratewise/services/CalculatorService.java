package com.ratewise.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.sql.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException;


/**
 * # CalculatorService
 *
 * Business logic for tariff & tax calculations and landed-cost estimation.
 *
 * Responsibilities:
 * - Resolve user inputs (country names/codes, HS code or product description).
 * - Fetch tariff and tax rules valid for a given date range.
 * - Apply CIF/FOB rules, compute customs value, duty, VAT/GST, and total landed cost.
 */

@Service
public class CalculatorService {
    private final JdbcTemplate jdbc;
    private record DateRange(LocalDate start, LocalDate end) {}

    /** Formatter for Singapore-style dates provided by the client, e.g. "01/09/2025". */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,                 // 1990-03-11
        DateTimeFormatter.ofPattern("dd/MM/uuuu"),        // 11/03/1990
        DateTimeFormatter.ofPattern("d/M/uuuu")           // 1/3/1990
    );

    public CalculatorService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------
    // Utility Helpers
    // ------------------------
    //Rounds a double to 2 decimal places using HALF_UP mode. Used mainly for monetary values (like the final returned landed cost)
    private static double round2DP(double value) {
        return BigDecimal.valueOf(value)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
    }

    //returns true if the string looks like a valid 2-letter ISO alpha-2 code (e.g. "SG")
    private boolean isIsoAlpha2(String s) {
        return s.length() == 2 && s.chars().allMatch(Character::isLetter);
    }
    /**
     * Resolve a country input to its ISO alpha-2 code.
     *
     * Accepts either:
     * - exact country code (e.g., "SG"), or
     * - exact stored country name (case-insensitive).
     *
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

    /**
     * Resolve an HS code from a product description using the {@code hs_codes} table.
     *
     * Strategy:
     * - First try exact (case-insensitive) match on description.
     * - Fallback to substring LIKE (case-insensitive), preferring the shortest description.
     *
     */
    private String resolveHsCodeFromDescription(String desc) {
        if (desc == null || desc.isBlank()) return null;

        // exact (case-insensitive)
        final String exactSql = """
            SELECT hs_code
            FROM hs_codes
            WHERE LOWER(description) = LOWER(?)
            LIMIT 1
        """;

        // substring (case-insensitive)
        final String likeSql = """
            SELECT hs_code
            FROM hs_codes
            WHERE LOWER(description) LIKE LOWER(?)
            ORDER BY LENGTH(description) ASC
            LIMIT 1
        """;

        try {
            return jdbc.queryForObject(exactSql, String.class, desc.trim());
        } catch (EmptyResultDataAccessException ignored) {
            // fall through
        }

        try {
            return jdbc.queryForObject(likeSql, String.class, "%" + desc.trim() + "%");
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }    

    /**
     * Resolve HS code from the request payload:
     * - Prefer explicit {@code hsCode} field.
     * - If missing, try mapping {@code productDescription} via {@link #resolveHsCodeFromDescription(String)}.
     *
     */
    private String resolveHsCodeFromRequest(Map<String, Object> req) {
        String hsCode = (String) req.get("hsCode");
        if (hsCode != null && !hsCode.isBlank()) {
            return hsCode;
        }

        String productDescription = (String) req.get("productDescription");
        if (productDescription == null || productDescription.isBlank()) {
            return null;
        }
        // Leverage your existing description→HS resolver
        return resolveHsCodeFromDescription(productDescription);
    }


    private static LocalDate parseFlexibleDate(Object value, String fieldName) {
        if (value == null) return null;
        final String s = value.toString().trim();
        if (s.isEmpty()) return null;

        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException(
            "Invalid date format for '" + fieldName + "'. Accepted: YYYY-MM-DD or DD/MM/YYYY."
        );
    }    
    /**
     * Parse "start_date" and "endDate" from the request (format: dd/MM/yyyy) and normalize them:
     * - If both missing → use today for both.
     * - If one bound missing → use the other for both (single-day window).
     * - If start > end → swap them.
     *
    */
    private DateRange parseDateRange(Map<String, Object> req) {
        LocalDate start = null, end = null;

        try {
            start = parseFlexibleDate(req.get("startDate"), "startDate");
            end   = parseFlexibleDate(req.get("endDate"), "endDate");
        } catch (IllegalArgumentException ex) {
            // Re-throw so your exception handler / caller can format {message, hint}
            throw ex;
        }

        // If both missing → today
        if (start == null && end == null) {
            start = LocalDate.now();
            end = start;
        } else if (start == null) {
            // Only end provided → single-day window
            start = end;
        } else if (end == null) {
            // Only start provided → single-day window
            end = start;
        }

        // Normalize order
        if (start.isAfter(end)) {
            LocalDate t = start; start = end; end = t;
        }

        return new DateRange(start, end);
    }

    
    /**
    * Fetch tariff % and tax info for a given trade lane and HS code, and time frame.
    * Returns: rate %, customs basis (CIF/FOB), tax type, tax rate %.
    * Called inside calculateLandedCost function later.
    */
    private Map<String, Object> getTariffAndTax(String exporter, String importer, String hsCode, String agreement, LocalDate startDate, LocalDate endDate) {
        // Overlap window we will bind to SQL
        Date sqlStart = Date.valueOf(startDate);
        Date sqlEnd   = Date.valueOf(endDate);

        // Tariff: pick latest by valid_from that overlaps the requested window
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

        // Tax: pick latest by valid_from that overlaps the requested window
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
    Example Input
    {
      "exporter": "Singapore",
      "importer": "United States",
      "hsCode": "010121",
      "agreement": "MFN",
      "goods_value": 1000,
      "quantity": 2,
      "freight": 50,
      "insurance": 100,
      "startDate": "01/09/2025",
      "endDate": "30/09/2025"
    }
     */
    public Map<String, Object> calculateLandedCost(Map<String, Object> request) {
         Map<String,Object> response = new LinkedHashMap<>();
        // 1. Extract inputs from request body
        String exporterInput = (String) request.get("exporter");
        String importerInput = (String) request.get("importer");
        String agreement     = (String) request.get("agreement");

        // Resolve HS code (prefer hsCode, else map from productDescription)
        String hsCode = resolveHsCodeFromRequest(request);
        if (hsCode == null || hsCode.isBlank()) {
            response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", "Either hsCode or productDescription must be provided (no match found).");
            response.put("productDescription", (String) request.get("productDescription"));
            return response;
        }

        double goodsValue = ((Number) request.get("goods_value")).doubleValue();
        double freight    = request.get("freight")   != null ? ((Number) request.get("freight")).doubleValue()   : 0.0;
        double insurance  = request.get("insurance") != null ? ((Number) request.get("insurance")).doubleValue() : 0.0;
        int quantity      = request.get("quantity")  != null ? ((Number) request.get("quantity")).intValue()    : 1;

        //parsing date
        DateRange range = parseDateRange(request);
        LocalDate startDate = range.start();
        LocalDate endDate   = range.end();

        // 2. Resolve country names → ISO codes
        String exporter = resolveCountryCode(exporterInput);
        String importer = resolveCountryCode(importerInput);

        if (exporter == null || importer == null) {
            response = new LinkedHashMap<>();
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
        response = new LinkedHashMap<>();
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
