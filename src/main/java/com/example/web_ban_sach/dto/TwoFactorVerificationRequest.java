package com.example.web_ban_sach.dto;

import lombok.Data;

@Data
public class TwoFactorVerificationRequest {
    private String mfaToken; // Token tạm thời từ backend
    private String verificationCode; // Mã 6 số từ Authenticator app
}