package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SseEmitterService;
import com.example.web_ban_sach.dao.NotificationRepository;
import com.example.web_ban_sach.entity.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private SseEmitterService sseEmitterService;
    
    /**
     * SSE Stream endpoint
     * GET /notifications/stream?userId=1
     * 
     * Client usage:
     * const eventSource = new EventSource('http://localhost:8080/notifications/stream?userId=1');
     * eventSource.addEventListener('NotificationMessage', (event) => {
     *     const NotificationMessage = JSON.parse(event.data);
     *     console.log('New NotificationMessage:', NotificationMessage);
     * });
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@RequestParam int userId) {
        return sseEmitterService.createEmitter(userId);
    }
    
    /**
     * Lấy danh sách thông báo của user
     * GET /notifications?userId=1&page=0&size=20&unreadOnly=false
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayTao").descending());
        
        Page<NotificationMessage> notifications;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByNguoiNhanAndDaDoc(userId, false, pageable);
        } else {
            notifications = notificationRepository.findByNguoiNhan(userId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", notifications.getContent());
        response.put("totalPages", notifications.getTotalPages());
        response.put("totalElements", notifications.getTotalElements());
        response.put("number", notifications.getNumber());
        response.put("size", notifications.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Đếm số thông báo chưa đọc
     * GET /notifications/unread-count?userId=1
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestParam int userId) {
        long count = notificationRepository.countByNguoiNhanAndDaDoc(userId, false);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * Đánh dấu thông báo đã đọc
     * PUT /notifications/{notificationId}/mark-read
     */
    @PutMapping("/{notificationId}/mark-read")
    public ResponseEntity<?> markAsRead(@PathVariable int notificationId) {
        NotificationMessage NotificationMessage = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("NotificationMessage không tồn tại"));
        
        NotificationMessage.setDaDoc(true);
        notificationRepository.save(NotificationMessage);
        
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * Đánh dấu tất cả thông báo đã đọc
     * PUT /notifications/mark-all-read?userId=1
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(@RequestParam int userId) {
        notificationRepository.markAllAsReadByUser(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * Lấy thông tin SSE connection
     * GET /notifications/stream-info
     */
    @GetMapping("/stream-info")
    public ResponseEntity<?> getStreamInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalConnections", sseEmitterService.getTotalConnections());
        info.put("onlineUsers", sseEmitterService.getOnlineUsersCount());
        return ResponseEntity.ok(info);
    }
    
    /**
     * Check user online status
     * GET /notifications/online-status?userId=1
     */
    @GetMapping("/online-status")
    public ResponseEntity<?> checkOnlineStatus(@RequestParam int userId) {
        boolean isOnline = sseEmitterService.isUserOnline(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "isOnline", isOnline
        ));
    }
}
