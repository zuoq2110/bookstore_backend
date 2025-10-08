package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.NotificationMessage;
import com.example.web_ban_sach.entity.NguoiDung;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseEmitterService {
    
    // Map userId -> List<SseEmitter>
    private final Map<Integer, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Tạo SSE connection mới cho user
     */
    public SseEmitter createEmitter(int userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout
        
        // Add to map
        emitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);
        
        // Remove on complete/timeout/error
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data(Map.of(
                    "status", "connected",
                    "message", "Connected to notification stream",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                )));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }
        
        return emitter;
    }
    
    /**
     * Xóa emitter khi disconnect
     */
    private void removeEmitter(int userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
    
    /**
     * Gửi thông báo đến tất cả admin
     */
    public void broadcastToAdmins(NotificationMessage notification) {
        // Lấy tất cả admin (Quyen 2 = Admin)
        List<NguoiDung> admins = nguoiDungRepository.findUsersByRoleId(2);
        
        for (NguoiDung admin : admins) {
            sendToUser(admin.getMaNguoiDung(), notification);
        }
    }
    
    /**
     * Gửi thông báo đến 1 user cụ thể
     */
    public void sendToUser(int userId, NotificationMessage notification) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return; // User không online
        }
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        
        // Remove dead emitters
        userEmitters.removeAll(deadEmitters);
    }
    
    /**
     * Gửi event tùy chỉnh
     */
    public void sendCustomEvent(int userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        
        userEmitters.removeAll(deadEmitters);
    }
    
    /**
     * Kiểm tra user có đang online không
     */
    public boolean isUserOnline(int userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }
    
    /**
     * Lấy số lượng connection hiện tại
     */
    public int getTotalConnections() {
        return emitters.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Lấy số users online
     */
    public int getOnlineUsersCount() {
        return emitters.size();
    }
}
