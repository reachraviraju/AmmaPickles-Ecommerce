package com.ammapickles.backend.dto.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
	
	
	    @NotBlank(message = "Email or phone number is required")
	    private String identifier;
	 
	    @NotBlank(message = "Password is required")
	    private String password;

    
}