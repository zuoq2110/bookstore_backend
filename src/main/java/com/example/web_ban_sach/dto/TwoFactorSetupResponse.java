package com.example.web_ban_sach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {
    private boolean success;
    private String message;
    private String qrCodeUrl; // URL để tạo QR code
    private String secretKey; // Secret key (để backup)
    private List<String> backupCodes; // Backup codes (chỉ hiển thị lần đầu)
}