-- Debug Script: Kiểm tra dữ liệu seller_books

-- 1. Xem tất cả các sách của seller
SELECT sb.*, s.ten_sach, s.ma_sach as 'sach_ton_tai'
FROM seller_books sb
LEFT JOIN sach s ON sb.ma_sach = s.ma_sach
WHERE sb.ma_seller = 50
ORDER BY sb.id DESC;

-- 2. Tìm các record trong seller_books mà sách KHÔNG tồn tại
SELECT sb.id, sb.ma_sach, sb.ma_seller, sb.trang_thai, 'SÁCH KHÔNG TỒN TẠI!' as warning
FROM seller_books sb
LEFT JOIN sach s ON sb.ma_sach = s.ma_sach
WHERE s.ma_sach IS NULL;

-- 3. Xem tất cả sách trong bảng sach
SELECT ma_sach, ten_sach, ten_tac_gia, gia_ban 
FROM sach 
ORDER BY ma_sach DESC 
LIMIT 10;

-- 4. FIX: Xóa các record rác (seller_books trỏ đến sách không tồn tại)
-- CHẠY LỆNH NÀY NẾU CÂU 2 TRẢ VỀ KẾT QUẢ:
/*
DELETE FROM seller_books 
WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);
*/

-- 5. Hoặc đánh dấu deleted thay vì xóa hẳn:
/*
UPDATE seller_books 
SET trang_thai = 'deleted'
WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);
*/
