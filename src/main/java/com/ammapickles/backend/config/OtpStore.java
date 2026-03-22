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
        String otp = String.format("%06d", random.nextInt(1_000_000));
        String normalizedEmail = email.toLowerCase();

        // Check if existing OTP record exists
        OtpVerification existing = otpRepository.findByEmail(normalizedEmail).orElse(null);

        if (existing != null) {
            // UPDATE existing record instead of delete + insert
            existing.setOtp(otp);
            existing.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
            existing.setVerified(false);
            otpRepository.saveAndFlush(existing);
        } else {
            // INSERT new record
            OtpVerification entry = OtpVerification.builder()
                    .email(normalizedEmail)
                    .otp(otp)
                    .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES))
                    .verified(false)
                    .build();
            otpRepository.saveAndFlush(entry);
        }

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