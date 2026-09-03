package com.ammapickles.backend.config;

import com.ammapickles.backend.entity.OtpVerification;
import com.ammapickles.backend.repository.OtpVerificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpStoreTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @InjectMocks
    private OtpStore otpStore;

    private final String testEmail = "customer@example.com";

    @Test
    @DisplayName("Generate OTP: First time user creates new entry")
    void testGenerateOtp_FirstTimeUser() {
        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        String otp = otpStore.generateOtp(testEmail);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(otpRepository).saveAndFlush(any(OtpVerification.class));
    }

    @Test
    @DisplayName("Generate OTP: Rate limit triggers when request count >= 3")
    void testGenerateOtp_RateLimitExceeded() {
        LocalDateTime now = LocalDateTime.now();
        OtpVerification existing = OtpVerification.builder()
                .email(testEmail)
                .otp("111111")
                .firstRequestTime(now.minusMinutes(2))
                .expiresAt(now.plusMinutes(8))
                .requestCount(3)
                .verified(false)
                .build();

        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> otpStore.generateOtp(testEmail));
        assertTrue(exception.getMessage().contains("Too many OTP requests"));
    }

    @Test
    @DisplayName("Validate OTP: Matching code returns 'ok' and sets verified")
    void testValidateOtp_Success() {
        LocalDateTime now = LocalDateTime.now();
        OtpVerification existing = OtpVerification.builder()
                .email(testEmail)
                .otp("654321")
                .expiresAt(now.plusMinutes(5))
                .verified(false)
                .build();

        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.of(existing));

        String result = otpStore.validate(testEmail, "654321");

        assertEquals("ok", result);
        assertTrue(existing.isVerified());
        verify(otpRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("Validate OTP: Wrong code returns 'invalid'")
    void testValidateOtp_Invalid() {
        LocalDateTime now = LocalDateTime.now();
        OtpVerification existing = OtpVerification.builder()
                .email(testEmail)
                .otp("654321")
                .expiresAt(now.plusMinutes(5))
                .verified(false)
                .build();

        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.of(existing));

        String result = otpStore.validate(testEmail, "000000");

        assertEquals("invalid", result);
        assertFalse(existing.isVerified());
    }

    @Test
    @DisplayName("Validate OTP: Expired code returns 'expired' and deletes entry")
    void testValidateOtp_Expired() {
        LocalDateTime now = LocalDateTime.now();
        OtpVerification existing = OtpVerification.builder()
                .email(testEmail)
                .otp("654321")
                .expiresAt(now.minusMinutes(1)) // expired
                .verified(false)
                .build();

        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.of(existing));

        String result = otpStore.validate(testEmail, "654321");

        assertEquals("expired", result);
        verify(otpRepository).deleteByEmail(testEmail);
    }

    @Test
    @DisplayName("isVerified: Returns true only when verified and not expired")
    void testIsVerified() {
        LocalDateTime now = LocalDateTime.now();
        OtpVerification verified = OtpVerification.builder()
                .email(testEmail)
                .expiresAt(now.plusMinutes(5))
                .verified(true)
                .build();

        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.of(verified));
        assertTrue(otpStore.isVerified(testEmail));

        OtpVerification unverified = OtpVerification.builder()
                .email(testEmail)
                .expiresAt(now.plusMinutes(5))
                .verified(false)
                .build();

        when(otpRepository.findByEmail(testEmail)).thenReturn(Optional.of(unverified));
        assertFalse(otpStore.isVerified(testEmail));
    }
}
