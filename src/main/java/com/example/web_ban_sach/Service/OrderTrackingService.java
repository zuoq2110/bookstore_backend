package com.example.web_ban_sach.Service;

import org.springframework.http.ResponseEntity;

public interface OrderTrackingService {
    
    /**
     * Lấy thông tin tracking của đơn hàng
     * @param maDonHang ID của đơn hàng
     * @param maNguoiDung ID của người dùng (để kiểm tra quyền)
     * @return ResponseEntity chứa OrderTrackingResponse
     */
    ResponseEntity<?> getOrderTracking(int maDonHang, int maNguoiDung);
    
    /**
     * Cập nhật trạng thái tracking của đơn hàng (Admin/Seller only)
     * @param maDonHang ID của đơn hàng
     * @param trangThai Trạng thái mới
     * @param tieuDe Tiêu đề trạng thái
     * @param moTa Mô tả chi tiết
     * @param viTri Vị trí hiện tại
     * @param nguoiCapNhat Người cập nhật
     * @return ResponseEntity
     */
    ResponseEntity<?> updateOrderTracking(int maDonHang, String trangThai, 
                                         String tieuDe, String moTa, 
                                         String viTri, String nguoiCapNhat);
}
