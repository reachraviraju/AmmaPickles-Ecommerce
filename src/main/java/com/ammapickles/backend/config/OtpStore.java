package com.ammapickles.backend.config;

import com.ammapickles.backend.entity.OtpVerification;
import com.ammapickles.backend.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class OtpStore {

    private final OtpVerificationRepository otpRepository;
    private final Random random = new Random();
    private static final int OTP_VALID_MINUTES = 10;
    
    
    
    @Transactional
    public String generateOtp(String email) {

        String normalizedEmail = email.toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        OtpVerification existing = otpRepository.findByEmail(normalizedEmail).orElse(null);

        // 🔴 FIRST TIME USER
        if (existing == null) {
            String otp = String.format("%06d", random.nextInt(1_000_000));

            OtpVerification entry = OtpVerification.builder()
                    .email(normalizedEmail)
                    .otp(otp)
                    .expiresAt(now.plusMinutes(OTP_VALID_MINUTES))
                    .verified(false)
                    .requestCount(1) // 🔥 new field
                    .firstRequestTime(now)
                    .build();

            otpRepository.saveAndFlush(entry);
            return otp;
        }

        // 🔴 RESET WINDOW (after 10 mins)
        if (existing.getFirstRequestTime() == null ||
            existing.getFirstRequestTime().plusMinutes(10).isBefore(now)) {

            existing.setRequestCount(0);
            existing.setFirstRequestTime(now);
        }

        // 🔴 LIMIT: MAX 3 REQUESTS
        if (existing.getRequestCount() >= 3) {
            throw new RuntimeException("Too many OTP requests. Try again after 10 minutes.");
        }

        // 🔴 COOLDOWN: 60 sec
        long secondsSinceLastOtp = java.time.Duration.between(
                existing.getExpiresAt().minusMinutes(OTP_VALID_MINUTES),
                now
        ).getSeconds();

        if (secondsSinceLastOtp < 60) {
            throw new RuntimeException("Please wait 60 seconds before requesting OTP again.");
        }

        // 🔴 GENERATE NEW OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));

        existing.setOtp(otp);
        existing.setExpiresAt(now.plusMinutes(OTP_VALID_MINUTES));
        existing.setVerified(false);

        // 🔥 INCREMENT COUNT
        existing.setRequestCount(existing.getRequestCount() + 1);

        otpRepository.saveAndFlush(existing);

        return otp;
    }

    

    @Transactional
    public String validate(String email, String otp) {
        String normalizedEmail = email.toLowerCase();
        OtpVerification entry = otpRepository.findByEmail(normalizedEmail).orElse(null);

        if (entry == null) return "invalid";

        if (entry.isExpired()) {
            otpRepository.deleteByEmail(normalizedEmail);
            otpRepository.flush();
            return "expired";
        }

        if (!entry.getOtp().equals(otp.trim())) return "invalid";

        entry.setVerified(true);
        otpRepository.saveAndFlush(entry);
        return "ok";
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String email) {
        return otpRepository.findByEmail(email.toLowerCase())
                .map(e -> e.isVerified() && !e.isExpired())
                .orElse(false);
    }

    @Transactional
    public void clear(String email) {
        otpRepository.deleteByEmail(email.toLowerCase());
        otpRepository.flush();
    }
}