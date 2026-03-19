package com.example.vulnerable.controller;

import com.example.vulnerable.model.User;
import com.example.vulnerable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // VULNERABILITY: SQL Injection
    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String username) {
        return userService.searchUsersByUsername(username);
    }

    // VULNERABILITY: Mass Assignment
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // VULNERABILITY: Insecure Direct Object Reference (IDOR)
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // VULNERABILITY: No authorization check
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    // VULNERABILITY: Information Disclosure
    @GetMapping("/debug")
    public String debugInfo(HttpServletRequest request) {
        StringBuilder debug = new StringBuilder();
        debug.append("Session ID: ").append(request.getSession().getId()).append("\n");
        debug.append("User Agent: ").append(request.getHeader("User-Agent")).append("\n");
        debug.append("All Users: ").append(userService.getAllUsers().toString());
        return debug.toString();
    }

    // VULNERABILITY: XSS - Reflected
    @GetMapping("/welcome")
    public String welcome(@RequestParam String name) {
        return "<h1>Welcome " + name + "</h1>";  // No sanitization
    }
}
