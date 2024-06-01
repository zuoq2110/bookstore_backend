package com.example.web_ban_sach.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
@Data
@Entity
@Table(name="the_loai")
public class TheLoai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ma_the_loai")
    private int maTheLoai;

    @Column(name="ten_the_loai")
    private String tenTheLoai;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinTable(name="sach_theloai",
    joinColumns = @JoinColumn(name="ma_the_loai"),
    inverseJoinColumns = @JoinColumn(name="ma_sach"))
    private List<Sach> danhSachQuyenSach;
}
