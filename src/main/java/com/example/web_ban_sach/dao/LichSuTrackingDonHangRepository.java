package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.LichSuTrackingDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuTrackingDonHangRepository extends JpaRepository<LichSuTrackingDonHang, Integer> {
    
    // Lấy tất cả tracking history của một đơn hàng, sắp xếp theo thời gian
    @Query("SELECT lst FROM LichSuTrackingDonHang lst WHERE lst.maDonHang = :maDonHang ORDER BY lst.thoiGian ASC")
    List<LichSuTrackingDonHang> findByMaDonHangOrderByThoiGianAsc(@Param("maDonHang") int maDonHang);
    
    // Lấy tracking entry mới nhất của một đơn hàng
    @Query("SELECT lst FROM LichSuTrackingDonHang lst WHERE lst.maDonHang = :maDonHang ORDER BY lst.thoiGian DESC LIMIT 1")
    LichSuTrackingDonHang findLatestByMaDonHang(@Param("maDonHang") int maDonHang);
    
    // Đếm số lượng tracking entries của một đơn hàng
    long countByMaDonHang(int maDonHang);
}
