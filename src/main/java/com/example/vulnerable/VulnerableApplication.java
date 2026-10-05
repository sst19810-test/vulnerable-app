package com.example.vulnerable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class VulnerableApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(VulnerableApplication.class, args);
    }

    // VULNERABILITY: SQL Injection - user input is concatenated directly into the query
    @GetMapping("/api/users/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String username) {
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        return jdbcTemplate.queryForList(query);
    }
}
