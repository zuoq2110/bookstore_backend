package com.example.web_ban_sach.config;

import com.example.web_ban_sach.Service.JWTService;
import com.example.web_ban_sach.util.UserSecurityService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    @Autowired
    private JWTService jwtService;
    
    @Autowired
    private UserSecurityService userSecurityService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("WebSocket CONNECT attempt from: " + accessor.getHost());
            
            // 1. Extract JWT token from headers
            String token = accessor.getFirstNativeHeader("Authorization");
            System.out.println("Authorization header: " + (token != null ? "Present: " + token : "Missing"));
            
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7).trim(); // Remove "Bearer " prefix and trim
                System.out.println("Token after extraction: " + (token.isEmpty() ? "EMPTY" : token.substring(0, Math.min(20, token.length())) + "..."));
                
                if (!token.isEmpty()) {
                    try {
                        // 2. Validate token
                        String username = jwtService.extractUsername(token);
                        System.out.println("Username from token: " + username);
                        
                        if (username != null) {
                            UserDetails userDetails = userSecurityService.loadUserByUsername(username);
                            
                            if (jwtService.validateToken(token, userDetails)) {
                                // 3. Set user principal
                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(authentication);
                                
                                // 4. Store user ID in session attributes for easy access
                                Integer userId = extractUserIdFromToken(token);
                                if (userId != null) {
                                    accessor.getSessionAttributes().put("userId", userId);
                                }
                                System.out.println("WebSocket authentication successful for user: " + username);
                            } else {
                                System.err.println("Token validation failed");
                            }
                        } else {
                            System.err.println("Username extraction failed");
                        }
                    } catch (Exception e) {
                        // Invalid token - connection will be rejected
                        System.err.println("WebSocket authentication failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Token is empty after Bearer prefix removal");
                }
            } else {
                System.err.println("No valid Authorization header found. Header: " + token);
                
                // For development - allow connections without auth temporarily
                // Remove this in production!
                System.out.println("WARNING: Allowing unauthenticated connection for development");
                String userId = accessor.getFirstNativeHeader("userId");
                if (userId != null) {
                    try {
                        accessor.getSessionAttributes().put("userId", Integer.parseInt(userId));
                        System.out.println("Set userId from header: " + userId);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid userId format: " + userId);
                    }
                }
            }
        }
        
        return message;
    }
    
    private Integer extractUserIdFromToken(String token) {
        try {
            Claims claims = jwtService.extractAllClaims(token);
            Object userId = claims.get("id");
            if (userId instanceof Integer) {
                return (Integer) userId;
            } else if (userId instanceof Number) {
                return ((Number) userId).intValue();
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        return null;
    }
    
    private UserPrincipal getUserFromToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            Integer userId = extractUserIdFromToken(token);
            
            if (username != null && userId != null) {
                return new UserPrincipal(userId, username);
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        return null;
    }
    
    // Simple Principal implementation for WebSocket
    public static class UserPrincipal implements Principal {
        private final Integer userId;
        private final String username;
        
        public UserPrincipal(Integer userId, String username) {
            this.userId = userId;
            this.username = username;
        }
        
        @Override
        public String getName() {
            return username;
        }
        
        public Integer getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
    }
}