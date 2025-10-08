package com.example.web_ban_sach.dto;

import com.example.web_ban_sach.entity.Sach;
import com.example.web_ban_sach.entity.SellerBooks;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class SachWithSellerResponse {
    private int maSach;
    private String tenSach;
    private String tenTacGia;
    private String isbn;
    private String moTa;
    private double giaNiemYet;
    private double giaBan;
    private int soLuong;
    private Double trungBinhXepHang;
    private int soLuongDaBan;
    private int giamGia;
    private String coverImageUrl;
    
    // Thông tin seller
    private Integer sellerId;
    private String sellerName;
    
    // Constructor từ Sach entity
    public SachWithSellerResponse(Sach sach) {
        this.maSach = sach.getMaSach();
        this.tenSach = sach.getTenSach();
        this.tenTacGia = sach.getTenTacGia();
        this.isbn = sach.getISBN();
        this.moTa = sach.getMoTa();
        this.giaNiemYet = sach.getGiaNiemYet();
        this.giaBan = sach.getGiaBan();
        this.soLuong = sach.getSoLuong();
        this.trungBinhXepHang = sach.getTrungBinhXepHang();
        this.soLuongDaBan = sach.getSoLuongDaBan();
        this.giamGia = sach.getGiamGia();
        
        // Lấy ảnh bìa
        if (sach.getDanhSachHinhAnh() != null && !sach.getDanhSachHinhAnh().isEmpty()) {
            this.coverImageUrl = sach.getDanhSachHinhAnh().stream()
                    .filter(ha -> ha.isLaIcon())
                    .findFirst()
                    .map(ha -> ha.getDuongDan())
                    .orElse(null);
        }
    }
    
    // Constructor với thông tin seller từ SellerBooks
    public SachWithSellerResponse(Sach sach, SellerBooks sellerBooks) {
        this(sach); // Gọi constructor trên
        
        if (sellerBooks != null && sellerBooks.getSeller() != null) {
            this.sellerId = sellerBooks.getSeller().getMaNguoiDung();
            this.sellerName = sellerBooks.getSeller().getTenGianHang() != null 
                ? sellerBooks.getSeller().getTenGianHang() 
                : sellerBooks.getSeller().getHoDem() + " " + sellerBooks.getSeller().getTen();
        }
    }
}
