package com.ammapickles.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests()
  
                .requestMatchers("/api/users/register", "/api/users/login" , "/api/products/**").permitAll()
                .requestMatchers("/api/users/reset-password/**").permitAll()
                .requestMatchers("/api/cart/**" ,"/api/orders/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST,"/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,"/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/products/**").hasRole("ADMIN") 
                .anyRequest().authenticated()
            .and()
            .httpBasic(); // can switch to formLogin() if needed
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
