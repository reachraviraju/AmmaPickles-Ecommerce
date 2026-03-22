package com.ammapickles.backend.controller;

import com.ammapickles.backend.config.OtpStore;
import com.ammapickles.backend.dto.auth.ResetPasswordRequest;
import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.entity.Role;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.RoleRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AuthService;
import com.ammapickles.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final EmailService emailService;
    private final OtpStore otpStore;

    // LOGIN

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // REGISTER — show form

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // REGISTER — send OTP to email (called via fetch from JS)

    @PostMapping("/register/send-otp")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestParam String email,
                                                       @RequestParam String name) {
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email already registered. Please login instead."));
        }
        
        try {
                String otp = otpStore.generateOtp(email);

                   boolean sent = emailService.sendOtpEmail(email, name, otp);

                   if (!sent) {
                              return ResponseEntity.status(500).body(ApiResponse.error("Failed to send OTP"));
                              }
     
                     return ResponseEntity.ok(ApiResponse.success("OTP sent"));

            } 
        
        catch (RuntimeException e) {
           
        	return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
        
        
    }

    // REGISTER — verify OTP (called via fetch from JS)

    @PostMapping("/register/verify-otp")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestParam String email,
                                                         @RequestParam String otp) {
        String result = otpStore.validate(email, otp);
        return switch (result) {
            case "ok"      -> ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
            case "expired" -> ResponseEntity.badRequest().body(ApiResponse.error("OTP has expired. Please request a new one."));
            default        -> ResponseEntity.badRequest().body(ApiResponse.error("Invalid OTP. Please check and try again."));
        };
    }

    // REGISTER — final form submit (only reaches here after OTP verified)

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String phone,
                           Model model) {

        // Double-check email not taken
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email already registered! Please login.");
            return "register";
        }

        // Double-check OTP was verified
        if (!otpStore.isVerified(email)) {
            model.addAttribute("error", "Email not verified. Please verify your email with OTP first.");
            return "register";
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("ROLE_CUSTOMER not found in DB."));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        // enabled = true — email already verified via OTP
        User user = User.builder()
                .username(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phoneNumber((phone != null && !phone.isBlank()) ? phone : null)
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(user);
        otpStore.clear(email);

        // Send welcome email
        emailService.sendWelcomeEmail(email, name);

        return "redirect:/login?verified=true";
    }

    // FORGOT PASSWORD (Step 1)

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submitForgotPassword(@RequestParam String email, Model model) {
        authService.forgotPassword(email);
        model.addAttribute("success",
                "If this email is registered, a reset link has been sent. Check your inbox.");
        return "forgot-password";
    }

    // RESET PASSWORD (Step 2)

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String submitResetPassword(@RequestParam String token,
                                      @RequestParam String newPassword,
                                      Model model) {
        try {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setNewPassword(newPassword);
            authService.resetPasswordWithToken(token, request);
            return "redirect:/login?passwordReset=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }

}