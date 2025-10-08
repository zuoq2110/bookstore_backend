package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.SellerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRequestRepository extends JpaRepository<SellerRequest, Integer> {
    
    // Tìm yêu cầu theo user và trạng thái
    Optional<SellerRequest> findByMaNguoiDungAndTrangThai(int maNguoiDung, String trangThai);
    
    // Tìm yêu cầu mới nhất của user
    Optional<SellerRequest> findFirstByMaNguoiDungOrderByNgayTaoDesc(int maNguoiDung);
    
    // Lấy danh sách yêu cầu theo trạng thái với pagination
    Page<SellerRequest> findByTrangThai(String trangThai, Pageable pageable);
    
    // Đếm số yêu cầu PENDING
    @Query("SELECT COUNT(sr) FROM SellerRequest sr WHERE sr.trangThai = 'PENDING'")
    long countPendingRequests();
    
    // Kiểm tra user đã có yêu cầu PENDING chưa
    @Query("SELECT COUNT(sr) > 0 FROM SellerRequest sr WHERE sr.maNguoiDung = :userId AND sr.trangThai = 'PENDING'")
    boolean existsPendingRequestByUserId(@Param("userId") int userId);
}
