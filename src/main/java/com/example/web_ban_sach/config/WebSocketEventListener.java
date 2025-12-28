package com.example.web_ban_sach.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Map;

@Component
public class WebSocketEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        // User connected - log connection
        String userId = getUserIdFromSession(event);
        logger.info("User {} connected to WebSocket", userId);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // User disconnected - cleanup if needed
        String userId = getUserIdFromSession(event);
        logger.info("User {} disconnected from WebSocket", userId);
        
        // Optional: Cleanup user session data if needed
        // For example, remove from online users list, etc.
    }
    
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("User session {} subscribed to {}", sessionId, destination);
    }
    
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("User session {} unsubscribed", sessionId);
    }
    
    private String getUserIdFromSession(AbstractSubProtocolEvent event) {
        // Extract userId from query parameters or headers
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        
        if (sessionAttributes != null) {
            Object userId = sessionAttributes.get("userId");
            if (userId != null) {
                return userId.toString();
            }
        }
        
        // Try to get from native headers
        String authHeader = headerAccessor.getFirstNativeHeader("Authorization");
        if (authHeader != null) {
            // Extract user ID from JWT token if needed
            // This would require JWT parsing logic
        }
        
        return "Unknown";
    }
}