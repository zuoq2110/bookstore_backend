package com.example.web_ban_sach.dto;

import lombok.Data;

@Data
public class TwoFactorSetupRequest {
    private String verificationCode; // Mã 6 số để xác nhận lần đầu
}