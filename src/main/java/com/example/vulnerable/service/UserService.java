package com.example.vulnerable.service;

import com.example.vulnerable.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    @Autowired
    JdbcTemplate jdbcTemplate = new JdbcTemplate() ;
    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public UserService() {
        // Initialize with mock data
        users.put(1L, new User(1L, "admin", "admin123", "admin@example.com", "ADMIN", "123-45-6789", "4532-1234-5678-9012"));
        users.put(2L, new User(2L, "user1", "password", "user1@example.com", "USER", "987-65-4321", "4532-9876-5432-1098"));
        idCounter.set(3L);
    }

    // VULNERABILITY: SQL Injection (simulated)
    public List<User> searchUsersByUsername(String username) {
        // Simulating SQL injection vulnerability
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        System.out.println("Executing query: " + query);  // VULNERABILITY: Logging sensitive data
        jdbcTemplate.execute(query);
        List<User> result = new ArrayList<>();
        for (User user : users.values()) {
            if (user.getUsername().contains(username)) {
                result.add(user);
            }
        }
        return result;
    }

    public User createUser(User user) {
        if (user.getId() == null) {
            user.setId(idCounter.getAndIncrement());
        }
        // VULNERABILITY: No password hashing
        users.put(user.getId(), user);
        System.out.println("Created user with password: " + user.getPassword());  // VULNERABILITY: Logging passwords
        return user;
    }

    public User getUserById(Long id) {
        // VULNERABILITY: No authorization check
        return users.get(id);
    }

    public void deleteUser(Long id) {
        users.remove(id);
    }

    public List<User> getAllUsers() {
        // VULNERABILITY: Exposing all user data including sensitive fields
        return new ArrayList<>(users.values());
    }

    // VULNERABILITY: Hardcoded credentials
    public boolean isAdmin(String username, String password) {
        return "admin".equals(username) && "admin123".equals(password);
    }
}
