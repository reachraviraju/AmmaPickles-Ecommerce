package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.auth.AuthResponse;
import com.ammapickles.backend.dto.auth.LoginRequest;
import com.ammapickles.backend.dto.auth.RegisterRequest;
import com.ammapickles.backend.dto.auth.ResetPasswordRequest;

                                          //   Single Responsibility Principle:
                                         // AuthService handles ONLY authentication — login, register, password reset
                                        // UserService handles ONLY profile — get, update
public interface AuthService {
	
	
	
	   AuthResponse register(RegisterRequest request);
	 
	    AuthResponse login(LoginRequest request);
	 
	    // STEP 1: User submits email → generates token ->  sends reset email
	    void forgotPassword(String email);
	 
	    // STEP 2: User clicks link with token -> validates token → updates password
	    void resetPasswordWithToken(String token, ResetPasswordRequest request);
	 
	    
	 
	
	

}