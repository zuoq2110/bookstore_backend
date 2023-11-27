package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="hinh_anh")
public class HinhAnh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ma_hinh_anh")
    private int maHinhAnh;

    @Column(name="ten_hinh_anh", length = 256)
    private String tenHinhAnh;

    @Column(name="la_icon")
    private boolean laIcon;

    @Column(name="duong_dan")
    private String duongDan;

    @Column(name="du_lieu_anh")
    @Lob
    private String duLieuAnh;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name="ma_sach", nullable = false)
    private Sach sach;
}
