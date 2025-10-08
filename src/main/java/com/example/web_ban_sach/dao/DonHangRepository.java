package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.DonHang;
import com.example.web_ban_sach.entity.NguoiDung;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@RepositoryRestResource(path = "don-hang")
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {
    public DonHang findByMaDonHang(int maDonHang);

    public long countBy();
    
    // Phân trang đơn hàng theo người dùng
    Page<DonHang> findByNguoiDung(NguoiDung nguoiDung, Pageable pageable);
    
    // Phân trang đơn hàng theo mã người dùng
    Page<DonHang> findByNguoiDungMaNguoiDung(int maNguoiDung, Pageable pageable);
    
    // Đếm số đơn hàng của người dùng
    long countByNguoiDungMaNguoiDung(int maNguoiDung);
    
    /**
     * Lấy danh sách đơn hàng có chứa sách của seller
     * Query sẽ tìm tất cả đơn hàng có ít nhất 1 sản phẩm thuộc seller
     */
    @Query("SELECT DISTINCT dh FROM DonHang dh " +
           "JOIN dh.danhSachChiTietDonHang ctdh " +
           "JOIN SellerBooks sb ON sb.sach.maSach = ctdh.sach.maSach " +
           "WHERE sb.seller.maNguoiDung = :sellerId")
    Page<DonHang> findOrdersBySeller(@Param("sellerId") int sellerId, Pageable pageable);
    
    /**
     * Đếm số đơn hàng có chứa sách của seller
     */
    @Query("SELECT COUNT(DISTINCT dh) FROM DonHang dh " +
           "JOIN dh.danhSachChiTietDonHang ctdh " +
           "JOIN SellerBooks sb ON sb.sach.maSach = ctdh.sach.maSach " +
           "WHERE sb.seller.maNguoiDung = :sellerId")
    long countOrdersBySeller(@Param("sellerId") int sellerId);
}
