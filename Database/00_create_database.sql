-- =====================================================
-- QUICK SETUP - Tạo Database
-- =====================================================

-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS sach 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Sử dụng database
USE sach;

-- Hiển thị thông báo
SELECT 'Database "sach" created successfully!' AS Status;

-- Hiển thị danh sách databases
SHOW DATABASES LIKE 'sach';
