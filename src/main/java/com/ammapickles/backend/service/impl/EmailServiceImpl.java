package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // This is your verified sender address in Brevo (e.g. ammapickles.official@gmail.com)
    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Amma Pickles — Reset Your Password");
        message.setText(
            "Hello " + username + ",\n\n" +
            "Click the link below to reset your password (valid 1 hour):\n\n" +
            resetLink + "\n\n - Amma Pickles Team"
        );
        try {
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Welcome to Amma Pickles! 🌶️");
        message.setText(
            "Hello " + username + ",\n\n" +
            "Welcome to Amma Pickles! Your account is ready.\n\n" +
            "Happy Shopping!\n\n - Amma Pickles Team"
        );
        try {
            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendOtpEmail(String toEmail, String username, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Amma Pickles — Your Verification Code");
        message.setText(
            "Hello " + username + ",\n\n" +
            "Your verification code is:\n\n   " + otp + "\n\n" +
            "Valid for 10 minutes.\n\n - Amma Pickles Team"
        );
        try {
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, String username,
                                           Long orderId, BigDecimal grandTotal) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Amma Pickles — Order #" + orderId + " Confirmed! 🌶️");
        message.setText(
            "Hello " + username + ",\n\n" +
            "Your order has been placed successfully!\n\n" +
            "Order ID  : #" + orderId + "\n" +
            "Amount    : ₹" + grandTotal + "\n" +
            "Payment   : Cash on Delivery\n" +
            "Delivery  : 5–7 working days\n\n" +
            "We'll deliver your pickles soon!\n\n - Amma Pickles Team"
        );
        try {
            mailSender.send(message);
            log.info("Order confirmation email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }
}