package com.example.web_ban_sach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerRequestDTO {
    private Integer maNguoiDung;
    private String tenGianHang;
    private String moTaGianHang;
}
