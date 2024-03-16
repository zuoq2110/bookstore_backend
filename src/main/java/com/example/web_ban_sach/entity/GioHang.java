package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "gio-hang")
public class GioHang {
    @Id
    @Column(name = "ma-gio-hang")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int maGioHang;
    @Column(name = "so-luong")
    private int soLuong;

    @ManyToOne(
    )
    @JoinColumn(name = "ma-sach", nullable = false)
    private Sach sach;

    @ManyToOne(
    )
    @JoinColumn(name = "ma-nguoi-dung", nullable = false)
    private NguoiDung nguoiDung;
}
