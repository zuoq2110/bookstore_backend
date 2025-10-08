package com.example.web_ban_sach.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "seller_books")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SellerBooks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_sach", nullable = false)
    private Sach sach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_seller", nullable = false)
    private NguoiDung seller;

    @Column(name = "ngay_dang")
    private Timestamp ngayDang;

    @Column(name = "trang_thai", length = 50)
    private String trangThai = "active";

    @Column(name = "gia_nhap")
    private Double giaNhap;

    @Column(name = "so_luong_kho")
    private int soLuongKho = 0;

    @Column(name = "ngay_cap_nhat")
    private Timestamp ngayCapNhat;

    @PrePersist
    protected void onCreate() {
        ngayDang = new Timestamp(System.currentTimeMillis());
        ngayCapNhat = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = new Timestamp(System.currentTimeMillis());
    }
}
