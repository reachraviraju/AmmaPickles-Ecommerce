package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.auth.AuthResponse;
import com.ammapickles.backend.dto.auth.LoginRequest;
import com.ammapickles.backend.dto.auth.RegisterRequest;
import com.ammapickles.backend.dto.auth.ResetPasswordRequest;
import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Register request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registered successfully", response));
    }

   
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

   
    @PutMapping("/reset-password/{email}")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String email,
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

   
    @GetMapping("/verify/{token}")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @PathVariable String token) {

        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }
}