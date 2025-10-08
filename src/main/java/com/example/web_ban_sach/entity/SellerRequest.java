package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_yeu_cau")
    private Integer maYeuCau;
    
    @Column(name = "ma_nguoi_dung", nullable = false)
    private Integer maNguoiDung;
    
    @Column(name = "ten_gian_hang", nullable = false, length = 255)
    private String tenGianHang;
    
    @Column(name = "mo_ta_gian_hang", columnDefinition = "TEXT")
    private String moTaGianHang;
    
    @Column(name = "trang_thai", length = 20)
    private String trangThai = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
    
    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;
    
    @Column(name = "nguoi_xu_ly")
    private Integer nguoiXuLy;
    
    @Column(name = "ly_do_tu_choi", columnDefinition = "TEXT")
    private String lyDoTuChoi;
    
    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
        if (trangThai == null) {
            trangThai = "PENDING";
        }
    }
}
