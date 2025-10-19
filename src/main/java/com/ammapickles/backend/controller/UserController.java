package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.LoginRequest;
import com.ammapickles.backend.dto.ResetPasswordDTO;
import com.ammapickles.backend.dto.UserDTO;
import com.ammapickles.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.registerUser(userDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> loginUser(@RequestBody LoginRequest loginResquest) {
    	
         UserDTO userDTO = userService.login( loginResquest.getUsername(), loginResquest.getPassword());
        return ResponseEntity.ok(userDTO);
    }
    
    @PutMapping("/reset-password/{username}")
    public ResponseEntity<String> resetPassword(@PathVariable String username , @RequestBody ResetPasswordDTO resetPasswordDTO)
    {
    	userService.resetPassword(username, resetPasswordDTO);
    	return  ResponseEntity.ok("Password updated successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }
}
