-- =====================================================
-- SHOP INFO UPDATE - DATABASE MIGRATION SCRIPT
-- =====================================================
-- Thêm 4 cột mới vào bảng nguoi_dung để lưu thông tin cửa hàng

USE web_ban_sach;

-- Thêm các cột mới cho thông tin cửa hàng
ALTER TABLE nguoi_dung 
ADD COLUMN dia_chi_gian_hang VARCHAR(500) COMMENT 'Địa chỉ đầy đủ của cửa hàng',
ADD COLUMN vi_do_gian_hang DOUBLE COMMENT 'Vĩ độ (Latitude) của cửa hàng',
ADD COLUMN kinh_do_gian_hang DOUBLE COMMENT 'Kinh độ (Longitude) của cửa hàng',
ADD COLUMN so_dien_thoai_gian_hang VARCHAR(15) COMMENT 'Số điện thoại liên hệ cửa hàng';

-- Tạo indexes để tối ưu query theo location (optional, cho tính năng tìm shop gần nhất)
CREATE INDEX idx_nguoi_dung_location ON nguoi_dung(vi_do_gian_hang, kinh_do_gian_hang);
CREATE INDEX idx_nguoi_dung_seller_location ON nguoi_dung(is_seller, vi_do_gian_hang, kinh_do_gian_hang);

-- Test data (optional - chỉ để test)
-- UPDATE nguoi_dung 
-- SET dia_chi_gian_hang = '123 Nguyễn Huệ, Quận 1, TP.HCM',
--     vi_do_gian_hang = 10.762622,
--     kinh_do_gian_hang = 106.660172,
--     so_dien_thoai_gian_hang = '0987654321'
-- WHERE ma_nguoi_dung = 1 AND is_seller = TRUE;

-- Verify
SELECT ma_nguoi_dung, ten_dang_nhap, ten_gian_hang, 
       dia_chi_gian_hang, vi_do_gian_hang, kinh_do_gian_hang, 
       so_dien_thoai_gian_hang
FROM nguoi_dung 
WHERE is_seller = TRUE
LIMIT 5;
