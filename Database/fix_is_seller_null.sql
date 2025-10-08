-- =====================================================
-- SỬA LỖI IS_SELLER TRẢ VỀ NULL
-- Vấn đề: MySQL lưu BOOLEAN thành TINYINT(1) và có giá trị NULL
-- =====================================================

USE sach;

-- BƯỚC 1: Kiểm tra cấu trúc hiện tại
-- =====================================================
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'sach' 
  AND TABLE_NAME = 'nguoi_dung'
  AND COLUMN_NAME IN ('is_seller', 'ten_gian_hang', 'mo_ta_gian_hang');

-- BƯỚC 2: Xem dữ liệu hiện tại (kiểm tra NULL)
-- =====================================================
SELECT 
    ma_nguoi_dung,
    ten_dang_nhap,
    is_seller,
    IFNULL(is_seller, 'NULL') as is_seller_check,
    ten_gian_hang,
    mo_ta_gian_hang
FROM nguoi_dung 
LIMIT 10;

-- BƯỚC 3: UPDATE tất cả giá trị NULL thành 0 (FALSE)
-- =====================================================
UPDATE nguoi_dung 
SET is_seller = 0 
WHERE is_seller IS NULL;

-- BƯỚC 4: Đảm bảo cột is_seller là TINYINT(1) NOT NULL DEFAULT 0
-- =====================================================
ALTER TABLE nguoi_dung 
MODIFY COLUMN is_seller TINYINT(1) NOT NULL DEFAULT 0;

-- BƯỚC 5: VERIFY - Kiểm tra lại sau khi fix
-- =====================================================
SELECT 
    ma_nguoi_dung,
    ten_dang_nhap,
    is_seller,
    ten_gian_hang,
    mo_ta_gian_hang
FROM nguoi_dung 
LIMIT 10;

-- BƯỚC 6: Kiểm tra cấu trúc cuối cùng
-- =====================================================
DESCRIBE nguoi_dung;

-- =====================================================
-- KẾT QUẢ MONG ĐỢI:
-- - is_seller: TINYINT(1) NOT NULL DEFAULT 0
-- - Tất cả user hiện tại có is_seller = 0 (FALSE)
-- - Không còn giá trị NULL
-- =====================================================
