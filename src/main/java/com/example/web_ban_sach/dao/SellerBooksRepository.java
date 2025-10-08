package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.SellerBooks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerBooksRepository extends JpaRepository<SellerBooks, Integer> {
    
    // Tìm tất cả sách của seller (with JOIN FETCH to avoid lazy loading)
    @Query("SELECT sb FROM SellerBooks sb JOIN FETCH sb.sach JOIN FETCH sb.seller WHERE sb.seller.maNguoiDung = :maSeller AND sb.trangThai != 'deleted' ORDER BY sb.ngayDang DESC")
    List<SellerBooks> findBySellerMaNguoiDungWithSach(@Param("maSeller") int maSeller);
    
    // Tìm sách của seller theo status (with JOIN FETCH)
    @Query("SELECT sb FROM SellerBooks sb JOIN FETCH sb.sach JOIN FETCH sb.seller WHERE sb.seller.maNguoiDung = :maSeller AND sb.trangThai = :trangThai ORDER BY sb.ngayDang DESC")
    List<SellerBooks> findBySellerMaNguoiDungAndTrangThaiWithSach(@Param("maSeller") int maSeller, @Param("trangThai") String trangThai);
    
    // Count queries (no need for JOIN)
    @Query("SELECT COUNT(sb) FROM SellerBooks sb WHERE sb.seller.maNguoiDung = :maSeller AND sb.trangThai != 'deleted'")
    long countBySellerMaNguoiDungActive(@Param("maSeller") int maSeller);
    
    // Đếm số lượng sách của seller
    long countBySellerMaNguoiDung(int maSeller);
    
    // Kiểm tra seller có sở hữu sách này không
    @Query("SELECT CASE WHEN COUNT(sb) > 0 THEN true ELSE false END FROM SellerBooks sb WHERE sb.seller.maNguoiDung = :maSeller AND sb.sach.maSach = :maSach AND sb.trangThai != 'deleted'")
    boolean existsBySellerMaNguoiDungAndSachMaSach(@Param("maSeller") int maSeller, @Param("maSach") int maSach);
    
    // Tìm một sách cụ thể của seller (with JOIN FETCH)
    @Query("SELECT sb FROM SellerBooks sb JOIN FETCH sb.sach JOIN FETCH sb.seller WHERE sb.seller.maNguoiDung = :maSeller AND sb.sach.maSach = :maSach AND sb.trangThai != 'deleted'")
    Optional<SellerBooks> findBySellerMaNguoiDungAndSachMaSach(@Param("maSeller") int maSeller, @Param("maSach") int maSach);
    
    // Xóa sách của seller (soft delete) - with JOIN FETCH
    @Query("SELECT sb FROM SellerBooks sb JOIN FETCH sb.sach JOIN FETCH sb.seller WHERE sb.seller.maNguoiDung = :maSeller AND sb.id = :id AND sb.trangThai != 'deleted'")
    Optional<SellerBooks> findBySellerAndId(@Param("maSeller") int maSeller, @Param("id") int id);
    
    // Tìm seller book theo mã sách (để lấy thông tin seller cho API công khai)
    @Query("SELECT sb FROM SellerBooks sb JOIN FETCH sb.seller WHERE sb.sach.maSach = :maSach AND sb.trangThai = 'active' ORDER BY sb.ngayDang DESC")
    Optional<SellerBooks> findFirstBySachMaSachAndActive(@Param("maSach") int maSach);
}
