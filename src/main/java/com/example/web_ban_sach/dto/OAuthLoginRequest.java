package com.example.web_ban_sach.dto;

import lombok.Data;

@Data
public class OAuthLoginRequest {
    private String provider;      // "google" hoặc "apple"
    private String idToken;        // ID token từ OAuth provider
    private String accessToken;    // Access token (optional)
    private String email;          // Email từ OAuth
    private String displayName;    // Tên hiển thị
}
