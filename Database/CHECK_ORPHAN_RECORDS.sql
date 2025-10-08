-- Kiểm tra các record trong seller_books mà sách không tồn tại
SELECT sb.*, s.ma_sach as sach_ton_tai
FROM seller_books sb
LEFT JOIN sach s ON sb.ma_sach = s.ma_sach
WHERE s.ma_sach IS NULL;

-- Xóa các record rác này
DELETE FROM seller_books 
WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);

-- Hoặc đánh dấu là deleted
UPDATE seller_books 
SET trang_thai = 'deleted'
WHERE ma_sach NOT IN (SELECT ma_sach FROM sach);
