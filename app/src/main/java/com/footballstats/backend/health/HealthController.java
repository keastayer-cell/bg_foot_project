package com.footballstats.backend.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> appHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "football-stats-app");
        return result;
    }

    @GetMapping("/db")
    public Map<String, Object> dbHealth() {
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        String user = jdbcTemplate.queryForObject("select current_user", String.class);
        String schema = jdbcTemplate.queryForObject("select current_schema()", String.class);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("database", database);
        result.put("user", user);
        result.put("schema", schema);
        return result;
    }
}
