package com.ratewise.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * TariffService
 * Business logic for retrieving tariff data for the dashboard
 * and performing landed cost calculations.
 */
@Service
public class TariffService {

  private final JdbcTemplate jdbc;

  public TariffService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Resolve a country input into a standard ISO country code.
   * Accepts either full country names ("Singapore") or codes ("SG").
   * Returns the code (e.g. "SG").
   */
  public String resolveCountryCode(String input) {
    String sql = """
            SELECT country_code
            FROM countries
            WHERE LOWER(country_name) = LOWER(?) OR UPPER(country_code) = UPPER(?)
            LIMIT 1
        """;

    return jdbc.queryForObject(sql, String.class, input, input);
  }

  /**
   * Get tariff info for a specific lane (exporter → importer),
   * HS code, and agreement.
   *
   * Example use:
   * - Exporter = "SG"
   * - Importer = "US"
   * - HS code = "010121"
   * - Agreement = "MFN"
   *
   * Returns duty rate %, customs basis (CIF/FOB), agreement info, HS code details,
   * and exporter/importer country details.
   */
  public Map<String, Object> getTariffInfo(String exporter, String importer, String hsCode, String agreement) {
    String sql = """
            SELECT tariff_rates.rate_percent,
                   importer_country.customs_basis,
                   agreements.agreement_code,
                   agreements.agreement_name,
                   hs_codes.hs_code,
                   hs_codes.description AS hs_description,
                   exporter_country.country_code AS exporter_code,
                   exporter_country.country_name AS exporter_name,
                   importer_country.country_code AS importer_code,
                   importer_country.country_name AS importer_name
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

    return jdbc.queryForMap(sql, exporter, importer, hsCode, agreement);
  }

  /**
   * Optional filters: importer, exporter, agreement.
   *
   * Example:
   * - GET /api/tariffs/list?importer=US
   * - GET /api/tariffs/list?exporter=SG&agreement=CPTPP
   *
   * Returns a list of tariff rows (HS code, description, exporter/importer, agreement, duty rate, basis).
   */
  public List<Map<String, Object>> listTariffs(String importer, String exporter, String agreement) {
    StringBuilder sql = new StringBuilder("""
            SELECT hs_codes.hs_code,
                   hs_codes.description AS hs_description,
                   exporter_country.country_code AS exporter_code,
                   exporter_country.country_name AS exporter_name,
                   importer_country.country_code AS importer_code,
                   importer_country.country_name AS importer_name,
                   agreements.agreement_code,
                   agreements.agreement_name,
                   tariff_rates.rate_percent,
                   importer_country.customs_basis
            FROM tariff_rates
            JOIN hs_codes
              ON hs_codes.id = tariff_rates.hs_code_id
            JOIN countries AS exporter_country
              ON exporter_country.id = tariff_rates.exporter_id
            JOIN countries AS importer_country
              ON importer_country.id = tariff_rates.importer_id
            JOIN agreements
              ON agreements.id = tariff_rates.agreement_id
            WHERE (tariff_rates.valid_to IS NULL OR tariff_rates.valid_to >= CURRENT_DATE)
        """);

    List<Object> params = new ArrayList<>();

    if (importer != null && !importer.isEmpty()) {
      sql.append(" AND importer_country.country_code = ?");
      params.add(importer);
    }

    if (exporter != null && !exporter.isEmpty()) {
      sql.append(" AND exporter_country.country_code = ?");
      params.add(exporter);
    }

    if (agreement != null && !agreement.isEmpty()) {
      sql.append(" AND agreements.agreement_code = ?");
      params.add(agreement);
    }

    sql.append(" ORDER BY hs_codes.hs_code ASC, agreements.agreement_code ASC");

    return jdbc.queryForList(sql.toString(), params.toArray());
  }

  // Query for Tariff Table
  public List<Map<String, Object>> listTariffsTable() {
    String sql = """
            SELECT exporter.country_code AS exporter_code,
                   exporter.country_name AS exporter_name,
                   importer.country_code AS importer_code,
                   importer.country_name AS importer_name,
                   importer.customs_basis AS importer_customs,
                   tax_rules.tax_type AS importer_tax,
                   agreements.agreement_code,
                   agreements.agreement_name,
                   hs_codes.hs_code,
                   hs_codes.description AS hs_description,
                   tariff_rates.rate_percent,
                   tariff_rates.valid_from
            FROM tariff_rates
            JOIN countries AS exporter
              ON exporter.id = tariff_rates.exporter_id
            JOIN countries AS importer
              ON importer.id = tariff_rates.importer_id
            JOIN hs_codes
              ON hs_codes.id = tariff_rates.hs_code_id
            JOIN agreements
              ON agreements.id = tariff_rates.agreement_id
            JOIN tax_rules
              ON tax_rules.country_id = importer.id
            WHERE tariff_rates.id < 3000
            ORDER BY exporter ASC, importer ASC, hs_code ASC
        """;

    return jdbc.queryForList(sql);
  }
}