package com.ammapickles.backend.service;

import java.math.BigDecimal;

public interface EmailService {
	
	
    boolean sendOtpEmail(String toEmail, String username, String otp);

    void sendWelcomeEmail(String toEmail, String username);

    void sendPasswordResetEmail(String toEmail, String username, String resetLink);

    void sendOrderConfirmationEmail(String toEmail, String username, Long orderId, BigDecimal grandTotal);

   
    
    
}