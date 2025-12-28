package com.example.web_ban_sach.Service.chat;

import com.example.web_ban_sach.entity.chat.ConversationDocument;
import com.example.web_ban_sach.entity.chat.MessageDocument;
import com.example.web_ban_sach.util.MessageEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to migrate existing unencrypted messages to encrypted format
 * This should be run once after implementing encryption
 */
@Service
public class ChatEncryptionMigrationService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private MessageEncryptionUtil encryptionUtil;
    
    /**
     * Migrates all unencrypted messages to encrypted format
     * This method checks if a message is already encrypted before processing
     */
    public void migrateAllMessages() {
        System.out.println("Starting message encryption migration...");
        
        // Migrate chat messages
        migrateChatMessages();
        
        // Migrate conversation last messages
        migrateConversationLastMessages();
        
        System.out.println("Message encryption migration completed!");
    }
    
    private void migrateChatMessages() {
        // Get all messages
        List<MessageDocument> messages = mongoTemplate.findAll(MessageDocument.class, "chat_messages");
        
        int migratedCount = 0;
        int totalCount = messages.size();
        
        for (MessageDocument message : messages) {
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                try {
                    // Try to decrypt the message to see if it's already encrypted
                    encryptionUtil.decrypt(message.getContent());
                    // If decryption succeeds, message is already encrypted
                    continue;
                } catch (Exception e) {
                    // If decryption fails, message is not encrypted yet
                    try {
                        String encryptedContent = encryptionUtil.encrypt(message.getContent());
                        
                        Query query = new Query(Criteria.where("messageId").is(message.getMessageId()));
                        Update update = new Update().set("content", encryptedContent);
                        mongoTemplate.updateFirst(query, update, "chat_messages");
                        
                        migratedCount++;
                        
                        if (migratedCount % 100 == 0) {
                            System.out.println("Migrated " + migratedCount + "/" + totalCount + " messages");
                        }
                    } catch (Exception encryptException) {
                        System.err.println("Failed to encrypt message: " + message.getMessageId() + ", Error: " + encryptException.getMessage());
                    }
                }
            }
        }
        
        System.out.println("Migrated " + migratedCount + " chat messages out of " + totalCount + " total messages");
    }
    
    private void migrateConversationLastMessages() {
        // Get all conversations
        List<ConversationDocument> conversations = mongoTemplate.findAll(ConversationDocument.class, "conversations");
        
        int migratedCount = 0;
        int totalCount = conversations.size();
        
        for (ConversationDocument conversation : conversations) {
            if (conversation.getLastMessage() != null && !conversation.getLastMessage().isEmpty()) {
                try {
                    // Try to decrypt to see if it's already encrypted
                    encryptionUtil.decrypt(conversation.getLastMessage());
                    // If decryption succeeds, message is already encrypted
                    continue;
                } catch (Exception e) {
                    // If decryption fails, message is not encrypted yet
                    try {
                        String encryptedLastMessage = encryptionUtil.encrypt(conversation.getLastMessage());
                        
                        Query query = new Query(Criteria.where("conversationId").is(conversation.getConversationId()));
                        Update update = new Update().set("lastMessage", encryptedLastMessage);
                        mongoTemplate.updateFirst(query, update, "conversations");
                        
                        migratedCount++;
                    } catch (Exception encryptException) {
                        System.err.println("Failed to encrypt last message for conversation: " + conversation.getConversationId() + ", Error: " + encryptException.getMessage());
                    }
                }
            }
        }
        
        System.out.println("Migrated " + migratedCount + " conversation last messages out of " + totalCount + " total conversations");
    }
    
    /**
     * Checks the encryption status of all messages
     * @return number of unencrypted messages
     */
    public int checkEncryptionStatus() {
        List<MessageDocument> messages = mongoTemplate.findAll(MessageDocument.class, "chat_messages");
        int unencryptedCount = 0;
        
        for (MessageDocument message : messages) {
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                try {
                    encryptionUtil.decrypt(message.getContent());
                    // If decryption succeeds, message is encrypted
                } catch (Exception e) {
                    // If decryption fails, message is not encrypted
                    unencryptedCount++;
                }
            }
        }
        
        System.out.println("Found " + unencryptedCount + " unencrypted messages out of " + messages.size() + " total messages");
        return unencryptedCount;
    }
}