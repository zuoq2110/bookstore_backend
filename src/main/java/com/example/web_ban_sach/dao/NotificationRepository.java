package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.NotificationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationMessage, Integer> {
    
    // Lấy thông báo theo người nhận
    Page<NotificationMessage> findByNguoiNhan(Integer nguoiNhan, Pageable pageable);
    
    // Lấy thông báo chưa đọc của user
    Page<NotificationMessage> findByNguoiNhanAndDaDoc(Integer nguoiNhan, boolean daDoc, Pageable pageable);
    
    // Đếm số thông báo chưa đọc
    long countByNguoiNhanAndDaDoc(Integer nguoiNhan, boolean daDoc);
    
    // Lấy thông báo gửi cho tất cả admin (nguoiNhan = null)
    @Query("SELECT n FROM NotificationMessage n WHERE n.nguoiNhan IS NULL ORDER BY n.ngayTao DESC")
    Page<NotificationMessage> findAdminBroadcastNotifications(Pageable pageable);
    
    // Đánh dấu tất cả thông báo là đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE NotificationMessage n SET n.daDoc = true WHERE n.nguoiNhan = :userId AND n.daDoc = false")
    void markAllAsReadByUser(@Param("userId") int userId);
    
    // Lấy thông báo theo loại
    Page<NotificationMessage> findByLoaiThongBao(String loaiThongBao, Pageable pageable);
    
    // Xóa thông báo cũ (optional - for cleanup)
    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationMessage n WHERE n.ngayTao < :beforeDate")
    void deleteOldNotifications(@Param("beforeDate") java.time.LocalDateTime beforeDate);
}
