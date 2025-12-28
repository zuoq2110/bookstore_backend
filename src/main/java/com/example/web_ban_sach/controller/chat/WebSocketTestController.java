package com.example.web_ban_sach.controller.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
public class WebSocketTestController {
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "WebSocket server is running");
        response.put("endpoint", "/ws");
        response.put("protocols", "STOMP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getEndpoints() {
        Map<String, Object> response = new HashMap<>();
        
        // WebSocket endpoints
        Map<String, String> wsEndpoints = new HashMap<>();
        wsEndpoints.put("connect", "/ws");
        wsEndpoints.put("protocols", "STOMP over WebSocket");
        
        // Message destinations
        Map<String, String> destinations = new HashMap<>();
        destinations.put("sendMessage", "/app/chat.sendMessage");
        destinations.put("typing", "/app/chat.typing");
        destinations.put("markReadUntil", "/app/chat.markReadUntil");
        destinations.put("createConversation", "/app/chat.createConversation");
        
        // Subscription channels
        Map<String, String> subscriptions = new HashMap<>();
        subscriptions.put("messages", "/user/{userId}/queue/messages");
        subscriptions.put("typing", "/user/{userId}/queue/typing");
        subscriptions.put("conversations", "/user/{userId}/queue/conversations");
        subscriptions.put("readReceipts", "/user/{userId}/queue/read-receipt");
        
        response.put("websocket", wsEndpoints);
        response.put("destinations", destinations);
        response.put("subscriptions", subscriptions);
        
        return ResponseEntity.ok(response);
    }
}