package com.ratewise.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.ratewise.dto.TariffAdminRequest;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * TariffAdminService — isolated admin CRUD logic for tariff_rates.
 * Only admin accounts should call these endpoints (controller is mounted under /api/v1/admin/**).
 */
@Service
public class TariffAdminService {

    private final JdbcTemplate jdbc;

    public TariffAdminService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Create a tariff_rates row. Returns the inserted row (joined view).
     * Note: uses subqueries to resolve foreign keys by code.
     */
    public Map<String, Object> createTariff(TariffAdminRequest req) {
        // --- 1. Parse dates safely ---
        java.sql.Date validFromDate = null;
        java.sql.Date validToDate = null;
        try {
            if (req.validFrom != null && !req.validFrom.isBlank()) {
                validFromDate = java.sql.Date.valueOf(java.time.LocalDate.parse(req.validFrom));
            } else {
                throw new IllegalArgumentException("validFrom is required and must be ISO yyyy-MM-dd");
            }
            if (req.validTo != null && !req.validTo.isBlank()) {
                validToDate = java.sql.Date.valueOf(java.time.LocalDate.parse(req.validTo));
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid date format. Use ISO yyyy-MM-dd.", ex);
        }

        // --- 2. Execute insert with RETURNING id ---
        String sql = """
            WITH ids AS (
                SELECT
                (SELECT id FROM countries  WHERE country_code = ? LIMIT 1) AS exporter_id,
                (SELECT id FROM countries  WHERE country_code = ? LIMIT 1) AS importer_id,
                (SELECT id FROM hs_codes   WHERE hs_code = ? LIMIT 1)      AS hs_code_id,
                (SELECT id FROM agreements WHERE agreement_code = ? LIMIT 1) AS agreement_id
            )
            INSERT INTO tariff_rates (
                exporter_id, importer_id, hs_code_id, agreement_id,
                rate_percent, valid_from, valid_to, source_ref
            )
            SELECT
                i.exporter_id, i.importer_id, i.hs_code_id, i.agreement_id,
                ?, ?, ?, 'API: admin create'
            FROM ids i
            ON CONFLICT ON CONSTRAINT uniq_tariff_version
            DO UPDATE SET
                rate_percent = EXCLUDED.rate_percent,
                valid_to     = EXCLUDED.valid_to,
                source_ref   = EXCLUDED.source_ref
            RETURNING id
            """;

        Long newId = jdbc.queryForObject(
            sql,
            Long.class,
            req.exporterCode,  // ?
            req.importerCode,  // ?
            req.hsCode,        // ?
            req.agreementCode, // ?
            req.ratePercent,   // ?
            validFromDate,     // ?
            validToDate        // ?
        );

        if (newId == null) {
            throw new IllegalStateException("Failed to insert or update tariff — no ID returned.");
        }

        // --- 3. Return the inserted/updated tariff as a map ---
        return getById(newId.intValue());
    }


    /**
     * Update tariff row by id; returns updated row.
     */
    public Map<String, Object> updateTariff(int id, TariffAdminRequest req) {
        // 1) Parse dates as java.sql.Date (NOT strings)
        java.sql.Date vf;
        java.sql.Date vt = null;
        try {
            if (req.validFrom == null || req.validFrom.isBlank()) {
                throw new IllegalArgumentException("validFrom is required (ISO yyyy-MM-dd)");
            }
            vf = java.sql.Date.valueOf(java.time.LocalDate.parse(req.validFrom));
            if (req.validTo != null && !req.validTo.isBlank()) {
                vt = java.sql.Date.valueOf(java.time.LocalDate.parse(req.validTo));
            }
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Date fields must be ISO yyyy-MM-dd", ex
            );
        }

        // 2) Ensure the row exists first (give 404 if not)
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM tariff_rates WHERE id = ?",
                                            Integer.class, id);
        if (exists == null || exists == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Tariff ID " + id + " not found"
            );
        }

        // 3) Update using codes -> ids, and bind DATE params (avoids VARCHAR vs DATE error)
        final String sql = """
            WITH ids AS (
            SELECT
                (SELECT id FROM countries  WHERE country_code = ? LIMIT 1) AS exporter_id,
                (SELECT id FROM countries  WHERE country_code = ? LIMIT 1) AS importer_id,
                (SELECT id FROM hs_codes   WHERE hs_code      = ? LIMIT 1) AS hs_code_id,
                (SELECT id FROM agreements WHERE agreement_code = ? LIMIT 1) AS agreement_id
            )
            UPDATE tariff_rates t
            SET exporter_id = i.exporter_id,
                importer_id = i.importer_id,
                hs_code_id  = i.hs_code_id,
                agreement_id = i.agreement_id,
                rate_percent = ?,
                valid_from   = ?,   -- <-- DATE param
                valid_to     = ?,   -- <-- DATE or NULL
                source_ref   = 'API: admin update'
            FROM ids i
            WHERE t.id = ?
            RETURNING t.id
            """;

        try {
            Long updatedId = jdbc.queryForObject(
                sql,
                Long.class,
                // ids CTE params:
                req.exporterCode,
                req.importerCode,
                req.hsCode,
                req.agreementCode,
                // SET params:
                req.ratePercent,
                vf,
                vt,
                // WHERE id
                id
            );
            if (updatedId == null) {
                throw new IllegalStateException("Update returned no id");
            }
            return getById(updatedId.intValue());
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // Likely hit uniq_tariff_version after changing keys/valid_from to an existing version
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Version already exists for the given (HS, exporter, importer, agreement, validFrom)", dup
            );
        }
    }


    /**
     * Delete tariff row by id.
     */
    public void deleteTariff(int id) {
        // Optional: verify the tariff exists before deletion (so we can return 404)
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM tariff_rates WHERE id = ?", Integer.class, id);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff ID " + id + " not found");
        }

        try {
            int rows = jdbc.update("DELETE FROM tariff_rates WHERE id = ?", id);
            if (rows == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff ID " + id + " not found");
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff ID " + id + " not found", ex);
        }
    }

    /**
     * Fetch a tariff row (joined payload) by tariff_rates.id
     */
    public Map<String, Object> getById(Integer id) {
        String sql = """
            SELECT tr.id,
                   exporter.country_code AS exporter_code,
                   exporter.country_name AS exporter_name,
                   importer.country_code AS importer_code,
                   importer.country_name AS importer_name,
                   importer.customs_basis AS importer_customs,
                   tax_rules.tax_type AS importer_tax,
                   agreements.agreement_code,
                   agreements.agreement_name,
                   hs_codes.hs_code,
                   hs_codes.description AS hs_description,
                   tr.rate_percent,
                   tr.valid_from,
                   tr.valid_to
            FROM tariff_rates tr
            JOIN countries AS exporter ON exporter.id = tr.exporter_id
            JOIN countries AS importer ON importer.id = tr.importer_id
            JOIN hs_codes ON hs_codes.id = tr.hs_code_id
            JOIN agreements ON agreements.id = tr.agreement_id
            LEFT JOIN tax_rules ON tax_rules.country_id = importer.id
            WHERE tr.id = ?
            LIMIT 1
            """;

        try {
            return jdbc.queryForMap(sql, id);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Tariff row not found: " + id);
        }
    }

    /**
     * Optional: list all admin-manageable tariff rows (delegates to TariffService.listTariffsTable)
     */
    public List<Map<String, Object>> listAll() {
        // reuse existing TariffService logic if desired by injecting it here instead.
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
                   tariff_rates.valid_from,
                   tariff_rates.valid_to,
                   tariff_rates.id
            FROM tariff_rates
            JOIN countries AS exporter ON exporter.id = tariff_rates.exporter_id
            JOIN countries AS importer ON importer.id = tariff_rates.importer_id
            JOIN hs_codes ON hs_codes.id = tariff_rates.hs_code_id
            JOIN agreements ON agreements.id = tariff_rates.agreement_id
            LEFT JOIN tax_rules ON tax_rules.country_id = importer.id
            ORDER BY exporter ASC, importer ASC, hs_codes.hs_code ASC
            """;
        return jdbc.queryForList(sql);
    }
}