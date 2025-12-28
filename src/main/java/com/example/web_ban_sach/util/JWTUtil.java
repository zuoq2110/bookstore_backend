package com.example.web_ban_sach.util;

import com.example.web_ban_sach.Service.JWTService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class JWTUtil {
    
    @Autowired
    private JWTService jwtService;
    
    /**
     * Extract userId from JWT token in Authorization header
     */
    public Integer getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return getUserIdFromToken(token);
        }
        return null;
    }
    
    /**
     * Extract userId from JWT token string
     */
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = jwtService.extractAllClaims(token);
            Object userId = claims.get("id");
            System.out.println("JWT Token claims: " + claims);
            System.out.println("UserId from token: " + userId);
            if (userId instanceof Integer) {
                return (Integer) userId;
            } else if (userId instanceof Number) {
                return ((Number) userId).intValue();
            }
        } catch (Exception e) {
            System.out.println("Error parsing JWT token: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Extract userId from current authentication context
     */
    public Integer getUserIdFromAuth() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // Extract from principal if available
                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    // For now, we'll need to get userId differently
                    // You might need to modify UserDetails implementation to include userId
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}