package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.DonHangRepository;
import com.example.web_ban_sach.dao.LichSuTrackingDonHangRepository;
import com.example.web_ban_sach.dao.SellerBooksRepository;
import com.example.web_ban_sach.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Service
public class OrderTrackingServiceImpl implements OrderTrackingService {
    
    @Autowired
    private DonHangRepository donHangRepository;
    
    @Autowired
    private LichSuTrackingDonHangRepository lichSuTrackingRepository;
    
    @Autowired
    private SellerBooksRepository sellerBooksRepository;
    
    @Override
    public ResponseEntity<?> getOrderTracking(int maDonHang, int maNguoiDung) {
        try {
            // Kiểm tra đơn hàng tồn tại
            DonHang donHang = donHangRepository.findByMaDonHang(maDonHang);
            if (donHang == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(OrderTrackingResponse.error("ORDER_NOT_FOUND", 
                                                      "Không tìm thấy đơn hàng"));
            }
            
            // Kiểm tra quyền truy cập (user chỉ xem đơn của mình)
            if (donHang.getNguoiDung().getMaNguoiDung() != maNguoiDung) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(OrderTrackingResponse.error("ACCESS_DENIED", 
                                                      "Bạn không có quyền xem đơn hàng này"));
            }
            
            // Lấy tracking history
            List<LichSuTrackingDonHang> trackingHistory = 
                lichSuTrackingRepository.findByMaDonHangOrderByThoiGianAsc(maDonHang);
            
            // Tạo timeline
            List<OrderTrackingResponse.TimelineEntry> timeline = new ArrayList<>();
            String currentStatus = donHang.getTinhTrangDonHang();
            
            // Map các trạng thái
            String[] allStatuses = {"pending", "confirmed", "processing", "shipping", "delivered"};
            String[] statusTitles = {
                "Đơn hàng đã đặt", 
                "Đã xác nhận", 
                "Đang chuẩn bị hàng", 
                "Đang giao hàng", 
                "Đã giao hàng"
            };
            
            for (int i = 0; i < allStatuses.length; i++) {
                String status = allStatuses[i];
                
                // Tìm tracking entry cho trạng thái này
                LichSuTrackingDonHang entry = trackingHistory.stream()
                    .filter(t -> matchStatus(t.getTrangThai(), status))
                    .findFirst()
                    .orElse(null);
                
                OrderTrackingResponse.TimelineEntry timelineEntry = 
                    new OrderTrackingResponse.TimelineEntry();
                timelineEntry.setStatus(status);
                timelineEntry.setTitle(entry != null ? entry.getTieuDe() : statusTitles[i]);
                timelineEntry.setDescription(entry != null ? entry.getMoTa() : "");
                timelineEntry.setTimestamp(entry != null ? formatTimestamp(entry.getThoiGian()) : null);
                timelineEntry.setCompleted(entry != null);
                timelineEntry.setLocation(entry != null ? entry.getViTri() : null);
                timelineEntry.setUpdatedBy(entry != null ? entry.getNguoiCapNhat() : null);
                
                timeline.add(timelineEntry);
            }
            
            // Tạo response data
            OrderTrackingResponse.OrderTrackingData data = 
                new OrderTrackingResponse.OrderTrackingData();
            data.setOrderId(donHang.getMaDonHang());
            data.setCurrentStatus(mapStatusToEnglish(currentStatus));
            data.setTrackingNumber(donHang.getMaVanDon());
            data.setCarrier(donHang.getDonViVanChuyen());
            data.setCarrierPhone(donHang.getSdtVanChuyen());
            data.setTrackingUrl(donHang.getLinkTracking());
            data.setEstimatedDelivery(donHang.getThoiGianGiaoDuKien() != null ? 
                                     formatTimestamp(new Timestamp(donHang.getThoiGianGiaoDuKien().getTime())) : null);
            data.setTimeline(timeline);
            
            // Shipping address
            OrderTrackingResponse.ShippingAddress address = 
                new OrderTrackingResponse.ShippingAddress();
            address.setFullName(donHang.getHoVaTen());
            address.setPhone(donHang.getSoDienThoai());
            address.setAddress(donHang.getDiaChiGiaoHang());
            data.setShippingAddress(address);
            
            // Order summary
            OrderTrackingResponse.OrderSummary summary = 
                new OrderTrackingResponse.OrderSummary();
            summary.setTotalItems(donHang.getDanhSachChiTietDonHang() != null ? 
                                 donHang.getDanhSachChiTietDonHang().size() : 0);
            summary.setTotalAmount(donHang.getTongTien());
            summary.setPaymentMethod(donHang.getHinhThucThanhToan() != null ? 
                                    donHang.getHinhThucThanhToan().getTenHinhThucThanhToan() : "COD");
            data.setOrderSummary(summary);
            
            // Seller info - Lấy từ sách đầu tiên trong đơn hàng
            OrderTrackingResponse.SellerInfo sellerInfo = null;
            if (donHang.getDanhSachChiTietDonHang() != null && !donHang.getDanhSachChiTietDonHang().isEmpty()) {
                try {
                    // Lấy sách đầu tiên
                    ChiTietDonHang firstItem = donHang.getDanhSachChiTietDonHang().get(0);
                    int maSach = firstItem.getSach().getMaSach();
                    
                    // Tìm seller của sách này
                    var sellerBookOpt = sellerBooksRepository.findFirstBySachMaSachAndActive(maSach);
                    if (sellerBookOpt.isPresent()) {
                        NguoiDung seller = sellerBookOpt.get().getSeller();
                        sellerInfo = new OrderTrackingResponse.SellerInfo();
                        sellerInfo.setTenGianHang(seller.getTenGianHang());
                        sellerInfo.setDiaChiGianHang(seller.getDiaChiGianHang());
                        sellerInfo.setViDoGianHang(seller.getViDoGianHang());
                        sellerInfo.setKinhDoGianHang(seller.getKinhDoGianHang());
                        sellerInfo.setSoDienThoaiGianHang(seller.getSoDienThoaiGianHang());
                    }
                } catch (Exception e) {
                    // Nếu lỗi khi lấy seller info, bỏ qua (sellerInfo = null)
                    e.printStackTrace();
                }
            }
            data.setSellerInfo(sellerInfo);
            
            return ResponseEntity.ok(OrderTrackingResponse.success(data));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OrderTrackingResponse.error("INTERNAL_ERROR", 
                                                  "Lỗi khi lấy thông tin tracking: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> updateOrderTracking(int maDonHang, String trangThai, 
                                                String tieuDe, String moTa, 
                                                String viTri, String nguoiCapNhat) {
        try {
            // Kiểm tra đơn hàng tồn tại
            DonHang donHang = donHangRepository.findByMaDonHang(maDonHang);
            if (donHang == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(OrderTrackingResponse.error("ORDER_NOT_FOUND", 
                                                      "Không tìm thấy đơn hàng"));
            }
            
            // Tạo tracking entry mới
            LichSuTrackingDonHang tracking = new LichSuTrackingDonHang();
            tracking.setMaDonHang(maDonHang);
            tracking.setTrangThai(trangThai);
            tracking.setTieuDe(tieuDe);
            tracking.setMoTa(moTa);
            tracking.setViTri(viTri);
            tracking.setNguoiCapNhat(nguoiCapNhat);
            tracking.setThoiGian(new Timestamp(System.currentTimeMillis()));
            
            lichSuTrackingRepository.save(tracking);
            
            // Cập nhật trạng thái đơn hàng
            donHang.setTinhTrangDonHang(trangThai);
            donHangRepository.save(donHang);
            
            return ResponseEntity.ok(new ThongBao("Cập nhật trạng thái tracking thành công"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OrderTrackingResponse.error("INTERNAL_ERROR", 
                                                  "Lỗi khi cập nhật tracking: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private String formatTimestamp(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(timestamp);
    }
    
    private boolean matchStatus(String dbStatus, String apiStatus) {
        // Map trạng thái tiếng Việt sang tiếng Anh
        return mapStatusToEnglish(dbStatus).equalsIgnoreCase(apiStatus);
    }
    
    private String mapStatusToEnglish(String vietnameseStatus) {
        if (vietnameseStatus == null) return "pending";
        
        switch (vietnameseStatus.toLowerCase()) {
            case "đang xử lý":
            case "pending":
                return "pending";
            case "đã xác nhận":
            case "confirmed":
                return "confirmed";
            case "đang chuẩn bị":
            case "processing":
                return "processing";
            case "đang giao":
            case "shipping":
                return "shipping";
            case "đã giao":
            case "delivered":
                return "delivered";
            case "đã hủy":
            case "cancelled":
                return "cancelled";
            default:
                return "pending";
        }
    }
}
