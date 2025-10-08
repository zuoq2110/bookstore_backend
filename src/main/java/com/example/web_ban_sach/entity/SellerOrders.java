package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "seller_orders")
public class SellerOrders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "ma_don_hang", nullable = false)
    private int maDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_seller", nullable = false)
    private NguoiDung seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach", nullable = false)
    private Sach sach;

    @Column(name = "so_luong", nullable = false)
    private int soLuong;

    @Column(name = "gia_ban", nullable = false)
    private Double giaBan;

    @Column(name = "tong_tien", nullable = false)
    private Double tongTien;

    @Column(name = "ngay_dat")
    private Timestamp ngayDat;

    @Column(name = "trang_thai", length = 50)
    private String trangThai = "pending";

    @PrePersist
    protected void onCreate() {
        ngayDat = new Timestamp(System.currentTimeMillis());
    }
}
