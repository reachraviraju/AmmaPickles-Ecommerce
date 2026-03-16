package com.ammapickles.backend.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String username, String resetLink);

    void sendWelcomeEmail(String toEmail, String username);

    // Send 6-digit OTP for inline register verification
    void sendOtpEmail(String toEmail, String username, String otp);
}