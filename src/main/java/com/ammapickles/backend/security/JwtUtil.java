package com.ammapickles.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    // Generate token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
        
        
    }

    // Extract email/username from token
    public String getUserMailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Validate token
    public boolean validateToken(String token, String userEmail) {
        String extractedEmail = getUserMailFromToken(token);
        return extractedEmail.equals(userEmail) && !isTokenExpired(token);
    }

    // Check if token expired
    private boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    private Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Extract claim helper
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())  // use correct key decoding
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    // Proper key handling (Base64 decoding)
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
