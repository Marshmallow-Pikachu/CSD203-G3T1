package com.ratewise.restcontrollers;

import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to get HS Codes information
 */
@RestController
@RequestMapping("/api/v1/hscodes")
public class HSCodeController {
    private final JdbcTemplate jdbc;
    
    public HSCodeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
    * GET /api/hscodes
    * Returns a list of all hs codes with their codes and description.
    */
    @GetMapping
    public List<Map<String, Object>> listHSCodes() {
        return jdbc.queryForList(
            "SELECT hs_code, description FROM hs_codes ORDER BY description"
        );
    }
}
