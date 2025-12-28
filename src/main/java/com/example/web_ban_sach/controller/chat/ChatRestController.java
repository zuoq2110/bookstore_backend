package com.example.web_ban_sach.controller.chat;

import com.example.web_ban_sach.Service.chat.ChatService;
import com.example.web_ban_sach.dto.chat.ConversationModel;
import com.example.web_ban_sach.dto.chat.CreateConversationRequest;
import com.example.web_ban_sach.dto.chat.RealtimeChatMessage;
import com.example.web_ban_sach.dto.chat.SendMessageRequest;
import com.example.web_ban_sach.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatRestController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private JWTUtil jwtUtil;
    
    // Test endpoint to check if API is accessible
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chat API is working!");
    }
    
    // Test conversation creation without JWT (for debugging)
    @PostMapping("/conversations/test")
    public ResponseEntity<?> createConversationTest(@RequestBody CreateConversationRequest request) {
        try {
            // Use hardcoded userId for testing
            Integer testUserId = 1; 
            
            ConversationModel conversation = chatService.createConversation(
                testUserId, 
                request.getSellerId()
            );
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating test conversation: " + e.getMessage());
        }
    }
    
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationModel>> getConversations(
            HttpServletRequest request) {
        try {
            Integer userId = jwtUtil.getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            List<ConversationModel> conversations = chatService.getConversationsForUser(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<RealtimeChatMessage>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        try {
            Integer userId = jwtUtil.getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            List<RealtimeChatMessage> messages = chatService.getMessagesForConversation(
                conversationId, userId, page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<RealtimeChatMessage> sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        try {
            Integer userId = jwtUtil.getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            // Create RealtimeChatMessage from request
            RealtimeChatMessage message = new RealtimeChatMessage();
            message.setConversationId(conversationId);
            message.setSenderId(userId);
            message.setReceiverId(request.getReceiverId());
            message.setContent(request.getContent());
            message.setType(request.getMessageType() != null ? request.getMessageType() : "text");
            message.setStatus("sent");
            
            RealtimeChatMessage savedMessage = chatService.saveMessage(message);
            return ResponseEntity.ok(savedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/conversations")
    public ResponseEntity<ConversationModel> createConversation(
            @RequestBody CreateConversationRequest request,
            HttpServletRequest httpRequest) {
        try {
            System.out.println("=== DEBUG CREATE CONVERSATION ===");
            String authHeader = httpRequest.getHeader("Authorization");
            System.out.println("Auth header: " + authHeader);
            
            Integer userId = jwtUtil.getUserIdFromRequest(httpRequest);
            System.out.println("Extracted userId: " + userId);
            
            if (userId == null) {
                System.out.println("UserId is null, returning 401");
                return ResponseEntity.status(401).body(null);
            }
            
            ConversationModel conversation = chatService.createConversation(userId, request.getSellerId());
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            System.out.println("Error in createConversation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}