package com.ammapickles.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
    
            throws ServletException, IOException {
    	

        // Get Authorization header 
    	
        String authHeader = request.getHeader("Authorization"); 
        
        
        
        
        
        
       
        
        
        
       
        
        
        
        
        String token = null;
        String userEmail = null;

        // Extract token if it starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            userEmail = jwtUtil.getUserMailFromToken(token); 
          
        }
          
        
       
        // If user not yet authenticated

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) { 
        
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Validate token
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                
                
                

                //  Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response); 
        
    }
}
