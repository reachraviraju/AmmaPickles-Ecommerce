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

    // COMMON EMAIL  ---- > >  ALL EMAILS USE THIS)
    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            String apiKey = System.getenv("BREVO_API_KEY");

            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);

            // TIMEOUT FIX 
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

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

            // FAILURE HANDLING
            if (responseCode != 201) {
                log.error("Email failed for {} | Response: {}", toEmail, responseCode);
                return false;
            }

            log.info("Email sent to {} | Response: {}", toEmail, responseCode);
            return true;

        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
            return false;
        }
    }

  
    @Override
    public boolean sendOtpEmail(String toEmail, String username, String otp) {
        String html = "<h2>Amma Pickles</h2>"
                + "<p>Hello " + username + ",</p>"
                + "<p>Your OTP is:</p>"
                + "<h1 style='letter-spacing:5px;'>" + otp + "</h1>"
                + "<p>Valid for 10 minutes.</p>";

        return sendEmail(toEmail, "Amma Pickles — Your OTP Code", html);
    }

  
    @Override
    public boolean sendWelcomeEmail(String toEmail, String username) {
        String html = "<h2>Welcome to Amma Pickles 🌶️</h2>"
                + "<p>Hello " + username + ",</p>"
                + "<p>Your account is ready.</p>"
                + "<p>Happy Shopping!</p>";

        return sendEmail(toEmail, "Welcome to Amma Pickles!", html);
    }

   
    @Override
    public boolean sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        String html = "<h2>Password Reset</h2>"
                + "<p>Hello " + username + ",</p>"
                + "<p>Click below to reset your password:</p>"
                + "<a href='" + resetLink + "'>Reset Password</a>";

        return sendEmail(toEmail, "Reset Your Password", html);
    }

    @Override
    public boolean sendOrderConfirmationEmail(String toEmail, String username,
                                              Long orderId, BigDecimal grandTotal) {

        String html = "<h2>Order Confirmed 🎉</h2>"
                + "<p>Hello " + username + ",</p>"
                + "<p>Your order has been placed successfully!</p>"
                + "<p><b>Order ID:</b> #" + orderId + "</p>"
                + "<p><b>Amount:</b> ₹" + grandTotal + "</p>";

        return sendEmail(toEmail, "Order #" + orderId + " Confirmed!", html);
    }
}