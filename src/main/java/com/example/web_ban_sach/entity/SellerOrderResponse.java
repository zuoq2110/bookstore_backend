package com.example.web_ban_sach.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * Response DTO cho Seller Orders API
 * Chỉ chứa thông tin đơn hàng và các items thuộc seller đó
 */
@Data
public class SellerOrderResponse {
    private int maDonHang;
    private Date ngayTao;
    private String diaChiGiaoHang;
    private String tinhTrangDonHang;
    private String maVanDon;
    private String donViVanChuyen;
    private String sdtVanChuyen;
    private String linkTracking;
    private Date thoiGianGiaoDuKien;
    private String ghiChu;
    private double tongTienSanPham;
    private double chiPhiGiaoHang;
    private double chiPhiThanhToan;
    private double tongTien;
    private String soDienThoai;
    private String hoVaTen;
    
    // Chỉ chứa items của seller này
    private List<SellerItemInfo> sellerItems;
    
    // Thông tin người mua
    private NguoiDungInfo nguoiDung;
    
    // Thông tin thanh toán
    private HinhThucThanhToanInfo hinhThucThanhToan;
    
    // Thông tin giao hàng
    private HinhThucGiaoHangInfo hinhThucGiaoHang;
    
    @Data
    public static class SellerItemInfo {
        private int chiTietDonHang;
        private int soLuong;
        private double giaBan;
        private SachInfo sach;
    }
    
    @Data
    public static class SachInfo {
        private int maSach;
        private String tenSach;
        private double giaBan;
        private String tenTacGia;
        private String thumbnail;
    }
    
    @Data
    public static class NguoiDungInfo {
        private int maNguoiDung;
        private String email;
        private String tenDangNhap;
    }
    
    @Data
    public static class HinhThucThanhToanInfo {
        private int maHinhThucThanhToan;
        private String tenHinhThucThanhToan;
    }
    
    @Data
    public static class HinhThucGiaoHangInfo {
        private int maHinhThucGiaoHang;
        private String tenHinhThucGiaoHang;
    }
}
