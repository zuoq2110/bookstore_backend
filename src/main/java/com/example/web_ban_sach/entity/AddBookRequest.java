package com.example.web_ban_sach.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBookRequest {
    private String tenSach;
    private String tacGia;
    private String moTa;
    private double giaBan;
    private double giaNhap;
    private int soLuong;
    private String nhaXuatBan;
    private int namXuatBan;
}
