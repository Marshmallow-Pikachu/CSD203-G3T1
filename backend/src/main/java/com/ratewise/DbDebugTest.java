package com.ratewise;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.*;

@RestController
@RequestMapping("/db")
public class DbDebugTest {
    private final JdbcTemplate jdbc;

    public DbDebugTest(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 1) Smoke test: proves we can run a query
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("now", jdbc.queryForObject("SELECT NOW()", Object.class));
        r.put("version", jdbc.queryForObject("SELECT version()", String.class));
        return r;
    }

    // 2) List public tables
    @GetMapping("/tables")
    public List<String> tables() {
        return jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name",
            String.class
        );
    }

    // 3) Dump first N rows of a table (default 10)
    //    Example: /db/rows?table=countries&limit=5
    @GetMapping(value = "/rows", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> rows(
            @RequestParam String table,
            @RequestParam(defaultValue = "10") int limit) {

        // very basic guardrails: allow only simple identifiers and a sane limit
        if (!table.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name.");
        }
        limit = Math.max(1, Math.min(limit, 100));

        String sql = "SELECT * FROM " + table + " LIMIT " + limit;
        return jdbc.queryForList(sql);
    }
}
