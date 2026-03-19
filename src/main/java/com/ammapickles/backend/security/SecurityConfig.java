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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

   
    // 1  WEB / THYMELEAF  (Session based)
    
    //  /home and /products -> PUBLIC 
    //  /cart and /orders   - > LOGIN 
  
    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/", "/home", "/login", "/register",
            		         "/logout",
            		         "/forgot-password",    
                             "/reset-password", 
                             "/cart/**", "/orders/**",
                             "/addresses/**",
                             "/profile/**",
                             "/admin/**",           
                             "/products/**",
                             "/css/**", "/images/**", "/js/**",
                             "/favicon.ico")

            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            .authorizeHttpRequests(auth -> auth

                // PUBLIC
                .requestMatchers(
                		"/",
                	    "/home",
                	    "/login",
                	    "/register",
                	    "/forgot-password",    
                	    "/reset-password", 
                	    "/error", 
                	    "/css/**",
                	    "/images/**",
                	    "/js/**",
                	    "/favicon.ico",
                	    "/products/**"
                	    
                      ).permitAll()

                //  PROTECTED 
                .requestMatchers("/cart/**").authenticated()
                .requestMatchers("/orders/**").authenticated()
                .requestMatchers("/profile/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(customSuccessHandler()) 
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/home")   // after logout -> back to home (still see products)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        
            

        return http.build();
    }

    
    //  2  REST API  (JWT based)
    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**", "/actuator/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/users/verify/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/products/**", "/api/categories/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/cart/**",
                                 "/api/orders/**",
                                 "/api/addresses/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/addresses/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    
    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/home");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}