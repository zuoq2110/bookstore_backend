-- Migration: Seller Approval System with Realtime Notifications
-- Date: 2025-10-07
-- Purpose: Thêm chức năng phê duyệt seller và hệ thống thông báo realtime

USE web_ban_sach;

-- =====================================================
-- TABLE: seller_requests
-- Lưu yêu cầu đăng ký làm seller
-- =====================================================
CREATE TABLE IF NOT EXISTS seller_requests (
    ma_yeu_cau INT PRIMARY KEY AUTO_INCREMENT,
    ma_nguoi_dung INT NOT NULL,
    ten_gian_hang VARCHAR(255) NOT NULL,
    mo_ta_gian_hang TEXT,
    trang_thai VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_xu_ly DATETIME,
    nguoi_xu_ly INT, -- Admin ID
    ly_do_tu_choi TEXT,
    
    FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung) ON DELETE CASCADE,
    FOREIGN KEY (nguoi_xu_ly) REFERENCES nguoi_dung(ma_nguoi_dung) ON DELETE SET NULL,
    
    -- Mỗi user chỉ có 1 yêu cầu PENDING tại 1 thời điểm
    UNIQUE KEY unique_user_pending_request (ma_nguoi_dung, trang_thai),
    
    -- Indexes for performance
    INDEX idx_trang_thai (trang_thai),
    INDEX idx_ngay_tao (ngay_tao DESC),
    INDEX idx_ma_nguoi_dung (ma_nguoi_dung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: notifications
-- Lưu thông báo hệ thống
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    ma_thong_bao INT PRIMARY KEY AUTO_INCREMENT,
    loai_thong_bao VARCHAR(50) NOT NULL, -- SELLER_REQUEST, ORDER_UPDATE, SYSTEM
    tieu_de VARCHAR(255) NOT NULL,
    noi_dung TEXT NOT NULL,
    nguoi_nhan INT, -- NULL = tất cả admin
    da_doc BOOLEAN DEFAULT FALSE,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    du_lieu JSON, -- Dữ liệu bổ sung dạng JSON
    
    FOREIGN KEY (nguoi_nhan) REFERENCES nguoi_dung(ma_nguoi_dung) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_nguoi_nhan (nguoi_nhan),
    INDEX idx_loai_thong_bao (loai_thong_bao),
    INDEX idx_da_doc (da_doc),
    INDEX idx_ngay_tao (ngay_tao DESC),
    INDEX idx_nguoi_nhan_da_doc (nguoi_nhan, da_doc) -- Composite index for unread queries
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- INSERT SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Sample seller request (for testing)
-- INSERT INTO seller_requests (ma_nguoi_dung, ten_gian_hang, mo_ta_gian_hang, trang_thai)
-- VALUES (5, 'Nhà sách ABC', 'Chuyên sách văn học, tiểu thuyết', 'PENDING');

-- Sample notification (for testing)
-- INSERT INTO notifications (loai_thong_bao, tieu_de, noi_dung, nguoi_nhan, da_doc)
-- VALUES ('SYSTEM', 'Chào mừng', 'Chào mừng bạn đến với hệ thống', 1, FALSE);

-- =====================================================
-- VERIFY TABLES
-- =====================================================
SELECT 'seller_requests table created' as status;
DESCRIBE seller_requests;

SELECT 'notifications table created' as status;
DESCRIBE notifications;

-- Check constraints
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'web_ban_sach'
  AND TABLE_NAME IN ('seller_requests', 'notifications');
