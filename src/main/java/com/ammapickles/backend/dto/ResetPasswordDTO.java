package com.ammapickles.backend.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDTO {
	
	
	 @NotBlank
	 @Size(min=8, message="password must be at least 8 characters long with atleast one number and one special character ")
	 @Pattern( regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$")
	 private String newPassword ;

}
