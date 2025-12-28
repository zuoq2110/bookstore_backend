package com.example.web_ban_sach.dto;

import lombok.Data;

import java.sql.Date;

@Data
public class UserDTO {
    private int maNguoiDung;
    private String hoDem;
    private String ten;
    private String tenDangNhap;
    private String email;
    private String soDienThoai;
    private String diaChiMuaHang;
    private String diaChiGiaoHang;
    private Date ngaySinh;
    private String gioiTinh;
    private Boolean isAdmin;
    private Boolean isSeller;
    private String anhDaiDien;
    private String tenGianHang;
    private String moTaGianHang;
    private String diaChiGianHang;
    private String soDienThoaiGianHang;
}
