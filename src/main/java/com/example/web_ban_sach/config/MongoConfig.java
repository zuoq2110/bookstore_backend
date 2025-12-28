package com.example.web_ban_sach.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.web_ban_sach.dao")
@EnableMongoAuditing
public class MongoConfig {
    
    private final MongoTemplate mongoTemplate;
    
    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Create indexes on startup
        createIndexes();
    }
    
    private void createIndexes() {
        // Messages collection indexes
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("conversationId", Sort.Direction.ASC)
                                           .on("createdAt", Sort.Direction.DESC));
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("messageId", Sort.Direction.ASC).unique());
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("senderId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("receiverId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("status", Sort.Direction.ASC));
        
        // Conversations collection indexes
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("conversationId", Sort.Direction.ASC).unique());
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("userId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("sellerId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("participants.userId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("lastMessageTime", Sort.Direction.DESC));
    }
}