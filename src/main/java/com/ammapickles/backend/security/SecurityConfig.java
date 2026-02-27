package com.ammapickles.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            //  CSRF disabled — correct for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // Stateless session — JWT handles auth, no server-side sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Return clean JSON 401 instead of ugly HTML when token is missing
            // ExceptionHandling in Security
            // Without this, Spring returns a 403 HTML page on unauthorized requests
            // With this, it returns a clean 401 JSON response — much better for REST APIs
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )

            .authorizeHttpRequests(auth -> auth

                                                         //  Public endpoints — no token required
                .requestMatchers(
                    "/api/auth/**",           // login, register, reset-password
                    "/api/users/verify/**"    // email verification
                ).permitAll()

                                                        // Public product & category browsing (GET only)
                .requestMatchers(HttpMethod.GET,
                    "/api/products/**",
                    "/api/categories/**"
                ).permitAll()

                                                           // Actuator secured — only expose /health publicly
                
    
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                                                         // Customer-only endpoints
                                                   
                .requestMatchers(
                    "/api/cart/**",
                    "/api/orders/**",
                    "/api/addresses/**"
                ).hasRole("CUSTOMER")

                                                                   // Admin-only — product management
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                                                               //  Admin-only — category management (POST, PUT, DELETE)
       
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                                                                  // User profile — must be logged in
                .requestMatchers("/api/users/**").authenticated()

                                                                //Everything else requires authentication
                .anyRequest().authenticated()
            )

                                                           // Add JWT filter before Spring's default username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

 
                                             // BCrypt automatically adds a random data before hashing
                                              // So even if two users have same password, their hashed passwords are different
   
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); 
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}