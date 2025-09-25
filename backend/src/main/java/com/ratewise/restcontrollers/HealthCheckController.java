package com.ratewise.restcontrollers;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Controller for basic health checks.
 * Used to confirm the backend and database are reachable.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {
    private final JdbcTemplate jdbc;

    public HealthCheckController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("now", jdbc.queryForObject("SELECT NOW()", Object.class));
        r.put("version", jdbc.queryForObject("SELECT version()", String.class));
        return r;
    }
}
