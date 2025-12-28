package com.example.web_ban_sach.dto.chat;

import java.time.LocalDateTime;

public class ReadUntilRequest {
    private String conversationId;
    private Integer readerId;
    private String readUntilMessageId;
    private LocalDateTime timestamp;
    
    public ReadUntilRequest() {}
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public Integer getReaderId() {
        return readerId;
    }
    
    public void setReaderId(Integer readerId) {
        this.readerId = readerId;
    }
    
    public String getReadUntilMessageId() {
        return readUntilMessageId;
    }
    
    public void setReadUntilMessageId(String readUntilMessageId) {
        this.readUntilMessageId = readUntilMessageId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}