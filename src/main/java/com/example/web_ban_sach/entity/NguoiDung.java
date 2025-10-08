package com.example.web_ban_sach.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
import java.util.List;

@Data
@Entity
@Table(name="nguoi_dung")
@JsonIgnoreProperties({"danhSachDonHang", "danhSachGioHang", "matKhau"})
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

    @Column(name="gioi_tinh", length = 10)
    private String gioiTinh;

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
    @Column(name = "da_kich_hoat")
    private boolean daKichHoat;
    @Column(name = "ma_kich_hoat")
    private String maKichHoat;
    @Column(name = "avatar", columnDefinition = "LONGTEXT")
    @Lob
    private String avatar;

    @Column(name = "is_seller")
    @JsonProperty("isSeller")
    private boolean isSeller = false;

    @Column(name = "ten_gian_hang")
    private String tenGianHang;

    @Column(name = "mo_ta_gian_hang", columnDefinition = "TEXT")
    private String moTaGianHang;
    
    // === THÔNG TIN CỬA HÀNG BỔ SUNG ===
    
    @Column(name = "dia_chi_gian_hang", length = 500)
    private String diaChiGianHang;
    
    @Column(name = "vi_do_gian_hang")
    private Double viDoGianHang;
    
    @Column(name = "kinh_do_gian_hang")
    private Double kinhDoGianHang;
    
    @Column(name = "so_dien_thoai_gian_hang", length = 15)
    private String soDienThoaiGianHang;

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
    private List<DonHang> danhSachDonHang;

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<GioHang> danhSachGioHang;
}
