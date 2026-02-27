package com.ammapickles.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

                                                                  //  @Slf4j gives you 'log' variable automatically via Lombok
                                                               // Use log.info(), log.warn(), log.error() — never System.out.println() in production!  




@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // shouldNotFilter()
    // public URLs don't need JWT checking at all 
    // This makes your app slightly faster and avoids unnecessary token parsing
    
    private static final List<String> PUBLIC_URLS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/reset-password",
            "/actuator/health"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // If the request path starts with any public URL, skip this filter
        return PUBLIC_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

                //  Read the Authorization header
        String authHeader = request.getHeader("Authorization");

        // Check if header exists and starts with "Bearer "
        // If not, just continue — SecurityConfig will reject unauthenticated requests
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;  
        }

        //  Extract the token (remove "Bearer " prefix — 7 characters)
        String token = authHeader.substring(7);
        String userEmail = null;

       
        //  If we don't catch here, a malformed token causes a 500 error
        // We want a clean 401 Unauthorized instead — much better UX for API consumers
        try {
            // Was calling getUserMailFromToken() — renamed to extractEmail() in JwtUtil
            userEmail = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            log.warn("Could not extract email from JWT token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;  // Stop here — invalid token, don't authenticate
        }

         // If we got an email AND user is not already authenticated
        //   Why check getAuthentication() == null?
        //  Avoid re-authenticating on every filter if already done earlier in the chain
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            //  Load full user details from database using email
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            //  Validate the token against the loaded user
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {

                // Create authentication token
                // Why null as credentials (2nd param)?
                // We already trust the JWT — no need to keep the password around
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials = null (intentional)
                                userDetails.getAuthorities()   // roles: ROLE_ADMIN, ROLE_CUSTOMER
                        );

                //  Attach request details (IP address, session info etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContextHolder
                // This tells Spring Security: "this user is authenticated for this request"
                // It's thread-local only lives for the duration of this HTTP request
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("User '{}' authenticated successfully", userEmail);

            } else {
                log.warn("JWT token validation failed for user: {}", userEmail);
            }
        }

        //  Continue to next filter or controller
        filterChain.doFilter(request, response);
    }
}