package com.example.web_ban_sach.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_thong_bao")
    private Integer maThongBao;
    
    @Column(name = "loai_thong_bao", nullable = false, length = 50)
    private String loaiThongBao; // SELLER_REQUEST, ORDER_UPDATE, SYSTEM
    
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;
    
    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;
    
    @Column(name = "nguoi_nhan")
    @JsonProperty("nguoiNhan")
    private Integer nguoiNhan; // NULL = tất cả admin
    
    // Alias field for Flutter compatibility
    @JsonProperty("maNguoiDung")
    public Integer getMaNguoiDung() {
        return nguoiNhan;
    }
    
    @Column(name = "da_doc")
    private Boolean daDoc = false;
    
    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
    
    @Column(name = "du_lieu", columnDefinition = "JSON")
    private String duLieu; // JSON string
    
    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
        if (daDoc == null) {
            daDoc = false;
        }
    }
}
