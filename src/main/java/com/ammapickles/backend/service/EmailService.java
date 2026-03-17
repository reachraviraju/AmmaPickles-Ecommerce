package com.ammapickles.backend.service;

import java.math.BigDecimal;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String username, String resetLink);

    void sendWelcomeEmail(String toEmail, String username);

    // Send 6-digit OTP for inline register verification
    void sendOtpEmail(String toEmail, String username, String otp);
    
    void sendOrderConfirmationEmail(String toEmail, String username, Long orderId, BigDecimal grandTotal);
}