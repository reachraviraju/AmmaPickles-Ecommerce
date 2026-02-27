package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.user.UpdateUserRequest;
import com.ammapickles.backend.dto.user.UserResponse;

// UserService handles ONLY user profile operations
// Login & register moved to AuthService
public interface UserService {

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}