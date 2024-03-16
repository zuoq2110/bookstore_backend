package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
import java.util.List;

@Data
@Entity
@Table(name="nguoi_dung")
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ma_nguoi_dung")
    private int maNguoiDung;

    @Column(name="ho_dem")
    private String hoDem;

    @Column(name="ten")
    private String ten;

    @Column(name="ten_dang_nhap")
    private String tenDangNhap;

    @Column(name="mat_khau")
    private String matKhau;

    @Column(name="gioi_tinh")
    private char gioiTinh;

    @Column(name="email")
    private String email;

    @Column(name="so_dien_thoai")
    private String soDienThoai;

    @Column(name="dia_chi_mua_hang")
    private String diaChiMuaHang;

    @Column(name="dia_chi_giao_hang")
    private String diaChiGiaoHang;
    @Column(name="ngay_sinh")
    private Date ngaySinh;
    @Column(name = "da_kich_hoat", columnDefinition = "default false")
    private boolean daKichHoat;
    @Column(name = "ma_kich_hoat")
    private String maKichHoat;
    @Column(name = "avatar", columnDefinition = "LONGTEXT")
    @Lob
    private String avatar;

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<SuDanhGia> danhSachSuDanhGia;

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SachYeuThich> danhSachSachYeuThich;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinTable(name="nguoidung_quyen",
            joinColumns = @JoinColumn(name="ma_nguoi_dung"),
    inverseJoinColumns = @JoinColumn(name="ma_quyen"))
    private List<Quyen> danhSachQuyen;

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    private List<DonHang> danhSachDonhang;

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<GioHang> danhSachGioHang;
}
