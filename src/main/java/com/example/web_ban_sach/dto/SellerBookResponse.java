package com.example.web_ban_sach.dto;

import com.example.web_ban_sach.entity.SellerBooks;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class SellerBookResponse {
    private int id;
    private int maSach;
    private String tenSach;
    private String tenTacGia;
    private String moTa;
    private double giaBan;
    private double giaNhap;
    private int soLuongKho;
    private String trangThai;
    private Timestamp ngayDang;
    private Timestamp ngayCapNhat;
    private String coverImageUrl; // Ảnh bìa
    
    // Thông tin seller
    private Integer sellerId;
    private String sellerName;
    
    // Constructor từ SellerBooks entity
    public SellerBookResponse(SellerBooks sellerBook) {
        this.id = sellerBook.getId();
        this.maSach = sellerBook.getSach().getMaSach();
        this.tenSach = sellerBook.getSach().getTenSach();
        this.tenTacGia = sellerBook.getSach().getTenTacGia();
        this.moTa = sellerBook.getSach().getMoTa();
        this.giaBan = sellerBook.getSach().getGiaBan();
        this.giaNhap = sellerBook.getGiaNhap();
        this.soLuongKho = sellerBook.getSoLuongKho();
        this.trangThai = sellerBook.getTrangThai();
        this.ngayDang = sellerBook.getNgayDang();
        this.ngayCapNhat = sellerBook.getNgayCapNhat();
        
        // Thông tin seller
        if (sellerBook.getSeller() != null) {
            this.sellerId = sellerBook.getSeller().getMaNguoiDung();
            this.sellerName = sellerBook.getSeller().getTenGianHang() != null 
                ? sellerBook.getSeller().getTenGianHang() 
                : sellerBook.getSeller().getHoDem() + " " + sellerBook.getSeller().getTen();
        }
        
        // Lấy ảnh bìa (icon) đầu tiên
        if (sellerBook.getSach().getDanhSachHinhAnh() != null && !sellerBook.getSach().getDanhSachHinhAnh().isEmpty()) {
            this.coverImageUrl = sellerBook.getSach().getDanhSachHinhAnh().stream()
                    .filter(ha -> ha.isLaIcon()) // boolean getter là isLaIcon() không phải getLaIcon()
                    .findFirst()
                    .map(ha -> ha.getDuongDan())
                    .orElse(null);
        }
    }
}
