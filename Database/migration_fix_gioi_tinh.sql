-- Migration: Fix gioi_tinh field from CHAR to VARCHAR
-- Date: 2025-10-06
-- Purpose: Giải quyết vấn đề encoding UTF-8 với giới tính

USE web_ban_sach;

-- Bước 1: Xem data hiện tại
SELECT 
    ma_nguoi_dung, 
    ten, 
    gioi_tinh,
    HEX(gioi_tinh) as hex_value,
    LENGTH(gioi_tinh) as byte_length,
    CHAR_LENGTH(gioi_tinh) as char_length
FROM nguoi_dung 
WHERE gioi_tinh IS NOT NULL
LIMIT 10;

-- Bước 2: Đổi kiểu dữ liệu từ CHAR(1) sang VARCHAR(10) với UTF-8
ALTER TABLE nguoi_dung 
MODIFY COLUMN gioi_tinh VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bước 3: Clean data lỗi (ký tự � hoặc giá trị lạ)
-- Replace ký tự lỗi thành NULL hoặc giá trị mặc định
UPDATE nguoi_dung 
SET gioi_tinh = NULL 
WHERE gioi_tinh IS NOT NULL 
  AND gioi_tinh NOT IN ('M', 'F', 'Nam', 'Nữ', 'Khác', 'male', 'female', 'other');

-- Bước 4: Chuẩn hóa giá trị (optional)
-- Đổi 'male' -> 'M', 'female' -> 'F'
UPDATE nguoi_dung SET gioi_tinh = 'M' WHERE gioi_tinh = 'male';
UPDATE nguoi_dung SET gioi_tinh = 'F' WHERE gioi_tinh = 'female';
UPDATE nguoi_dung SET gioi_tinh = 'M' WHERE gioi_tinh = 'Nam';
UPDATE nguoi_dung SET gioi_tinh = 'F' WHERE gioi_tinh = 'Nữ';

-- Bước 5: Verify kết quả
SELECT 
    ma_nguoi_dung, 
    ten, 
    gioi_tinh,
    HEX(gioi_tinh) as hex_value
FROM nguoi_dung 
WHERE ma_nguoi_dung = 50;

-- Check tất cả giá trị giới tính hiện có
SELECT 
    gioi_tinh, 
    COUNT(*) as count,
    HEX(gioi_tinh) as hex_value
FROM nguoi_dung 
GROUP BY gioi_tinh;
