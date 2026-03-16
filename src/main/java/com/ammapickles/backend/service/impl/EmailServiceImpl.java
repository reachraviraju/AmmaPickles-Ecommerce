package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Amma Pickles — Reset Your Password");
        message.setText("Hello " + username + ",\n\nClick the link below to reset your password (valid 1 hour):\n\n" + resetLink + "\n\n - Amma Pickles Team");
        mailSender.send(message);
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Welcome to Amma Pickles! 🌶️");
        message.setText("Hello " + username + ",\n\nWelcome to Amma Pickles! Your account is ready.\n\nHappy Shopping!\n - Amma Pickles Team");
        mailSender.send(message);
    }

    @Override
    public void sendOtpEmail(String toEmail, String username, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Amma Pickles <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("Amma Pickles — Your Verification Code");
        message.setText("Hello " + username + ",\n\nYour verification code is:\n\n   " + otp + "\n\nValid for 10 minutes.\n\n - Amma Pickles Team");
        mailSender.send(message);
    }
}