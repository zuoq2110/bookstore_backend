-- =====================================================
-- SELLER FEATURE - DATABASE MIGRATION SCRIPT
-- =====================================================

-- 1. Cập nhật bảng nguoi_dung - THÊM 3 CỘT
-- =====================================================
ALTER TABLE nguoi_dung 
ADD COLUMN is_seller BOOLEAN DEFAULT FALSE,
ADD COLUMN ten_gian_hang VARCHAR(255) NULL,
ADD COLUMN mo_ta_gian_hang TEXT NULL;

-- 2. Tạo bảng seller_books
-- =====================================================
CREATE TABLE seller_books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ma_sach INT NOT NULL,
    ma_seller INT NOT NULL,
    ngay_dang TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trang_thai VARCHAR(50) DEFAULT 'active',
    gia_nhap DECIMAL(10,2) NULL,
    so_luong_kho INT DEFAULT 0,
    FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach),
    FOREIGN KEY (ma_seller) REFERENCES nguoi_dung(ma_nguoi_dung),
    UNIQUE KEY unique_seller_book (ma_seller, ma_sach)
);

-- 3. Tạo bảng seller_orders
-- =====================================================
CREATE TABLE seller_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ma_don_hang INT NOT NULL,
    ma_seller INT NOT NULL,
    ma_sach INT NOT NULL,
    so_luong INT NOT NULL,
    gia_ban DECIMAL(10,2) NOT NULL,
    tong_tien DECIMAL(10,2) NOT NULL,
    ngay_dat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trang_thai VARCHAR(50) DEFAULT 'pending',
    FOREIGN KEY (ma_seller) REFERENCES nguoi_dung(ma_nguoi_dung),
    FOREIGN KEY (ma_sach) REFERENCES sach(ma_sach)
);

-- 4. Tạo indexes để tối ưu performance
-- =====================================================

-- Indexes cho seller_books
CREATE INDEX idx_seller_books_seller ON seller_books(ma_seller);
CREATE INDEX idx_seller_books_status ON seller_books(trang_thai);
CREATE INDEX idx_seller_books_ngay_dang ON seller_books(ngay_dang);

-- Indexes cho seller_orders
CREATE INDEX idx_seller_orders_seller ON seller_orders(ma_seller);
CREATE INDEX idx_seller_orders_date ON seller_orders(ngay_dat);
CREATE INDEX idx_seller_orders_status ON seller_orders(trang_thai);
CREATE INDEX idx_seller_orders_don_hang ON seller_orders(ma_don_hang);

-- Index cho nguoi_dung
CREATE INDEX idx_nguoi_dung_is_seller ON nguoi_dung(is_seller);

-- =====================================================
-- COMPLETE! Database schema updated for Seller Feature
-- =====================================================
