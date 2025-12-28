# Fix cho JPA Null Exception

## Vấn đề
```
org.springframework.orm.jpa.JpaSystemException: Null value was assigned to a property [mfaEnabled] of primitive type
```

## Nguyên nhân
Database có giá trị NULL trong cột `mfa_enabled` nhưng entity sử dụng primitive `boolean`.

## Giải pháp

### 1. Chạy SQL để fix database:
```sql
-- Kiểm tra dữ liệu hiện tại
SELECT mfa_enabled, COUNT(*) FROM nguoi_dung GROUP BY mfa_enabled;

-- Fix NULL values
UPDATE nguoi_dung SET mfa_enabled = FALSE WHERE mfa_enabled IS NULL;

-- Modify cột để đảm bảo NOT NULL
ALTER TABLE nguoi_dung MODIFY COLUMN mfa_enabled BOOLEAN DEFAULT FALSE NOT NULL;
```

### 2. Đã cập nhật Entity:
- Đổi từ `boolean mfaEnabled` → `Boolean mfaEnabled`
- Thêm helper methods `isMfaEnabled()` và `setMfaEnabled(boolean)`
- Đặt default value `= false`

### 3. Restart ứng dụng sau khi chạy SQL

### 4. Test 2FA:
1. Đăng nhập để lấy token
2. Call `/api/2fa/setup`
3. Scan QR code
4. Call `/api/2fa/confirm` với verification code