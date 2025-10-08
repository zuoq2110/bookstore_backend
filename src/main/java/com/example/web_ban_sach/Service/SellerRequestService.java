package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.NotificationRepository;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.SellerRequestRepository;
import com.example.web_ban_sach.dto.SellerRequestDTO;
import com.example.web_ban_sach.dto.SellerRequestResponse;
import com.example.web_ban_sach.entity.NotificationMessage;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.SellerRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SellerRequestService {
    
    @Autowired
    private SellerRequestRepository sellerRequestRepository;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private SseEmitterService sseEmitterService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Submit yêu cầu đăng ký seller
     */
    @Transactional
    public Map<String, Object> submitSellerRequest(SellerRequestDTO dto) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Kiểm tra user tồn tại
        NguoiDung user = nguoiDungRepository.findByMaNguoiDung(dto.getMaNguoiDung());
        if (user == null) {
            response.put("success", false);
            response.put("error", "User không tồn tại");
            return response;
        }
        
        // 2. Kiểm tra user đã là seller chưa
        if (user.isSeller()) {
            response.put("success", false);
            response.put("error", "Bạn đã là seller rồi");
            return response;
        }
        
        // 3. Kiểm tra đã có yêu cầu PENDING chưa
        if (sellerRequestRepository.existsPendingRequestByUserId(dto.getMaNguoiDung())) {
            response.put("success", false);
            response.put("error", "Bạn đã có yêu cầu đang chờ xử lý");
            return response;
        }
        
        // 4. Tạo yêu cầu mới
        SellerRequest request = new SellerRequest();
        request.setMaNguoiDung(dto.getMaNguoiDung());
        request.setTenGianHang(dto.getTenGianHang());
        request.setMoTaGianHang(dto.getMoTaGianHang());
        request.setTrangThai("PENDING");
        request.setNgayTao(LocalDateTime.now());
        
        sellerRequestRepository.save(request);
        
        // 5. Tạo thông báo cho admin
        try {
            NotificationMessage NotificationMessage = new NotificationMessage();
            NotificationMessage.setLoaiThongBao("SELLER_REQUEST");
            NotificationMessage.setTieuDe("Yêu cầu đăng ký Seller mới");
            NotificationMessage.setNoiDung(String.format(
                "%s (%s) đã gửi yêu cầu đăng ký làm seller với tên gian hàng: %s",
                (user.getHoDem() != null ? user.getHoDem() + " " : "") + user.getTen(),
                user.getEmail(),
                dto.getTenGianHang()
            ));
            NotificationMessage.setNguoiNhan(null); // Gửi cho tất cả admin
            NotificationMessage.setDaDoc(false);
            NotificationMessage.setNgayTao(LocalDateTime.now());
            
            // Set JSON data
            Map<String, Object> duLieu = new HashMap<>();
            duLieu.put("requestId", request.getMaYeuCau());
            duLieu.put("userId", user.getMaNguoiDung());
            duLieu.put("shopName", dto.getTenGianHang());
            duLieu.put("userEmail", user.getEmail());
            NotificationMessage.setDuLieu(objectMapper.writeValueAsString(duLieu));
            
            notificationRepository.save(NotificationMessage);
            
            // 6. Broadcast thông báo qua SSE
            sseEmitterService.broadcastToAdmins(NotificationMessage);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Không fail transaction nếu gửi NotificationMessage lỗi
        }
        
        response.put("success", true);
        response.put("message", "Đã gửi yêu cầu thành công. Vui lòng chờ admin phê duyệt.");
        response.put("requestId", request.getMaYeuCau());
        
        return response;
    }
    
    /**
     * Lấy danh sách yêu cầu (Admin)
     */
    public Page<SellerRequestResponse> getSellerRequests(String status, Pageable pageable) {
        Page<SellerRequest> requests;
        
        if (status != null && !status.isEmpty()) {
            requests = sellerRequestRepository.findByTrangThai(status, pageable);
        } else {
            requests = sellerRequestRepository.findAll(pageable);
        }
        
        // Map to response DTO with user info
        return requests.map(req -> {
            NguoiDung user = nguoiDungRepository.findByMaNguoiDung(req.getMaNguoiDung());
            
            return SellerRequestResponse.builder()
                .maYeuCau(req.getMaYeuCau())
                .maNguoiDung(req.getMaNguoiDung())
                .tenGianHang(req.getTenGianHang())
                .moTaGianHang(req.getMoTaGianHang())
                .trangThai(req.getTrangThai())
                .ngayTao(req.getNgayTao())
                .ngayXuLy(req.getNgayXuLy())
                .nguoiXuLy(req.getNguoiXuLy())
                .lyDoTuChoi(req.getLyDoTuChoi())
                .tenNguoiDung(user != null ? (user.getHoDem() != null ? user.getHoDem() + " " : "") + user.getTen() : null)
                .email(user != null ? user.getEmail() : null)
                .soDienThoai(user != null ? user.getSoDienThoai() : null)
                .avatar(user != null ? user.getAvatar() : null)
                .build();
        });
    }
    
    /**
     * Phê duyệt yêu cầu (Admin)
     */
    @Transactional
    public Map<String, Object> approveSellerRequest(int requestId, int adminId) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Lấy yêu cầu
        Optional<SellerRequest> requestOpt = sellerRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            response.put("success", false);
            response.put("error", "Request không tồn tại");
            return response;
        }
        
        SellerRequest request = requestOpt.get();
        
        if (!"PENDING".equals(request.getTrangThai())) {
            response.put("success", false);
            response.put("error", "Request đã được xử lý rồi");
            return response;
        }
        
        // 2. Cập nhật user thành seller
        NguoiDung user = nguoiDungRepository.findByMaNguoiDung(request.getMaNguoiDung());
        if (user == null) {
            response.put("success", false);
            response.put("error", "User không tồn tại");
            return response;
        }
        
        user.setSeller(true);
        user.setTenGianHang(request.getTenGianHang());
        user.setMoTaGianHang(request.getMoTaGianHang());
        nguoiDungRepository.save(user);
        
        // 3. Cập nhật trạng thái yêu cầu
        request.setTrangThai("APPROVED");
        request.setNgayXuLy(LocalDateTime.now());
        request.setNguoiXuLy(adminId);
        sellerRequestRepository.save(request);
        
        // 4. Gửi thông báo cho user
        try {
            NotificationMessage notification = new NotificationMessage();
            notification.setLoaiThongBao("SELLER_APPROVED");
            notification.setTieuDe("Yêu cầu bán hàng đã được duyệt");
            notification.setNoiDung(String.format(
                "Chúc mừng! Tài khoản của bạn đã được duyệt làm người bán. " +
                "Gian hàng '%s' của bạn đã sẵn sàng.",
                request.getTenGianHang()
            ));
            notification.setNguoiNhan(user.getMaNguoiDung());
            notification.setDaDoc(false);
            notification.setNgayTao(LocalDateTime.now());
            
            Map<String, Object> duLieu = new HashMap<>();
            duLieu.put("action", "APPROVED");
            duLieu.put("sellerRequestId", requestId);
            notification.setDuLieu(objectMapper.writeValueAsString(duLieu));
            
            notificationRepository.save(notification);
            
            // Gửi SSE notification
            sseEmitterService.sendToUser(user.getMaNguoiDung(), notification);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        response.put("success", true);
        response.put("message", "Đã phê duyệt thành công");
        
        return response;
    }
    
    /**
     * Từ chối yêu cầu (Admin)
     */
    @Transactional
    public Map<String, Object> rejectSellerRequest(int requestId, int adminId, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Lấy yêu cầu
        Optional<SellerRequest> requestOpt = sellerRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            response.put("success", false);
            response.put("error", "Request không tồn tại");
            return response;
        }
        
        SellerRequest request = requestOpt.get();
        
        if (!"PENDING".equals(request.getTrangThai())) {
            response.put("success", false);
            response.put("error", "Request đã được xử lý rồi");
            return response;
        }
        
        // 2. Cập nhật trạng thái yêu cầu
        request.setTrangThai("REJECTED");
        request.setNgayXuLy(LocalDateTime.now());
        request.setNguoiXuLy(adminId);
        request.setLyDoTuChoi(reason);
        sellerRequestRepository.save(request);
        
        // 3. Gửi thông báo cho user
        try {
            NguoiDung user = nguoiDungRepository.findByMaNguoiDung(request.getMaNguoiDung());
            
            NotificationMessage notification = new NotificationMessage();
            notification.setLoaiThongBao("SELLER_REJECTED");
            notification.setTieuDe("Yêu cầu bán hàng đã bị từ chối");
            notification.setNoiDung(String.format(
                "Rất tiếc! Yêu cầu đăng ký seller của bạn đã bị từ chối.%s",
                reason != null && !reason.isEmpty() ? "\nLý do: " + reason : ""
            ));
            notification.setNguoiNhan(user.getMaNguoiDung());
            notification.setDaDoc(false);
            notification.setNgayTao(LocalDateTime.now());
            
            Map<String, Object> duLieu = new HashMap<>();
            duLieu.put("action", "REJECTED");
            duLieu.put("sellerRequestId", requestId);
            duLieu.put("reason", reason);
            notification.setDuLieu(objectMapper.writeValueAsString(duLieu));
            
            notificationRepository.save(notification);
            
            // Gửi SSE notification
            sseEmitterService.sendToUser(user.getMaNguoiDung(), notification);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        response.put("success", true);
        response.put("message", "Đã từ chối yêu cầu");
        
        return response;
    }
    
    /**
     * Lấy yêu cầu của user
     */
    public Optional<SellerRequest> getMyRequest(int userId) {
        return sellerRequestRepository.findFirstByMaNguoiDungOrderByNgayTaoDesc(userId);
    }
    
    /**
     * Đếm số yêu cầu PENDING
     */
    public long countPendingRequests() {
        return sellerRequestRepository.countPendingRequests();
    }
}
