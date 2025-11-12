package com.ratewise.restcontrollers;

import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to get agreement information
 */
@RestController
@RequestMapping("/api/v1/agreements")
public class AgreementController {
    private final JdbcTemplate jdbc;
    
    public AgreementController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
    * GET /api/agreements
    * Returns a list of all agreements with their codes and names.
    */
    @GetMapping
    public List<Map<String, Object>> listAgreements() {
        return jdbc.queryForList(
            "SELECT agreement_code, agreement_name FROM agreements ORDER BY agreement_code"
        );
    }
}
