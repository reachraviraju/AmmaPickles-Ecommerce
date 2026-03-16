package com.ammapickles.backend.security;

import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security calls this with whatever was typed in the login form.
    // If it contains "@" ->  treat as email
    // Otherwise          - > treat as phone number
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        User user;

        if (identifier.contains("@")) {
            // Login by email
            log.info("Loading user by email: {}", identifier);
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> {
                        log.warn("No user found with email: {}", identifier);
                        return new UsernameNotFoundException("Invalid email or password");
                    });
        } else {
            // Login by phone number
            log.info("Loading user by phone: {}", identifier);
            user = userRepository.findByPhoneNumber(identifier)
                    .orElseThrow(() -> {
                        log.warn("No user found with phone: {}", identifier);
                        return new UsernameNotFoundException("Invalid phone number or password");
                    });
        }

        log.info("User loaded successfully: {}", identifier);
        // can access user.getId(), user.getPhoneNumber()  anywhere
        return new CustomUserDetails(user);
    }
}