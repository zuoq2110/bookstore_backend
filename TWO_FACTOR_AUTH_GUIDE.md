# Hướng dẫn triển khai 2FA (Two-Factor Authentication)

## 📋 Tổng quan

Hệ thống 2FA đã được triển khai theo 2 giai đoạn chính:
1. **Thiết lập 2FA** - Người dùng bật bảo mật 2 lớp
2. **Đăng nhập với 2FA** - Xác thực mã khi đăng nhập

## 🚀 API Endpoints

### 1. Thiết lập 2FA (Setup)

#### Bước 1: Tạo QR Code
```
POST /api/2fa/setup
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "success": true,
  "message": "QR code generated",
  "data": {
    "success": true,
    "message": "Scan QR code with your authenticator app",
    "qrCodeUrl": "otpauth://totp/WebBanSach:user@email.com?secret=ABC123SECRET&issuer=WebBanSach",
    "secretKey": "ABC123SECRET"
  }
}
```

#### Bước 2: Xác nhận thiết lập
```
POST /api/2fa/confirm
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "verificationCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "2FA enabled successfully",
  "data": {
    "success": true,
    "message": "2FA enabled successfully",
    "backupCodes": [
      "12345678", "87654321", "11223344", "55667788",
      "99887766", "44332211", "66778899", "33445566"
    ]
  }
}
```

### 2. Đăng nhập với 2FA

#### Bước 1: Đăng nhập thông thường
```
POST /tai-khoan/dang-nhap
Content-Type: application/json

{
  "username": "user@email.com",
  "password": "password123"
}
```

**Response (nếu user có 2FA):**
```json
{
  "success": false,
  "message": "Two-factor authentication required",
  "mfaToken": "temp_mfa_token_here",
  "errorCode": "MFA_REQUIRED",
  "expiresIn": 300
}
```

#### Bước 2: Xác thực 2FA
```
POST /tai-khoan/verify-2fa
Content-Type: application/json

{
  "mfaToken": "temp_mfa_token_here",
  "verificationCode": "123456"  // Mã 6 số từ Authenticator hoặc 8 số backup code
}
```

**Lưu ý quan trọng:**
- **Authenticator Code**: Mã 6 số từ Google Authenticator/Authy (thời hạn 30 giây)
- **Backup Code**: Mã 8 số từ danh sách backup codes (chỉ dùng 1 lần)
- Hệ thống tự động phân biệt dựa vào độ dài và format của mã

**Response (thành công):**
```json
{
  "jwt": "actual_jwt_token",
  "refreshToken": "refresh_token",
  "id": 1,
  "email": "user@email.com",
  "admin": false,
  "seller": true,
  "tenGianHang": "My Shop"
}
```

### 3. Quản lý 2FA

#### Kiểm tra trạng thái 2FA
```
GET /api/2fa/status
Authorization: Bearer <jwt_token>
```

#### Tắt 2FA
```
POST /api/2fa/disable
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "verificationCode": "123456"
}
```

#### Tạo backup codes mới
```
POST /api/2fa/regenerate-backup-codes
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "verificationCode": "123456"
}
```

## 🛠 Triển khai Database

**Quan trọng**: Chạy migration script để thêm các trường 2FA:

```sql
-- File: Database/migration_2fa_support.sql
-- Thêm các cột cho 2FA
ALTER TABLE nguoi_dung 
ADD COLUMN mfa_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN mfa_secret VARCHAR(32) NULL,
ADD COLUMN backup_codes TEXT NULL;

-- Fix existing records
UPDATE nguoi_dung SET mfa_enabled = FALSE WHERE mfa_enabled IS NULL;

-- Set NOT NULL constraint
ALTER TABLE nguoi_dung MODIFY COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
```

**Lưu ý**: Phải chạy script này trước khi restart ứng dụng để tránh lỗi NULL values.

## 📱 Frontend Integration

### 1. Thiết lập 2FA

```javascript
// Bước 1: Tạo QR Code
const setupResponse = await fetch('/api/2fa/setup', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${userToken}`,
    'Content-Type': 'application/json'
  }
});

const setupData = await setupResponse.json();
const qrCodeUrl = setupData.data.qrCodeUrl;

// Hiển thị QR code cho user quét bằng Authenticator app
// Có thể dùng thư viện như qrcode để generate QR image

// Bước 2: User nhập mã xác nhận
const confirmResponse = await fetch('/api/2fa/confirm', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${userToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    verificationCode: userInput // 6 digit code từ authenticator
  })
});

const confirmData = await confirmResponse.json();
// Hiển thị backup codes cho user lưu lại
console.log('Backup codes:', confirmData.data.backupCodes);
```

### 2. Đăng nhập với 2FA

```javascript
// Bước 1: Đăng nhập thông thường
const loginResponse = await fetch('/tai-khoan/dang-nhap', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: email,
    password: password
  })
});

