package com.example.web_ban_sach.controller.chat;

import com.example.web_ban_sach.Service.chat.ChatService;
import com.example.web_ban_sach.dto.chat.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload RealtimeChatMessage message, 
                           SimpMessageHeaderAccessor headerAccessor,
                           Principal principal) {
        System.out.println("=== WEBSOCKET MESSAGE RECEIVED ===");
        System.out.println("Message content: " + message.getContent());
        System.out.println("Sender ID: " + message.getSenderId());
        System.out.println("Receiver ID: " + message.getReceiverId());
        System.out.println("Conversation ID: " + message.getConversationId());
        
        try {
            // 1. Validate message
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                System.err.println("ERROR: Message content is null or empty");
                return;
            }
            
            // 2. Set default values
            if (message.getType() == null) {
                message.setType("text");
            }
            if (message.getStatus() == null) {
                message.setStatus("sent");
            }
            
            System.out.println("=== SAVING MESSAGE TO DATABASE ===");
            // 3. Save to database
            RealtimeChatMessage savedMessage = chatService.saveMessage(message);
            System.out.println("Message saved successfully with ID: " + savedMessage.getMessageId());
            
            // 4. Send to receiver's queue
            messagingTemplate.convertAndSendToUser(
                String.valueOf(message.getReceiverId()),
                "/queue/messages",
                savedMessage
            );
            System.out.println("Message sent to receiver: " + message.getReceiverId());
            
            // 5. Send confirmation back to sender
            messagingTemplate.convertAndSendToUser(
                String.valueOf(message.getSenderId()),
                "/queue/messages",
                savedMessage
            );
            System.out.println("Confirmation sent to sender: " + message.getSenderId());
            
        } catch (Exception e) {
            System.err.println("ERROR in sendMessage: " + e.getMessage());
            e.printStackTrace();
            // Send error message back to sender
            message.setStatus("failed");
            messagingTemplate.convertAndSendToUser(
                String.valueOf(message.getSenderId()),
                "/queue/messages",
                message
            );
        }
    }
    
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload TypingIndicator typing,
                                    SimpMessageHeaderAccessor headerAccessor,
                                    Principal principal) {
        try {
            // Set timestamp
            typing.setTimestamp(LocalDateTime.now());
            
            // Send typing indicator to conversation participants
            Integer otherUserId = chatService.getOtherUserId(typing.getConversationId(), typing.getUserId());
            if (otherUserId != null) {
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(otherUserId),
                    "/queue/typing",
                    typing
                );
            }
        } catch (Exception e) {
            // Silently ignore typing indicator errors
        }
    }
    
    @MessageMapping("/chat.markReadUntil")
    public void markReadUntil(@Payload ReadUntilRequest request,
                             SimpMessageHeaderAccessor headerAccessor,
                             Principal principal) {
        try {
            // 1. Validate request
            if (request.getConversationId() == null || 
                request.getReadUntilMessageId() == null ||
                request.getReaderId() == null) {
                return;
            }
            
            // 2. Update all messages status up to readUntilMessageId
            chatService.markMessagesAsReadUntil(
                request.getConversationId(),
                request.getReadUntilMessageId(),
                request.getReaderId()
            );
            
            // 3. Notify sender about read status
            Integer otherUserId = chatService.getOtherUserId(request.getConversationId(), request.getReaderId());
            if (otherUserId != null) {
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(otherUserId),
                    "/queue/read-receipt",
                    request
                );
            }
            
        } catch (Exception e) {
            // Silently ignore read receipt errors
        }
    }
    
    @MessageMapping("/chat.createConversation")
    public void createConversation(@Payload CreateConversationRequest request,
                                 SimpMessageHeaderAccessor headerAccessor,
                                 Principal principal) {
        try {
            // 1. Validate request
            if (request.getUserId() == null || request.getSellerId() == null) {
                return;
            }
            
            // 2. Create conversation if not exists
            ConversationModel conversation = chatService.createConversation(
                request.getUserId(),
                request.getSellerId()
            );
            
            // 3. Notify both participants about new conversation
            messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getUserId()),
                "/queue/conversations",
                conversation
            );
            messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getSellerId()),
                "/queue/conversations",
                conversation
            );
            
        } catch (Exception e) {
            // Send error back to requester
            messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getUserId()),
                "/queue/error",
                "Failed to create conversation: " + e.getMessage()
            );
        }
    }
}