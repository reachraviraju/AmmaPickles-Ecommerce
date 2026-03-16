package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.auth.AuthResponse;
import com.ammapickles.backend.dto.auth.ForgotPasswordRequest;
import com.ammapickles.backend.dto.auth.LoginRequest;
import com.ammapickles.backend.dto.auth.RegisterRequest;
import com.ammapickles.backend.dto.auth.ResetPasswordRequest;
import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.repository.UserRepository;
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
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registered successfully.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Login request for: {}", request.getIdentifier());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // Used by register form for live email availability check
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        log.info("Checking email availability: {}", email);
        boolean available = !userRepository.existsByEmail(email);
        String message = available ? "Email is available" : "Email already registered";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for: {}", request.getEmail());
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If this email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPasswordWithToken(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordWithToken(token, request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }
}