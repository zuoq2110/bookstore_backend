package com.example.web_ban_sach.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerRegisterRequest {
    private int maNguoiDung;
    private String tenGianHang;
    private String moTaGianHang;
}
