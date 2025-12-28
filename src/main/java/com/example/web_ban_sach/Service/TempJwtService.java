package com.example.web_ban_sach.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class TempJwtService {
    
    // Separate secret key for temporary tokens (must be different from main JWT)
    private final SecretKey TEMP_SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    
    // Temporary token expires in 10 minutes
    private static final long TEMP_TOKEN_EXPIRATION = 10 * 60 * 1000; // 10 minutes
    private static final String TEMP_TOKEN_PREFIX = "TEMP_"; // Prefix to identify temp tokens
    
    /**
     * Generate temporary JWT token after OTP verification
     * This token allows user to complete registration within 10 minutes
     */
    public String generateTempToken(String email, String purpose) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("purpose", purpose); // "registration"
        claims.put("tokenType", "temporary");
        
        return createTempToken(claims, email);
    }
    
    private String createTempToken(Map<String, Object> claims, String subject) {
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TEMP_TOKEN_EXPIRATION))
                .signWith(TEMP_SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
        
        // Add prefix to distinguish from main JWT tokens
        return TEMP_TOKEN_PREFIX + token;
    }
    
    /**
     * Extract email from temporary token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    
    /**
     * Extract purpose from temporary token
     */
    public String extractPurpose(String token) {
        return extractClaim(token, claims -> claims.get("purpose", String.class));
    }
    
    /**
     * Extract token type from temporary token
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }
    
    /**
     * Extract expiration date from temporary token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        // Remove prefix if present
        String actualToken = token;
        if (token.startsWith(TEMP_TOKEN_PREFIX)) {
            actualToken = token.substring(TEMP_TOKEN_PREFIX.length());
        }
        
        return Jwts.parserBuilder()
                .setSigningKey(TEMP_SECRET_KEY)
                .build()
                .parseClaimsJws(actualToken)
                .getBody();
    }
    
    /**
     * Check if temporary token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Validate temporary token for registration purpose
     */
    public Boolean validateTempToken(String token, String email) {
        try {
            final String extractedEmail = extractEmail(token);
            final String purpose = extractPurpose(token);
            final String tokenType = extractTokenType(token);
            
            return (extractedEmail.equals(email) && 
                    "registration".equals(purpose) &&
                    "temporary".equals(tokenType) &&
                    !isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("Error validating temp token: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract email from Authorization header
     */
    public String extractEmailFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        try {
            String token = authHeader.substring(7);
            // Only process if it's a temp token
            if (!token.startsWith(TEMP_TOKEN_PREFIX)) {
                return null;
            }
            return extractEmail(token);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Validate Authorization header for registration
     */
    public Boolean validateAuthHeader(String authHeader, String email) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        
        try {
            String token = authHeader.substring(7);
            // Only validate if it's a temp token
            if (!token.startsWith(TEMP_TOKEN_PREFIX)) {
                return false;
            }
            return validateTempToken(token, email);
        } catch (Exception e) {
            return false;
        }
    }
}