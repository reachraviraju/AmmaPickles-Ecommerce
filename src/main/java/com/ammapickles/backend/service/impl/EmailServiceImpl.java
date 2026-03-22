package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${app.mail.from}")
    private String fromEmail;

    // Common method (ALL emails go through this)
    private void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            String apiKey = System.getenv("BREVO_API_KEY");

            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);

            String json = "{"
                    + "\"sender\": {\"email\": \"" + fromEmail + "\", \"name\": \"Amma Pickles\"},"
                    + "\"to\": [{\"email\": \"" + toEmail + "\"}],"
                    + "\"subject\": \"" + subject + "\","
                    + "\"htmlContent\": \"" + htmlContent + "\""
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            log.info("Email sent to {} | Response: {}", toEmail, responseCode);

        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
        }
    }

 
    @Override
    public void sendOtpEmail(String toEmail, String username, String otp) {
        String html = "<p>Hello " + username + ",</p>"
                + "<p>Your OTP is: <b>" + otp + "</b></p>"
                + "<p>Valid for 10 minutes.</p>";

        sendEmail(toEmail, "Amma Pickles — Your OTP Code", html);
    }

   
    @Override
    public void sendWelcomeEmail(String toEmail, String username) {
        String html = "<p>Hello " + username + ",</p>"
                + "<p>Welcome to Amma Pickles! 🌶️</p>"
                + "<p>Happy Shopping!</p>";

        sendEmail(toEmail, "Welcome to Amma Pickles!", html);
    }

  
    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        String html = "<p>Hello " + username + ",</p>"
                + "<p>Click below to reset your password:</p>"
                + "<a href='" + resetLink + "'>Reset Password</a>";

        sendEmail(toEmail, "Reset Your Password", html);
    }

   
    @Override
    public void sendOrderConfirmationEmail(String toEmail, String username,
                                           Long orderId, BigDecimal grandTotal) {

        String html = "<p>Hello " + username + ",</p>"
                + "<p>Your order is confirmed!</p>"
                + "<p><b>Order ID:</b> #" + orderId + "</p>"
                + "<p><b>Amount:</b> ₹" + grandTotal + "</p>";

        sendEmail(toEmail, "Order #" + orderId + " Confirmed!", html);
    }
}