if (loginResponse.status === 403) {
  // User có 2FA enabled
  const mfaData = await loginResponse.json();
  const mfaToken = mfaData.mfaToken;
  
  // Hiển thị form nhập mã 2FA
  // User nhập 6-digit code từ authenticator app
  
  // Bước 2: Xác thực 2FA
  const verify2faResponse = await fetch('/tai-khoan/verify-2fa', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      mfaToken: mfaToken,
      verificationCode: userTwoFactorCode // 6 số từ app hoặc 8 số backup code
    })
  });
  
  if (verify2faResponse.ok) {
    const userData = await verify2faResponse.json();
    // Lưu JWT và redirect user
    localStorage.setItem('token', userData.jwt);
    localStorage.setItem('refreshToken', userData.refreshToken);
  }
} else if (loginResponse.ok) {
  // User không có 2FA, đăng nhập thành công
  const userData = await loginResponse.json();
  localStorage.setItem('token', userData.jwt);
  localStorage.setItem('refreshToken', userData.refreshToken);
}
```

## 🔐 Bảo mật

1. **MFA Token**: Có thời hạn 5 phút, một lần sử dụng
2. **Secret Key**: **Được mã hóa AES-256-GCM** trước khi lưu vào database
3. **Backup Codes**: **Được hash bằng BCrypt**, chỉ sử dụng 1 lần, xóa sau khi dùng
4. **Rate Limiting**: Có thể thêm để chống brute force

### 🔒 AES Encryption cho MFA Secrets

**Sử dụng chung encryption key với chat system:**
```properties
# Chat Encryption Configuration (cũng dùng cho MFA secrets)
app.chat.encryption.key=1qTkyT9oaoScG22gcKQEbDs0JNuCGbiOz3CVxsmJBpY=
```

**Migration cho MFA secrets hiện có:**
```bash
# Chạy migration để encrypt secrets hiện có (CHỈ 1 LẦN)
POST /admin/mfa-migration/encrypt-secrets
Authorization: Bearer <admin_jwt_token>

# Kiểm tra trạng thái migration
GET /admin/mfa-migration/status
Authorization: Bearer <admin_jwt_token>

# Emergency rollback (nếu cần)
POST /admin/mfa-migration/rollback
Authorization: Bearer <admin_jwt_token>
```

**Lưu ý bảo mật:**
- ✅ MFA Secret được mã hóa AES-256-GCM với random IV (tái sử dụng MessageEncryptionUtil)
- ✅ Backup codes được hash bằng BCrypt
- ✅ Sử dụng chung encryption key với chat system để tối ưu hóa
- ✅ Migration script để encrypt secrets hiện có
- ✅ Rollback mechanism trong trường hợp emergency

## 📚 Thư viện sử dụng

- **GoogleAuth**: `com.warrenstrange:googleauth:1.5.0`
- **QR Code**: `com.google.zxing:core:3.5.1` & `javase:3.5.1`

## 🧪 Testing

### Test với Postman:

1. Đăng nhập để lấy JWT token
2. Call `/api/2fa/setup` để tạo QR code
3. Quét QR bằng Google Authenticator
4. Call `/api/2fa/confirm` với mã 6 số
5. Lưu backup codes được trả về
6. Logout và đăng nhập lại để test flow 2FA
7. Test với mã Authenticator và backup code

### Test Backup Codes:
```json
// Sử dụng backup code thay vì mã từ Authenticator
{
  "mfaToken": "temp_mfa_token_here", 
  "verificationCode": "12345678"  // Backup code 8 số
}
```

### Authenticator Apps:
- Google Authenticator
- Authy
- Microsoft Authenticator
- 1Password

## 🐛 Troubleshooting

**Lỗi 401 với `/tai-khoan/verify-2fa`:**
- Endpoint này phải được public (không cần authentication)
- Kiểm tra SecurityConfiguration đã permit endpoint `/tai-khoan/verify-2fa` chưa
- Restart application sau khi sửa security config

**Lỗi 401 với `/api/2fa/setup`:**
- Cần JWT token hợp lệ trong Authorization header
- Format: `Authorization: Bearer <jwt_token>`
- Token phải chưa hết hạn và signed đúng

**Lỗi "Invalid verification code":**
- Kiểm tra đồng hồ hệ thống (time sync)
- Đảm bảo mã được nhập trong window time (30 giây)
- Thử backup codes nếu có
- Backup code chỉ dùng được 1 lần, kiểm tra đã sử dụng chưa

**Lỗi "MFA token invalid":**
- Token đã hết hạn (5 phút)
- Đăng nhập lại từ đầu

**Database issues:**
- Chạy migration script
- Kiểm tra constraint và indexes