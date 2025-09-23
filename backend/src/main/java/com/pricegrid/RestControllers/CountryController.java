package com.pricegrid.RestControllers;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Controller for querying country information.
 * Now Includes CIF/FOB customs basis that we added recently.
 */

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

    private final JdbcTemplate jdbc;
    
    public CountryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


     /*
    * GET /api/countries
    * Returns a list of all countries with their codes, names, and customs basis.
    */
    @GetMapping
    public List<Map<String, Object>> listCountries() {
        return jdbc.queryForList(
            "SELECT country_code, country_name, customs_basis FROM countries ORDER BY country_code"
        );
    }

    /**
     * GET /api/countries/{code}
     * Returns details for one country (by ISO alpha-2 code, e.g. 'SG').
     */
    @GetMapping("/{code}")
    public Map<String, Object> getCountry(@PathVariable String code) {
        return jdbc.queryForMap(
            "SELECT country_code, country_name, customs_basis FROM countries WHERE country_code = ?",
            code
        );
    }
}
