package com.example.web_ban_sach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaRequiredResponse {
    private boolean success;
    private String message;
    private String mfaToken; // Token tạm thời để xác thực 2FA
    private String errorCode; // MFA_REQUIRED
    private long expiresIn; // Thời gian hết hạn của mfaToken (seconds)
}