package com.ammapickles.backend.service;

import java.math.BigDecimal;

public interface EmailService {
	
	
	 boolean sendOtpEmail(String toEmail, String username, String otp);

	    boolean sendWelcomeEmail(String toEmail, String username);

	    boolean sendPasswordResetEmail(String toEmail, String username, String resetLink);

	    boolean sendOrderConfirmationEmail(String toEmail, String username,  Long orderId, BigDecimal grandTotal);

   
    
    
}