package com.ammapickles.backend.controller;

import com.ammapickles.backend.entity.Role;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.RoleRepository;
import com.ammapickles.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashSet;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           Model model) {

        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email already registered! Please login.");
            return "register";
        }

        // Fetch ROLE_CUSTOMER from roles table
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException(
                    "ROLE_CUSTOMER not found! Please add it to roles table."));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        User user = User.builder()
                .username(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(user);

        return "redirect:/login?registered=true";
    }
}