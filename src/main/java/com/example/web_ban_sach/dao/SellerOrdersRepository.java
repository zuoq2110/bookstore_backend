package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.SellerOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface SellerOrdersRepository extends JpaRepository<SellerOrders, Integer> {
    
    // Đếm tổng số đơn hàng của seller
    long countBySellerMaNguoiDung(int maSeller);
    
    // Tính tổng doanh thu của seller
    @Query("SELECT COALESCE(SUM(so.tongTien), 0.0) FROM SellerOrders so WHERE so.seller.maNguoiDung = :maSeller")
    Double sumTongTienBySellerMaNguoiDung(@Param("maSeller") int maSeller);
    
    // Lấy doanh thu theo thời gian
    @Query("SELECT COALESCE(SUM(so.tongTien), 0.0) FROM SellerOrders so WHERE so.seller.maNguoiDung = :maSeller AND so.ngayDat BETWEEN :startDate AND :endDate")
    Double sumTongTienBySellerAndDateRange(@Param("maSeller") int maSeller, 
                                           @Param("startDate") Timestamp startDate, 
                                           @Param("endDate") Timestamp endDate);
    
    // Đếm đơn hàng theo thời gian
    @Query("SELECT COUNT(so) FROM SellerOrders so WHERE so.seller.maNguoiDung = :maSeller AND so.ngayDat BETWEEN :startDate AND :endDate")
    long countBySellerAndDateRange(@Param("maSeller") int maSeller, 
                                   @Param("startDate") Timestamp startDate, 
                                   @Param("endDate") Timestamp endDate);
    
    // Lấy top sách bán chạy
    @Query("SELECT so.sach.maSach, so.sach.tenSach, SUM(so.soLuong) as totalSold, SUM(so.tongTien) as totalRevenue " +
           "FROM SellerOrders so WHERE so.seller.maNguoiDung = :maSeller " +
           "GROUP BY so.sach.maSach, so.sach.tenSach " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingBooks(@Param("maSeller") int maSeller);
    
    // Đếm số khách hàng unique
    @Query("SELECT COUNT(DISTINCT so.maDonHang) FROM SellerOrders so WHERE so.seller.maNguoiDung = :maSeller")
    long countUniqueCustomers(@Param("maSeller") int maSeller);
    
    // Tổng số sách đã bán
    @Query("SELECT COALESCE(SUM(so.soLuong), 0) FROM SellerOrders so WHERE so.seller.maNguoiDung = :maSeller")
    long sumTotalBooksSold(@Param("maSeller") int maSeller);
    
    // Lấy doanh thu theo từng ngày
    @Query("SELECT DATE(so.ngayDat) as date, COALESCE(SUM(so.tongTien), 0.0) as revenue " +
           "FROM SellerOrders so " +
           "WHERE so.seller.maNguoiDung = :maSeller AND so.ngayDat BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(so.ngayDat) " +
           "ORDER BY DATE(so.ngayDat) ASC")
    List<Object[]> getRevenueByDate(@Param("maSeller") int maSeller,
                                    @Param("startDate") Timestamp startDate,
                                    @Param("endDate") Timestamp endDate);
}
