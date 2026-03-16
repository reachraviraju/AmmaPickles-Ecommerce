package com.ammapickles.backend.config;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store. No new DB table needed.
 * Stores email ->  {otp, expiresAt, verified}
 */
@Component
public class OtpStore {

    private static final int OTP_VALID_MINUTES = 10;

    private record OtpEntry(String otp, LocalDateTime expiresAt, boolean verified) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** Generate a new 6-digit OTP for the email and return it. */
    public String generateOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(otp, LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES), false));
        return otp;
    }

    /**
     * Validate OTP for email.
     * Returns: "ok" | "invalid" | "expired"
     */
    public String validate(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return "invalid";
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return "expired";
        }
        if (!entry.otp().equals(otp.trim())) return "invalid";
        // Mark as verified
        store.put(email.toLowerCase(), new OtpEntry(entry.otp(), entry.expiresAt(), true));
        return "ok";
    }

    /** Check if OTP was already verified for this email (before final register submit). */
    public boolean isVerified(String email) {
        OtpEntry entry = store.get(email.toLowerCase());
        return entry != null && entry.verified() && LocalDateTime.now().isBefore(entry.expiresAt());
    }

    /** Remove after successful registration. */
    public void clear(String email) {
        store.remove(email.toLowerCase());
    }
}