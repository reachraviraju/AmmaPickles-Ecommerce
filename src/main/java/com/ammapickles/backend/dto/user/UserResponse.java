package com.ammapickles.backend.dto.user;

import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;         // "ROLE_ADMIN" or "ROLE_CUSTOMER"
    
}