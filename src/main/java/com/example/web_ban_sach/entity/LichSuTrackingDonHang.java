package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "lich_su_tracking_don_hang")
public class LichSuTrackingDonHang {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_lich_su")
    private int maLichSu;
    
    @Column(name = "ma_don_hang", nullable = false)
    private int maDonHang;
    
    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai;
    
    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;
    
    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;
    
    @Column(name = "vi_tri")
    private String viTri;
    
    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;
    
    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;
    
    @Column(name = "thoi_gian", nullable = false)
    private Timestamp thoiGian;
    
    @Column(name = "ngay_tao")
    private Timestamp ngayTao;
    
    @PrePersist
    protected void onCreate() {
        if (thoiGian == null) {
            thoiGian = new Timestamp(System.currentTimeMillis());
        }
        if (ngayTao == null) {
            ngayTao = new Timestamp(System.currentTimeMillis());
        }
    }
}
