package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.ChiTietDonHang;
import com.example.web_ban_sach.entity.SuDanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@RepositoryRestResource(path = "su-danh-gia")
public interface SuDanhGiaRepository extends JpaRepository<SuDanhGia, Long> {
    public SuDanhGia findByChiTietDonHang(ChiTietDonHang chiTietDonHang);
    public SuDanhGia findByMaDanhGia(long maDanhGia);

    public long countBy();
    
    // Tính rating trung bình của các sách thuộc về seller
    @Query("SELECT COALESCE(AVG(sd.diemXepHang), 0.0) FROM SuDanhGia sd " +
           "JOIN sd.chiTietDonHang ctdh " +
           "JOIN ctdh.sach s " +
           "JOIN SellerBooks sb ON sb.sach.maSach = s.maSach " +
           "WHERE sb.seller.maNguoiDung = :maSeller AND sb.trangThai = 'active'")
    Double getAverageRatingBySeller(@Param("maSeller") int maSeller);
}
