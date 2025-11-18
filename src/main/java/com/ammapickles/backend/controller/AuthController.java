package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.UserDTO;
import com.ammapickles.backend.dto.LoginRequest;
import com.ammapickles.backend.dto.ResetPasswordDTO;
import com.ammapickles.backend.security.JwtUtil;
import com.ammapickles.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.registerUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }
    
    

    // LOGIN -> return JWT token
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        UserDTO user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(token);
    }

    
    // RESET PASSWORD
    @PostMapping("/reset-password/{email}")
    public ResponseEntity<String> resetPassword(
            @PathVariable String email,
            @RequestBody ResetPasswordDTO resetPasswordDTO) {
        userService.resetPassword(email, resetPasswordDTO);
        return ResponseEntity.ok("Password reset successful");
    }
}
