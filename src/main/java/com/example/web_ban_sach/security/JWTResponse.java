package com.example.web_ban_sach.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JWTResponse {
    private String jwt;
    private String refreshToken;
    private int id;
    private String email;
    private boolean isAdmin;
    private boolean isSeller;
    private String tenGianHang;
    
    public JWTResponse(String jwt) {
        this.jwt = jwt;
    }
    
    public JWTResponse(String jwt, String refreshToken) {
        this.jwt = jwt;
        this.refreshToken = refreshToken;
    }
}
