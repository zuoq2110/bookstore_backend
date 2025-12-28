package com.example.web_ban_sach.entity.chat;

import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

public class ParticipantDocument {
    @Indexed
    private Integer userId;
    private Integer unreadCount;
    private String lastReadMessageId;
    private LocalDateTime lastReadAt;
    
    public ParticipantDocument() {}
    
    public ParticipantDocument(Integer userId, Integer unreadCount, String lastReadMessageId, LocalDateTime lastReadAt) {
        this.userId = userId;
        this.unreadCount = unreadCount;
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = lastReadAt;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public Integer getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public String getLastReadMessageId() {
        return lastReadMessageId;
    }
    
    public void setLastReadMessageId(String lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
    
    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }
    
    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}