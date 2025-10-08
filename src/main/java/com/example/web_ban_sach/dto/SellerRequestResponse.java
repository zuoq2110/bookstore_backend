package com.example.web_ban_sach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerRequestResponse {
    private Integer maYeuCau;
    private Integer maNguoiDung;
    private String tenGianHang;
    private String moTaGianHang;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayXuLy;
    private Integer nguoiXuLy;
    private String lyDoTuChoi;
    
    // Thông tin user
    private String tenNguoiDung;
    private String email;
    private String soDienThoai;
    private String avatar;
}
