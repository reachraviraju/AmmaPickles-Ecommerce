package com.ammapickles.backend.controller;

import com.ammapickles.backend.config.OtpStore;
import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.entity.Role;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.RoleRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AuthService;
import com.ammapickles.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthViewControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpStore otpStore;

    @InjectMocks
    private AuthViewController authViewController;

    private final String testEmail = "test@example.com";
    private final String testName = "Test User";
    private final String testPassword = "Password@123";

    @Test
    @DisplayName("GET /login should return 'login' view")
    void testLoginPage() {
        String view = authViewController.loginPage();
        assertEquals("login", view);
    }

    @Test
    @DisplayName("GET /register should return 'register' view")
    void testRegisterPage() {
        String view = authViewController.registerPage();
        assertEquals("register", view);
    }

    // ==========================================
    // SEND OTP TESTS
    // ==========================================

    @Test
    @DisplayName("Send OTP: Success returns 200 OK")
    void testSendOtp_Success() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpStore.generateOtp(testEmail)).thenReturn("123456");
        when(emailService.sendOtpEmail(testEmail, testName, "123456")).thenReturn(true);

        ResponseEntity<ApiResponse<String>> response = authViewController.sendOtp(testEmail, testName);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("OTP sent", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Send OTP: Email already registered returns 400 Bad Request")
    void testSendOtp_EmailAlreadyExists() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        ResponseEntity<ApiResponse<String>> response = authViewController.sendOtp(testEmail, testName);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("already registered"));
        verifyNoInteractions(otpStore);
    }

    @Test
    @DisplayName("Send OTP: Email service delivery failure returns 500")
    void testSendOtp_EmailServiceFails() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpStore.generateOtp(testEmail)).thenReturn("123456");
        when(emailService.sendOtpEmail(testEmail, testName, "123456")).thenReturn(false);

        ResponseEntity<ApiResponse<String>> response = authViewController.sendOtp(testEmail, testName);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to send OTP", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Send OTP: Rate limiting exception returns 400 with message")
    void testSendOtp_RateLimited() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpStore.generateOtp(testEmail)).thenThrow(new RuntimeException("Too many OTP requests. Try again after 10 minutes."));

        ResponseEntity<ApiResponse<String>> response = authViewController.sendOtp(testEmail, testName);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Too many OTP requests"));
    }

    // ==========================================
    // VERIFY OTP TESTS
    // ==========================================

    @Test
    @DisplayName("Verify OTP: Valid code returns 200 OK")
    void testVerifyOtp_Success() {
        when(otpStore.validate(testEmail, "123456")).thenReturn("ok");

        ResponseEntity<ApiResponse<String>> response = authViewController.verifyOtp(testEmail, "123456");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("OTP verified successfully", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Verify OTP: Expired code returns 400 Bad Request")
    void testVerifyOtp_Expired() {
        when(otpStore.validate(testEmail, "123456")).thenReturn("expired");

        ResponseEntity<ApiResponse<String>> response = authViewController.verifyOtp(testEmail, "123456");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Verify OTP: Invalid code returns 400 Bad Request")
    void testVerifyOtp_Invalid() {
        when(otpStore.validate(testEmail, "999999")).thenReturn("invalid");

        ResponseEntity<ApiResponse<String>> response = authViewController.verifyOtp(testEmail, "999999");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid OTP"));
    }

    // ==========================================
    // REGISTER SUBMIT TESTS
    // ==========================================

    @Test
    @DisplayName("Register: Valid verified user redirects to login?verified=true")
    void testRegister_Success() {
        Model model = new ConcurrentModel();
        Role customerRole = new Role(1L, "ROLE_CUSTOMER");

        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpStore.isVerified(testEmail)).thenReturn(true);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(testPassword)).thenReturn("encodedPassword");

        String result = authViewController.register(testName, testEmail, testPassword, "9876543210", model);

        assertEquals("redirect:/login?verified=true", result);
        verify(userRepository).save(any(User.class));
        verify(otpStore).clear(testEmail);
        verify(emailService).sendWelcomeEmail(testEmail, testName);
    }

    @Test
    @DisplayName("Register: Duplicate email rejects and stays on register view")
    void testRegister_DuplicateEmail() {
        Model model = new ConcurrentModel();
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        String result = authViewController.register(testName, testEmail, testPassword, null, model);

        assertEquals("register", result);
        assertTrue(model.containsAttribute("error"));
        assertEquals("Email already registered! Please login.", model.getAttribute("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register: Unverified email rejects and stays on register view")
    void testRegister_UnverifiedEmail() {
        Model model = new ConcurrentModel();
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(otpStore.isVerified(testEmail)).thenReturn(false);

        String result = authViewController.register(testName, testEmail, testPassword, null, model);

        assertEquals("register", result);
        assertTrue(model.containsAttribute("error"));
        assertTrue(((String) model.getAttribute("error")).contains("Email not verified"));
        verify(userRepository, never()).save(any(User.class));
    }
}
