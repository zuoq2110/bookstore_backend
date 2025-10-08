package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.OrderTrackingService;
import com.example.web_ban_sach.dao.DonHangRepository;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.DonHang;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.ThongBao;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/don-hang")
@CrossOrigin(origins = "*")
public class OrderTrackingController {
    
    @Autowired
    private OrderTrackingService orderTrackingService;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private DonHangRepository donHangRepository;
    
    /**
     * GET /don-hang/{orderId}/tracking
     * Lấy thông tin tracking của đơn hàng
     * 
     * @param orderId ID của đơn hàng
     * @return ResponseEntity với OrderTrackingResponse
     */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getOrderTracking(@PathVariable int orderId) {
        try {
            // Endpoint này PROTECTED, nên authentication luôn có (đã qua JWT Filter)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // authentication.getName() trả về username (String), không phải maNguoiDung
            String username = authentication.getName();
            
            // Lấy user từ database để có maNguoiDung
            NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(username);
            if (nguoiDung == null) {
                return ResponseEntity.badRequest()
                    .body(new ThongBao("Người dùng không tồn tại"));
            }
            
            int maNguoiDung = nguoiDung.getMaNguoiDung();
            
            return orderTrackingService.getOrderTracking(orderId, maNguoiDung);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * POST /orders/{orderId}/tracking/update
     * Cập nhật trạng thái tracking (Admin/Seller only)
     * 
     * Request body:
     * {
     *   "status": "shipping",
     *   "title": "Đang giao hàng",
     *   "description": "Đơn hàng đang được vận chuyển",
     *   "location": "TP.HCM",
     *   "note": "Ghi chú thêm"
     * }
     * 
     * @param orderId ID của đơn hàng
     * @param request JsonNode chứa thông tin tracking
     * @return ResponseEntity
     */
    @PostMapping("/{orderId}/tracking/update")
    public ResponseEntity<?> updateOrderTracking(
            @PathVariable int orderId,
            @RequestBody JsonNode request) {
        try {
            // TODO: Kiểm tra quyền Admin/Seller
            // Tạm thời bỏ qua check quyền để test
            
            // Parse request body
            String trangThai = request.get("status").asText();
            String tieuDe = request.get("title").asText();
            String moTa = request.has("description") ? request.get("description").asText() : "";
            String viTri = request.has("location") ? request.get("location").asText() : "";
            
            // Lấy tên người cập nhật từ Security Context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String nguoiCapNhat = authentication.getName();
            
            return orderTrackingService.updateOrderTracking(
                orderId, trangThai, tieuDe, moTa, viTri, nguoiCapNhat);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /don-hang/{orderId}/tracking-info
     * Cập nhật thông tin vận chuyển (tracking number, carrier, etc.)
     * Admin/Seller only
     * 
     * Request body:
     * {
     *   "trackingNumber": "VNP123456789",
     *   "carrier": "Viettel Post",
     *   "carrierPhone": "1900-8095",
     *   "trackingUrl": "https://viettelpost.com.vn/tracking/VNP123456789",
     *   "estimatedDelivery": "2024-10-10T14:00:00Z"
     * }
     * 
     * @param orderId ID của đơn hàng
     * @param request JsonNode chứa thông tin vận chuyển
     * @return ResponseEntity
     */
    @PutMapping("/{orderId}/tracking-info")
    public ResponseEntity<?> updateTrackingInfo(
            @PathVariable int orderId,
            @RequestBody JsonNode request) {
        try {
            // TODO: Kiểm tra quyền Admin/Seller
            // Get authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            // Lấy đơn hàng
            DonHang donHang = donHangRepository.findByMaDonHang(orderId);
            if (donHang == null) {
                return ResponseEntity.badRequest()
                    .body(new ThongBao("Không tìm thấy đơn hàng"));
            }
            
            // Cập nhật thông tin vận chuyển
            if (request.has("trackingNumber")) {
                donHang.setMaVanDon(request.get("trackingNumber").asText());
            }
            if (request.has("carrier")) {
                donHang.setDonViVanChuyen(request.get("carrier").asText());
            }
            if (request.has("carrierPhone")) {
                donHang.setSdtVanChuyen(request.get("carrierPhone").asText());
            }
            if (request.has("trackingUrl")) {
                donHang.setLinkTracking(request.get("trackingUrl").asText());
            }
            if (request.has("estimatedDelivery")) {
                // Parse ISO 8601 date
                String dateStr = request.get("estimatedDelivery").asText();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date estimatedDate = sdf.parse(dateStr.replace("Z", ""));
                    donHang.setThoiGianGiaoDuKien(estimatedDate);
                } catch (Exception e) {
                    // Ignore invalid date format
                }
            }
            
            // Lưu vào database
            donHangRepository.save(donHang);
            
            return ResponseEntity.ok(new ThongBao("Cập nhật thông tin vận chuyển thành công"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
}
