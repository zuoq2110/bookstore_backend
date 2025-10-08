-- ================================================
-- FIX ORPHAN RECORDS - Xóa dữ liệu lỗi
-- ================================================

-- 1. Kiểm tra các chi tiết đơn hàng có sách không tồn tại
SELECT 
    ct.chi_tiet_don_hang,
    ct.ma_don_hang,
    ct.ma_sach,
    ct.so_luong,
    ct.gia_ban
FROM chi_tiet_don_hang ct
LEFT JOIN sach s ON ct.ma_sach = s.ma_sach
WHERE s.ma_sach IS NULL;

-- Kết quả sẽ show các record lỗi (ma_sach = 13, v.v.)

-- 2. XÓA các chi tiết đơn hàng có sách không tồn tại
-- CẢNH BÁO: Backup database trước khi chạy!
DELETE FROM chi_tiet_don_hang
WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- 3. Xác nhận đã xóa
SELECT COUNT(*) as orphan_records
FROM chi_tiet_don_hang ct
LEFT JOIN sach s ON ct.ma_sach = s.ma_sach
WHERE s.ma_sach IS NULL;
-- Kết quả phải là 0

-- 4. Kiểm tra các bảng khác có orphan records không
-- Giỏ hàng
SELECT COUNT(*) FROM gio_hang WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- Hình ảnh
SELECT COUNT(*) FROM hinh_anh WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- Đánh giá
SELECT COUNT(*) FROM su_danh_gia WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- Sách yêu thích
SELECT COUNT(*) FROM sach_yeu_thich WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- 5. Xóa tất cả orphan records (nếu có)
DELETE FROM gio_hang WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);
DELETE FROM hinh_anh WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);
DELETE FROM su_danh_gia WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);
DELETE FROM sach_yeu_thich WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- 6. Thêm Foreign Key Constraints (nếu chưa có)
ALTER TABLE chi_tiet_don_hang
ADD CONSTRAINT fk_chitiet_sach 
FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach) 
ON DELETE CASCADE;

ALTER TABLE gio_hang
ADD CONSTRAINT fk_giohang_sach 
FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach) 
ON DELETE CASCADE;

ALTER TABLE hinh_anh
ADD CONSTRAINT fk_hinhanh_sach 
FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach) 
ON DELETE CASCADE;

ALTER TABLE su_danh_gia
ADD CONSTRAINT fk_danhgia_sach 
FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach) 
ON DELETE CASCADE;

ALTER TABLE sach_yeu_thich
ADD CONSTRAINT fk_yeuthich_sach 
FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach) 
ON DELETE CASCADE;

-- ================================================
-- HOÀN TẤT
-- ================================================
