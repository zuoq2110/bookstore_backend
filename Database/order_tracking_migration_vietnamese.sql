-- ================================================
-- DATABASE MIGRATION FOR ORDER TRACKING FEATURE
-- Phiên bản: 1.0 - Tiếng Việt
-- Ngày: 2024-10-06
-- Mô tả: Thêm tính năng tracking đơn hàng
-- ================================================

-- 1. Tạo bảng lich_su_tracking_don_hang
-- ================================================
CREATE TABLE IF NOT EXISTS lich_su_tracking_don_hang (
    ma_lich_su INT PRIMARY KEY AUTO_INCREMENT,
    ma_don_hang INT NOT NULL COMMENT 'Tham chiếu đến bảng don_hang',
    trang_thai VARCHAR(50) NOT NULL COMMENT 'pending, confirmed, processing, shipping, delivered, cancelled',
    tieu_de VARCHAR(255) NOT NULL COMMENT 'Tiêu đề trạng thái',
    mo_ta TEXT COMMENT 'Mô tả chi tiết trạng thái',
    vi_tri VARCHAR(255) COMMENT 'Vị trí hiện tại của đơn hàng',
    nguoi_cap_nhat VARCHAR(100) COMMENT 'Người cập nhật (tên user/system)',
    ghi_chu TEXT COMMENT 'Ghi chú thêm',
    thoi_gian DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật trạng thái',
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_ma_don_hang (ma_don_hang),
    INDEX idx_trang_thai (trang_thai),
    INDEX idx_thoi_gian (thoi_gian),
    INDEX idx_don_hang_thoi_gian (ma_don_hang, thoi_gian DESC),
    INDEX idx_don_hang_trang_thai (ma_don_hang, trang_thai),
    
    -- Foreign key
    FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Lịch sử tracking của đơn hàng';

-- 2. Thêm các cột tracking vào bảng don_hang
-- ================================================
ALTER TABLE don_hang 
    ADD COLUMN ma_van_don VARCHAR(100) COMMENT 'Mã vận đơn' AFTER tinh_trang_don_hang,
    ADD COLUMN don_vi_van_chuyen VARCHAR(100) COMMENT 'Đơn vị vận chuyển (Viettel Post, GHN, etc.)' AFTER ma_van_don,
    ADD COLUMN sdt_van_chuyen VARCHAR(20) COMMENT 'Hotline đơn vị vận chuyển' AFTER don_vi_van_chuyen,
    ADD COLUMN link_tracking TEXT COMMENT 'Link tracking trên website đơn vị vận chuyển' AFTER sdt_van_chuyen,
    ADD COLUMN thoi_gian_giao_du_kien DATETIME COMMENT 'Thời gian dự kiến giao hàng' AFTER link_tracking,
    ADD INDEX idx_ma_van_don (ma_van_don);

-- 3. Tạo view để lấy trạng thái tracking mới nhất
-- ================================================
CREATE OR REPLACE VIEW v_tracking_don_hang_moi_nhat AS
SELECT 
    dh.ma_don_hang,
    dh.tinh_trang_don_hang as trang_thai_hien_tai,
    dh.ma_van_don,
    dh.don_vi_van_chuyen,
    dh.thoi_gian_giao_du_kien,
    lst.tieu_de as tieu_de_tracking_moi_nhat,
    lst.mo_ta as mo_ta_tracking_moi_nhat,
    lst.vi_tri as vi_tri_hien_tai,
    lst.thoi_gian as cap_nhat_lan_cuoi,
    lst.nguoi_cap_nhat
FROM don_hang dh
LEFT JOIN (
    SELECT 
        ma_don_hang,
        tieu_de,
        mo_ta,
        vi_tri,
        thoi_gian,
        nguoi_cap_nhat,
        ROW_NUMBER() OVER (PARTITION BY ma_don_hang ORDER BY thoi_gian DESC) as rn
    FROM lich_su_tracking_don_hang
) lst ON dh.ma_don_hang = lst.ma_don_hang AND lst.rn = 1;

-- 4. Tạo stored procedure để thêm tracking entry
-- ================================================
DELIMITER //

CREATE PROCEDURE sp_them_tracking_don_hang(
    IN p_ma_don_hang INT,
    IN p_trang_thai VARCHAR(50),
    IN p_tieu_de VARCHAR(255),
    IN p_mo_ta TEXT,
    IN p_vi_tri VARCHAR(255),
    IN p_nguoi_cap_nhat VARCHAR(100),
    IN p_ghi_chu TEXT
)
BEGIN
    -- Insert tracking history
    INSERT INTO lich_su_tracking_don_hang (
        ma_don_hang,
        trang_thai,
        tieu_de,
        mo_ta,
        vi_tri,
        nguoi_cap_nhat,
        ghi_chu,
        thoi_gian
    ) VALUES (
        p_ma_don_hang,
        p_trang_thai,
        p_tieu_de,
        p_mo_ta,
        p_vi_tri,
        p_nguoi_cap_nhat,
        p_ghi_chu,
        NOW()
    );
    
    -- Update trạng thái đơn hàng
    UPDATE don_hang 
    SET 
        tinh_trang_don_hang = p_trang_thai
    WHERE ma_don_hang = p_ma_don_hang;
    
    -- Return success
    SELECT LAST_INSERT_ID() as ma_lich_su;
END //

DELIMITER ;

-- 5. Tạo trigger để tự động tạo tracking entry khi đơn hàng mới
-- ================================================
DELIMITER //

CREATE TRIGGER trg_don_hang_moi_tao_tracking
AFTER INSERT ON don_hang
FOR EACH ROW
BEGIN
    INSERT INTO lich_su_tracking_don_hang (
        ma_don_hang,
        trang_thai,
        tieu_de,
        mo_ta,
        vi_tri,
        nguoi_cap_nhat,
        thoi_gian
    ) VALUES (
        NEW.ma_don_hang,
        'Đang xử lý',
        'Đơn hàng đã đặt',
        'Đơn hàng của bạn đã được tạo thành công',
        'Hệ thống',
        'System',
        NOW()
    );
END //

DELIMITER ;

-- 6. Thêm dữ liệu tracking mẫu cho các đơn hàng hiện có (nếu cần)
-- ================================================
-- Tạo tracking entry cho tất cả đơn hàng hiện có chưa có tracking

INSERT INTO lich_su_tracking_don_hang (ma_don_hang, trang_thai, tieu_de, mo_ta, vi_tri, nguoi_cap_nhat, thoi_gian)
SELECT 
    ma_don_hang,
    COALESCE(tinh_trang_don_hang, 'Đang xử lý') as trang_thai,
    'Đơn hàng đã đặt' as tieu_de,
    'Đơn hàng của bạn đã được tạo thành công' as mo_ta,
    'Hệ thống' as vi_tri,
    'System' as nguoi_cap_nhat,
    COALESCE(ngay_tao, NOW()) as thoi_gian
FROM don_hang
WHERE ma_don_hang NOT IN (SELECT DISTINCT ma_don_hang FROM lich_su_tracking_don_hang);

-- 7. Queries hữu ích để test
-- ================================================

-- Xem tracking history của một đơn hàng
-- SELECT * FROM lich_su_tracking_don_hang WHERE ma_don_hang = 1 ORDER BY thoi_gian DESC;

-- Xem tracking mới nhất của tất cả đơn hàng
-- SELECT * FROM v_tracking_don_hang_moi_nhat;

-- Đếm số lượng tracking entry theo trạng thái
-- SELECT trang_thai, COUNT(*) as so_luong FROM lich_su_tracking_don_hang GROUP BY trang_thai;

-- Xem đơn hàng với thông tin tracking
/*
SELECT 
    dh.ma_don_hang,
    dh.tinh_trang_don_hang,
    dh.ma_van_don,
    dh.don_vi_van_chuyen,
    dh.thoi_gian_giao_du_kien,
    COUNT(lst.ma_lich_su) as so_luong_tracking
FROM don_hang dh
LEFT JOIN lich_su_tracking_don_hang lst ON dh.ma_don_hang = lst.ma_don_hang
GROUP BY dh.ma_don_hang;
*/

-- ================================================
-- ROLLBACK SCRIPT (nếu cần)
-- ================================================
/*
-- Drop trigger
DROP TRIGGER IF EXISTS trg_don_hang_moi_tao_tracking;

-- Drop stored procedure
DROP PROCEDURE IF EXISTS sp_them_tracking_don_hang;

-- Drop view
DROP VIEW IF EXISTS v_tracking_don_hang_moi_nhat;

-- Xóa các cột khỏi don_hang
ALTER TABLE don_hang 
    DROP COLUMN ma_van_don,
    DROP COLUMN don_vi_van_chuyen,
    DROP COLUMN sdt_van_chuyen,
    DROP COLUMN link_tracking,
    DROP COLUMN thoi_gian_giao_du_kien;

-- Drop table
DROP TABLE IF EXISTS lich_su_tracking_don_hang;
*/

-- ================================================
-- VERIFICATION QUERIES
-- ================================================

-- Kiểm tra bảng đã tạo chưa
SELECT TABLE_NAME FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lich_su_tracking_don_hang';

-- Kiểm tra các cột đã thêm vào don_hang
SHOW COLUMNS FROM don_hang LIKE 'ma_van_don';
SHOW COLUMNS FROM don_hang LIKE 'don_vi_van_chuyen';

-- Kiểm tra view
SELECT * FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'v_tracking_don_hang_moi_nhat';

-- Kiểm tra stored procedure
SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name = 'sp_them_tracking_don_hang';

-- Kiểm tra trigger
SHOW TRIGGERS WHERE `Table` = 'don_hang' AND `Trigger` = 'trg_don_hang_moi_tao_tracking';

-- ================================================
-- KẾT THÚC MIGRATION
-- ================================================
