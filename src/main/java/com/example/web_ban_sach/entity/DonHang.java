package com.example.web_ban_sach.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "don_hang")
public class DonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_don_hang")
    private int maDonHang;

    @Column(name = "ngay_tao")
    private Date ngayTao;

    @Column(name = "dia_chi_giao_hang")
    private String diaChiGiaoHang;

    @Column(name = "tinh_trang_don_hang")
    private String tinhTrangDonHang;

    @Column(name = "ma_van_don")
    private String maVanDon;

    @Column(name = "don_vi_van_chuyen")
    private String donViVanChuyen;

    @Column(name = "sdt_van_chuyen")
    private String sdtVanChuyen;

    @Column(name = "link_tracking", columnDefinition = "TEXT")
    private String linkTracking;

    @Column(name = "thoi_gian_giao_du_kien")
    private Date thoiGianGiaoDuKien;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "tong_tien_san_pham")
    private double tongTienSanPham;

    @Column(name = "chi_phi_giao_hang")
    private double chiPhiGiaoHang;

    @Column(name = "chi_phi_thanh_toan")
    private double chiPhiThanhToan;

    @Column(name = "tong_tien")
    private double tongTien;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "ho_va_ten")
    private String hoVaTen;

    @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ChiTietDonHang> danhSachChiTietDonHang;

    @ManyToOne(cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "ma_nguoi_dung", nullable = false)
    @JsonIgnoreProperties({"danhSachDonHang", "danhSachSuDanhGia", "danhSachSachYeuThich", "danhSachQuyen", "matKhau"})
    private NguoiDung nguoiDung;

    @ManyToOne(cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "ma_hinh_thuc_thanh_toan")
    private HinhThucThanhToan hinhThucThanhToan;

    @ManyToOne(cascade = {
            CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "ma_hinh_thuc_giao_hang")
    private HinhThucGiaoHang hinhThucGiaoHang;
}
