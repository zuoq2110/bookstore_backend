package com.example.web_ban_sach.dto;

import lombok.Data;

@Data
public class AccountLinkingRequest {
    private String provider;           // "google" hoặc "apple"
    private String idToken;            // ID token từ OAuth provider
    private String email;              // Email của user
    private String displayName;        // Tên hiển thị
    private boolean forceLink;         // true = force link với account hiện có
    private String existingPassword;   // Password của account hiện có (để verify)
}