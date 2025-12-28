package com.example.web_ban_sach.dto.chat;

public class CreateConversationRequest {
    private Integer userId;
    private Integer sellerId;
    private String userName;
    private String sellerName;
    
    public CreateConversationRequest() {}
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public Integer getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
}