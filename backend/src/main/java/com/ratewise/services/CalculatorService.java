package com.ratewise.services;

import com.ratewise.exceptions.ApiValidationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.sql.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;

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

    private static boolean isBlank(Object v) {
        return v == null || (v instanceof String s && s.trim().isEmpty());
    }   

    private static double asDouble(Object v, String field) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString().trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Field '" + field + "' must be a number."); }
    }
    private static int asInt(Object v, String field) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString().trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Field '" + field + "' must be an integer."); }
    }    

    // Normalizes codes by trimming and converting to uppercase. This includes ISO/country code (e.g. SG) and Agreement codes (e.g. MFN)
    private static String normalizeCodeInput(String rawInput) {
        return (rawInput == null) ? null : rawInput.trim().toUpperCase();
    }

    // Normalizes HS code input: trim, remove internal spaces, convert to uppercase
    private static String normalizeHsCodeInput(String rawHsCode) {
        if (rawHsCode == null) return null;
        return rawHsCode.trim().replaceAll("\\s+", "").toUpperCase();
    }

    //returns true if the string looks like a valid 2-letter ISO alpha-2 code (e.g. "SG")
    private boolean isIsoAlpha2(String s) {
        return s != null && s.length() == 2 && s.chars().allMatch(Character::isLetter);
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
    private String resolveHsCodeFromRequest(Map<String, Object> requestPayload) {
        String rawHsCodeFromRequest = (String) requestPayload.get("hsCode");
        String resolvedHsCode;

        if (rawHsCodeFromRequest != null && !rawHsCodeFromRequest.isBlank()) {
            resolvedHsCode = normalizeHsCodeInput(rawHsCodeFromRequest);
        } else {
            String productDescriptionFromRequest = (String) requestPayload.get("productDescription");
            if (productDescriptionFromRequest == null || productDescriptionFromRequest.isBlank()) {
                return null;
            }
            resolvedHsCode = normalizeHsCodeInput(resolveHsCodeFromDescription(productDescriptionFromRequest));
        }

        if (resolvedHsCode == null || resolvedHsCode.isBlank()) return null;

        // MVP constraint: must be 6 characters
        if (resolvedHsCode.length() != 6) {
            throw new IllegalArgumentException("HS code must be exactly 6 characters for MVP.");
        }

        return resolvedHsCode;
    }
   
    private static LocalDate parseFlexibleDate(Object value, String fieldName) {
        if (value == null) return null;
        final String s = value.toString().trim();
        if (s.isEmpty()) return null;

        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try { return LocalDate.parse(s, f); }
            catch (DateTimeParseException ignored) {}
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

    private void preValidate(Map<String, Object> req) {
        List<String> errors = new ArrayList<>();

        // 0) Untouched form → "No inputs detected" (ignore 'agreement' which defaults to "MFN")
        boolean noMeaningfulText =
            isBlank(req.get("exporter")) &&
            isBlank(req.get("importer")) &&
            isBlank(req.get("hsCode")) &&
            isBlank(req.get("productDescription")) &&
            isBlank(req.get("startDate")) &&
            isBlank(req.get("endDate"));

        Double gv = null, fr = null, ins = null;
        Integer qty = null;

        // Parse numbers but collect parsing errors instead of throwing immediately
        try { gv  = req.get("goods_value") == null || isBlank(req.get("goods_value")) ? null : asDouble(req.get("goods_value"), "goods_value"); }
        catch (IllegalArgumentException e) { errors.add(e.getMessage()); }
        try { fr  = isBlank(req.get("freight"))   ? null : asDouble(req.get("freight"), "freight"); }
        catch (IllegalArgumentException e) { errors.add(e.getMessage()); }
        try { ins = isBlank(req.get("insurance")) ? null : asDouble(req.get("insurance"), "insurance"); }
        catch (IllegalArgumentException e) { errors.add(e.getMessage()); }
        try { qty = isBlank(req.get("quantity"))  ? null : asInt(req.get("quantity"), "quantity"); }
        catch (IllegalArgumentException e) { errors.add(e.getMessage()); }

        boolean noMeaningfulNumbers =
            (gv == null || gv == 0.0) &&
            (fr == null || fr == 0.0) &&
            (ins == null || ins == 0.0) &&
            (qty == null || qty == 0 || qty == 1);

        if ((req == null || req.isEmpty()) || (noMeaningfulText && noMeaningfulNumbers)) {
            errors.add("No inputs detected.");
        }

        // 1) Presence rules (ORDERED → deterministic)
        if (isBlank(req.get("exporter"))) errors.add("exporter is required.");
        if (isBlank(req.get("importer"))) errors.add("importer is required.");
        if (isBlank(req.get("agreement"))) errors.add("agreement is required (e.g., MFN, CPTPP).");
        if (isBlank(req.get("hsCode")) && isBlank(req.get("productDescription"))) {
            errors.add("Either hsCode or productDescription must be provided.");
        }
        if (gv == null) errors.add("goods_value is required.");

        // 2) Sign checks (only if parsed)
        if (gv != null && gv < 0)   errors.add("goods_value must not be negative.");
        if (fr != null && fr < 0)   errors.add("freight must not be negative.");
        if (ins != null && ins < 0) errors.add("insurance must not be negative.");
        if (qty != null && qty < 0) errors.add("quantity must not be negative.");

        if (!errors.isEmpty()) throw new ApiValidationException(errors);
    }    

    /**
    * Fetch tariff % and tax info for a given trade lane and HS code, and time frame.
    * Returns: rate %, customs basis (CIF/FOB), tax type, tax rate %.
    * Called inside calculateLandedCost function later.
    */
    private Map<String, Object> getTariffAndTax(
        String exporter, String importer, String hsCode, String agreement,
        LocalDate startDate, LocalDate endDate
    ) {
        // Overlap window bound to SQL
        Date sqlStart = Date.valueOf(startDate);
        Date sqlEnd   = Date.valueOf(endDate);

        // Treat NULL valid_to as "valid through CURRENT_DATE" (not infinite)
        String tariffSql = """
            SELECT tr.rate_percent,
                ic.customs_basis,
                ic.id AS importer_id
            FROM tariff_rates tr

            JOIN hs_codes   hc ON hc.id = tr.hs_code_id
            JOIN countries  ec ON ec.id = tr.exporter_id
            JOIN countries  ic ON ic.id = tr.importer_id
            JOIN agreements ag ON ag.id = tr.agreement_id

            WHERE UPPER(ec.country_code) = UPPER(?)
            AND UPPER(ic.country_code) = UPPER(?)
            AND UPPER(hc.hs_code) = UPPER(?)
            AND UPPER(ag.agreement_code) = UPPER(?)
            AND tr.valid_from <= ?
            AND COALESCE(tr.valid_to, CURRENT_DATE) >= ?
            
            ORDER BY tr.valid_from DESC
            LIMIT 1
        """;

        var tariffs = jdbc.queryForList(
            tariffSql,
            exporter, importer, hsCode, agreement,
            sqlEnd,   // valid_from <= end
            sqlStart  // coalesced valid_to >= start
        );

        if (tariffs.isEmpty()) {
            throw new IllegalStateException(
                "No tariff rate found for the given lane/HS/agreement within the requested date range. " +
                "Note: open-ended rates are only considered valid through today."
            );
        }

        Map<String, Object> tariffRow = tariffs.get(0);

        // Same treatment for tax rules
        String taxSql = """
            SELECT tr.tax_type,
                tr.rate_percent
            FROM tax_rules tr
            WHERE tr.country_id = ?
            AND tr.valid_from <= ?
            AND COALESCE(tr.valid_to, CURRENT_DATE) >= ?
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
                "No tax rule found for importer within the requested date range. " +
                "Note: open-ended rules are only considered valid through today."
            );
        }

        Map<String, Object> taxRow = taxes.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rate_percent",     tariffRow.get("rate_percent"));
        result.put("customs_basis",    tariffRow.get("customs_basis"));
        result.put("tax_type",         taxRow.get("tax_type"));
        result.put("tax_rate_percent", taxRow.get("rate_percent"));
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
        preValidate(request);
        
        // Optional: treat all-empty strings & zeros as no input
        boolean noMeaningfulText =
            isBlank(request.get("exporter")) &&
            isBlank(request.get("importer")) &&
            isBlank(request.get("hsCode")) &&
            isBlank(request.get("productDescription")) &&
            isBlank(request.get("startDate")) &&
            isBlank(request.get("endDate"));

        double gv  = asDouble(request.get("goods_value"), "goods_value");
        double fr  = asDouble(request.get("freight"), "freight");
        double ins = asDouble(request.get("insurance"), "insurance");
        int qty    = request.get("quantity") == null ? 0 : asInt(request.get("quantity"), "quantity");

        boolean noMeaningfulNumbers = gv == 0.0 && fr == 0.0 && ins == 0.0 && (qty == 0 || qty == 1);

        if (noMeaningfulText && noMeaningfulNumbers) {
            throw new IllegalArgumentException("No inputs detected.");
        }

        // 1) Inputs (normalize agreement up-front)
        String exporterCountryInput = (String) request.get("exporter");
        String importerCountryInput = (String) request.get("importer");

        String tradeAgreementInput  = normalizeCodeInput((String) request.get("agreement"));
        if (tradeAgreementInput == null || tradeAgreementInput.isBlank()) {
            throw new IllegalArgumentException("agreement is required (e.g., MFN, CPTPP).");
        }

        // 2) Resolve HS (prefers hsCode, else productDescription) and normalize
        String resolvedHsCode = resolveHsCodeFromRequest(request);
        if (resolvedHsCode == null || resolvedHsCode.isBlank()) {
            throw new IllegalArgumentException("Either hsCode or productDescription must be provided (no match found).");
        }

        // 3) Numbers + validation
        if (request.get("goods_value") == null) {
            throw new IllegalArgumentException("goods_value is required.");
        }
        double declaredGoodsValue    = gv;
        double declaredFreightCost   = fr;
        double declaredInsuranceCost = ins;
        int    declaredQuantity      = request.get("quantity") == null ? 1 : qty;

        if (declaredGoodsValue < 0 || declaredFreightCost < 0 || declaredInsuranceCost < 0 || declaredQuantity < 0) {
            throw new IllegalArgumentException("Numeric fields must not be negative. Check goods_value, freight, insurance, and quantity.");
        }

        // 4) Dates
        DateRange dateRange = parseDateRange(request);
        LocalDate startDate = dateRange.start();
        LocalDate endDate   = dateRange.end();

        // 5) Resolve countries to ISO codes
        String exporterIsoCode = resolveCountryCode(exporterCountryInput);
        String importerIsoCode = resolveCountryCode(importerCountryInput);
        if (exporterIsoCode == null || importerIsoCode == null) {
            throw new IllegalArgumentException("Invalid country input. Use ISO alpha-2 (e.g., \"SG\") or the exact country name.");
        }

        // 6) Duty/tax lookup
        Map<String, Object> tariffInfo = getTariffAndTax(
            exporterIsoCode, importerIsoCode, resolvedHsCode, tradeAgreementInput, startDate, endDate
        );

        double dutyRatePercent = ((Number) tariffInfo.get("rate_percent")).doubleValue();
        String customsValuationBasis = (String) tariffInfo.get("customs_basis");
        String importerTaxType = (String) tariffInfo.get("tax_type");
        double importerTaxRatePercent = ((Number) tariffInfo.get("tax_rate_percent")).doubleValue();

        // 7) Customs value (CIF vs FOB)
        double quantityAdjustedGoodsValue = declaredGoodsValue * declaredQuantity;
        double computedCustomsValue = (customsValuationBasis != null && customsValuationBasis.equalsIgnoreCase("CIF"))
            ? quantityAdjustedGoodsValue + declaredFreightCost + declaredInsuranceCost
            : quantityAdjustedGoodsValue;

        // 8) Duty and VAT/GST
        double computedDutyAmount = computedCustomsValue * (dutyRatePercent / 100.0);
        double computedTaxAmount  = (computedCustomsValue + computedDutyAmount) * (importerTaxRatePercent / 100.0);

        // 9) Success response
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("exporter_input", exporterCountryInput);
        response.put("importer_input", importerCountryInput);
        response.put("exporter_code", exporterIsoCode);
        response.put("importer_code", importerIsoCode);
        response.put("hs_code", resolvedHsCode);
        response.put("agreement", tradeAgreementInput);
        response.put("customs_basis", customsValuationBasis);
        response.put("rate_percent", dutyRatePercent);
        response.put("customs_value", computedCustomsValue);
        response.put("duty", computedDutyAmount);
        response.put("tax_type", importerTaxType);
        response.put("tax_rate_percent", importerTaxRatePercent);
        response.put("tax", computedTaxAmount);
        response.put("quantity", declaredQuantity);
        response.put("total_landed_cost", round2DP(computedCustomsValue + computedDutyAmount + computedTaxAmount));
        return response;
    }


}
