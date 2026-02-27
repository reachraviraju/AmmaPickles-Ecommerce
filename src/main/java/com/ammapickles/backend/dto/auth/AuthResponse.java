package com.ammapickles.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String email;
    private String username;
    private String role;       // "ROLE_ADMIN" or "ROLE_CUSTOMER"
    private String message;    // "Login successful" / "Registered successfully"
}