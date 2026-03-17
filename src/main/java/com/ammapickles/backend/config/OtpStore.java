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
        // delete old if exists
        otpRepository.deleteByEmail(email.toLowerCase());
        OtpVerification entry = OtpVerification.builder()
                .email(email.toLowerCase())
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES))
                .verified(false)
                .build();
        otpRepository.save(entry);
        return otp;
    }

    @Transactional
    public String validate(String email, String otp) {
        OtpVerification entry = otpRepository.findByEmail(email.toLowerCase())
                .orElse(null);
        if (entry == null) return "invalid";
        if (entry.isExpired()) {
            otpRepository.deleteByEmail(email.toLowerCase());
            return "expired";
        }
        if (!entry.getOtp().equals(otp.trim())) return "invalid";
        entry.setVerified(true);
        otpRepository.save(entry);
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
    }
}