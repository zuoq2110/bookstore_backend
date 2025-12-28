package com.example.web_ban_sach.dto;

import com.example.web_ban_sach.entity.Sach;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO đơn giản hóa cho cache - chỉ chứa dữ liệu cần thiết
 * Giảm overhead serialization/deserialization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SachCacheDTO implements Serializable {
    private int maSach;
    private String tenSach;
    private String tenTacGia;
    private String ISBN;
    private String moTa;
    private double giaNiemYet;
    private double giaBan;
    private int soLuong;
    private Double trungBinhXepHang;
    private int soLuongDaBan;
    private int giamGia;
    
    // Simplified relationships - chỉ lưu IDs và tên
    private List<Integer> theLoaiIds;
    private List<String> theLoaiNames;
    private String hinhAnhChinh; // URL hình ảnh chính
    
    /**
     * Convert từ Entity sang DTO
     */
    public static SachCacheDTO fromEntity(Sach sach) {
        SachCacheDTO dto = new SachCacheDTO();
        dto.setMaSach(sach.getMaSach());
        dto.setTenSach(sach.getTenSach());
        dto.setTenTacGia(sach.getTenTacGia());
        dto.setISBN(sach.getISBN());
        dto.setMoTa(sach.getMoTa());
        dto.setGiaNiemYet(sach.getGiaNiemYet());
        dto.setGiaBan(sach.getGiaBan());
        dto.setSoLuong(sach.getSoLuong());
        dto.setTrungBinhXepHang(sach.getTrungBinhXepHang());
        dto.setSoLuongDaBan(sach.getSoLuongDaBan());
        dto.setGiamGia(sach.getGiamGia());
        
        // Simplified relationships
        if (sach.getDanhSachTheLoai() != null) {
            dto.setTheLoaiIds(sach.getDanhSachTheLoai().stream()
                .map(tl -> tl.getMaTheLoai())
                .collect(Collectors.toList()));
            dto.setTheLoaiNames(sach.getDanhSachTheLoai().stream()
                .map(tl -> tl.getTenTheLoai())
                .collect(Collectors.toList()));
        }
        
        // Chỉ lưu URL hình ảnh chính
        if (sach.getDanhSachHinhAnh() != null && !sach.getDanhSachHinhAnh().isEmpty()) {
            dto.setHinhAnhChinh(sach.getDanhSachHinhAnh().get(0).getDuongDan());
        }
        
        return dto;
    }
}
