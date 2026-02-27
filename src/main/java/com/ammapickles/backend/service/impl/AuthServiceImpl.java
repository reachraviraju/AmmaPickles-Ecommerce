package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.auth.AuthResponse;
import com.ammapickles.backend.dto.auth.LoginRequest;
import com.ammapickles.backend.dto.auth.RegisterRequest;
import com.ammapickles.backend.dto.auth.ResetPasswordRequest;
import com.ammapickles.backend.entity.Role;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.RoleRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.security.JwtUtil;
import com.ammapickles.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // REGISTER 

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Fetch ROLE_CUSTOMER from DB
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_CUSTOMER not found in DB"));

        // Build and save user
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

        // Generate JWT and return
        String token = jwtUtil.generateToken(saved.getEmail());
        return new AuthResponse(token, saved.getEmail(), saved.getUsername(),
                "ROLE_CUSTOMER", "Registered successfully");
    }

    // LOGIN 

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        // AuthenticationManager validates email + password using BCrypt
        // Throws BadCredentialsException automatically if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load user for response data
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_CUSTOMER");

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("Login successful: {}", request.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getUsername(), role, "Login successful");
    }

    // RESET PASSWORD 

    @Override
    @Transactional
    public void resetPassword(String email, ResetPasswordRequest request) {
        log.info("Password reset for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Dirty checking handles save automatically
        log.info("Password reset successful: {}", email);
    }

    //  VERIFY EMAIL 

    @Override
    public void verifyEmail(String token) {
        // Future implementation
        log.info("Email verification token received: {}", token);
    }
}