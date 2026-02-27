package com.ammapickles.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;


                                                          // Instead of writing: private static final Logger log = LoggerFactory.getLogger(...)
                                                          // @Slf4j gives you log variable automatically — log.info(), log.error(), log.warn()
                                                        // Always use logger instead of System.out.println() in production code
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

   
    // GENERATE TOKEN

    
    
    //  Jwts.builder().subject().signWith(key)  cleaner, algorithm auto-detected
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)                                              // who the token is for
                .issuedAt(new Date())                                        // when token was created
                .expiration(new Date(System.currentTimeMillis() + expirationTime))  // when it expires
                .signWith(getSignKey())                                      // sign with secret key
                .compact();
    }

   
    // EXTRACT EMAIL FROM TOKEN
 

    
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

  
    // VALIDATE TOKEN
   

  
  
                        // validate JWT in try-catch 
                       // Token could be tampered ->  MalformedJwtException
                      // Token could be expired ->  ExpiredJwtException
                      // Token could use wrong algorithm - >UnsupportedJwtException
    public boolean validateToken(String token, String userEmail) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(userEmail) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

  
    // PRIVATE HELPERS
    

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic method with Function<Claims, T>
    
    // Instead of writing separate methods for each claim (subject, expiration, etc.)
    // We write ONE generic method and pass WHAT to extract as a function
    // Example :
    //   extractClaim(token, Claims::getSubject)      -> gets email
    //   extractClaim(token, Claims::getExpiration)   - > gets expiry date
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()                   // NEW API: parser() not parserBuilder()
                .verifyWith(getSignKey())               // NEW API: verifyWith() not setSigningKey()
                .build()
                .parseSignedClaims(token)               // NEW API: parseSignedClaims() not parseClaimsJws()
                .getPayload();                          // NEW API: getPayload() not getBody()
        return claimsResolver.apply(claims);
    }

 
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}