package com.example.vulnerable.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;  // VULNERABILITY: Storing password in plain text
    private String email;
    private String role;
    private String ssn;  // VULNERABILITY: Sensitive data
    private String creditCard;  // VULNERABILITY: PCI data
}
