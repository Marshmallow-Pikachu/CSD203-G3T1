package com.ratewise.restcontrollers;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    
    /**
     * GET /api/countries
     * Returns all countries for dropdown list
     */
    @GetMapping
    public List<Map<String, Object>> getAllCountries() {
        return jdbc.queryForList(
            "SELECT country_code, country_name FROM countries ORDER BY country_code"
        );
    }
}
