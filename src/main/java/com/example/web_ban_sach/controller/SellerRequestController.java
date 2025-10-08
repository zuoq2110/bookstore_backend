package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SellerRequestService;
import com.example.web_ban_sach.dto.SellerRequestDTO;
import com.example.web_ban_sach.dto.SellerRequestResponse;
import com.example.web_ban_sach.entity.SellerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/seller-requests")
@CrossOrigin(origins = "*")
public class SellerRequestController {
    
    @Autowired
    private SellerRequestService sellerRequestService;
    
    /**
     * Submit yêu cầu đăng ký seller
     * POST /seller-requests/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitSellerRequest(@RequestBody SellerRequestDTO dto) {
        Map<String, Object> response = sellerRequestService.submitSellerRequest(dto);
        
        if (Boolean.FALSE.equals(response.get("success"))) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy danh sách yêu cầu (Admin only)
     * GET /seller-requests?status=PENDING&page=0&size=20&sortBy=ngayTao&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<?> getSellerRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SellerRequestResponse> requests = sellerRequestService.getSellerRequests(status, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", requests.getContent());
        response.put("totalPages", requests.getTotalPages());
        response.put("totalElements", requests.getTotalElements());
        response.put("currentPage", requests.getNumber());
        response.put("size", requests.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Phê duyệt yêu cầu (Admin only)
     * POST /seller-requests/{requestId}/approve
     * Body: {"adminId": 1}
     */
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveSellerRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Integer> body) {
        
        int adminId = body.get("adminId");
        Map<String, Object> response = sellerRequestService.approveSellerRequest(requestId, adminId);
        
        if (Boolean.FALSE.equals(response.get("success"))) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Từ chối yêu cầu (Admin only)
     * POST /seller-requests/{requestId}/reject
     * Body: {"adminId": 1, "reason": "..."}
     */
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectSellerRequest(
            @PathVariable int requestId,
            @RequestBody Map<String, Object> body) {
        
        int adminId = (int) body.get("adminId");
        String reason = (String) body.getOrDefault("reason", "");
        
        Map<String, Object> response = sellerRequestService.rejectSellerRequest(requestId, adminId, reason);
        
        if (Boolean.FALSE.equals(response.get("success"))) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy yêu cầu của user hiện tại
     * GET /seller-requests/my-request?userId=5
     */
    @GetMapping("/my-request")
    public ResponseEntity<?> getMySellerRequest(@RequestParam int userId) {
        Optional<SellerRequest> request = sellerRequestService.getMyRequest(userId);
        
        if (request.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "message", "Bạn chưa có yêu cầu đăng ký seller nào"
            ));
        }
        
        return ResponseEntity.ok(request.get());
    }
    
    /**
     * Đếm số yêu cầu PENDING
     * GET /seller-requests/pending-count
     */
    @GetMapping("/pending-count")
    public ResponseEntity<?> getPendingCount() {
        long count = sellerRequestService.countPendingRequests();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
