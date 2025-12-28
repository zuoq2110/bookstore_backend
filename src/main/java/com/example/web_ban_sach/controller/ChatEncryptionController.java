package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.chat.ChatEncryptionMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat-encryption")
@CrossOrigin("*")
public class ChatEncryptionController {
    
    @Autowired
    private ChatEncryptionMigrationService migrationService;
    
    /**
     * Check the encryption status of chat messages
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkEncryptionStatus() {
        try {
            int unencryptedCount = migrationService.checkEncryptionStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unencryptedMessages", unencryptedCount);
            response.put("message", unencryptedCount == 0 ? 
                "All messages are encrypted" : 
                "Found " + unencryptedCount + " unencrypted messages");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Migrate all unencrypted messages to encrypted format
     * This should be called once after implementing encryption
     */
    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrateMessages() {
        try {
            migrationService.migrateAllMessages();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message encryption migration completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}