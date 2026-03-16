package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.auth.AuthResponse;
import com.ammapickles.backend.dto.auth.LoginRequest;
import com.ammapickles.backend.dto.auth.RegisterRequest;
import com.ammapickles.backend.dto.auth.ResetPasswordRequest;
import com.ammapickles.backend.entity.PasswordResetToken;
import com.ammapickles.backend.entity.Role;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.PasswordResetTokenRepository;
import com.ammapickles.backend.repository.RoleRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.security.JwtUtil;
import com.ammapickles.backend.service.AuthService;
import com.ammapickles.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // REGISTER — enabled = true, OTP verified before this is called via web form

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_CUSTOMER not found in DB"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .roles(Set.of(customerRole))
                .build();

        User saved = userRepository.save(user);
        log.info("User registered with id: {}", saved.getId());

        emailService.sendWelcomeEmail(saved.getEmail(), saved.getUsername());

        return new AuthResponse(null, saved.getEmail(), saved.getUsername(),
                "ROLE_CUSTOMER", "Registered successfully.");
    }

    // LOGIN

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt with: {}", request.getIdentifier());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getIdentifier(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            throw new IllegalStateException(
                "Your account is disabled. Please contact support.");
        }

        User user;
        if (request.getIdentifier().contains("@")) {
            user = userRepository.findByEmail(request.getIdentifier())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        } else {
            user = userRepository.findByPhoneNumber(request.getIdentifier())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_CUSTOMER");

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("Login successful for: {}", request.getIdentifier());

        return new AuthResponse(token, user.getEmail(), user.getUsername(), role, "Login successful");
    }

    // FORGOT PASSWORD

    @Override
    @Transactional
    public void forgotPassword(String email) {
        log.info("Forgot password request for email: {}", email);

        userRepository.findByEmail(email).ifPresent(user -> {

            passwordResetTokenRepository.deleteByUserId(user.getId());

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            passwordResetTokenRepository.save(resetToken);

            String resetLink = baseUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);

            log.info("Password reset email sent for: {}", email);
        });
    }

    // RESET PASSWORD

    @Override
    @Transactional
    public void resetPasswordWithToken(String token, ResetPasswordRequest request) {
        log.info("Resetting password with token");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset link. Please request a new one."));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException(
                    "This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }
}