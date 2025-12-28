# 🔐 OTP Security System Documentation

## Tổng quan
Hệ thống OTP (One-Time Password) an toàn với các tính năng bảo mật cao, chống brute force và rate limiting.

## 🔐 Tính năng Bảo mật

### ✅ 2.1 Generate OTP
- **Random 6 chữ số**: `100000 - 999999`
- **SecureRandom**: Không đoán được
- **Algorithm**: `int otp = 100000 + secureRandom.nextInt(900000)`

### ✅ 2.2 Lưu OTP AN TOÀN
- ❌ **KHÔNG** lưu plain OTP
- ✅ **Lưu**: `SHA-256 hash(otp)` + `expired_at` + `attempt_count`
- **Database Schema**:
  ```sql
  CREATE TABLE otp_verification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    identifier VARCHAR(255) NOT NULL,    -- email/phone
    otp_hash VARCHAR(255) NOT NULL,      -- SHA-256 hashed OTP
    otp_type ENUM('REGISTER', 'RESET_PASSWORD', 'CHANGE_EMAIL', 'LOGIN_VERIFICATION'),
    expired_at DATETIME NOT NULL,
    attempts INT DEFAULT 0,
    created_at DATETIME DEFAULT NOW(),
    verified_at DATETIME NULL,
    is_used BOOLEAN DEFAULT FALSE
  );
  ```

### ✅ 2.3 Gửi OTP
- **Email service** với template đẹp
- **Log trạng thái gửi** (không log OTP)
- **Error handling** robust

### ✅ 2.4 Verify OTP
Kiểm tra theo thứ tự:
1. **OTP tồn tại** và **chưa hết hạn**
2. **Chưa quá số lần thử** (default: 3 lần)
3. **Hash OTP** user nhập và so sánh với DB
4. **Mark as used** sau khi verify thành công

### ✅ 2.5 Rate Limit & Anti-Bruteforce
- **OTP requests**: Max 5 lần/giờ per identifier
- **Failed attempts**: Max 3 lần per OTP
- **Auto block**: OTP bị vô hiệu sau quá số lần thử
- **Rate limit**: Chặn tạm thời khi abuse

### ✅ 2.6 Cleanup OTP
- **Auto cleanup**: Scheduled task mỗi 30 phút
- **Immediate cleanup**: Sau khi verify thành công
- **Expired cleanup**: Cron job xóa OTP hết hạn

### ✅ 2.7 Business Logic
Theo `OtpType` được handle trực tiếp trong AuthOtpController:
- **REGISTER** → Xác thực email, update `emailXacThuc = true` cho user
- **RESET_PASSWORD** → Generate reset token để frontend cho đổi password

## 🚀 API Endpoints

### 1. Registration Flow
```bash
# Step 1: Send registration OTP
POST /api/auth/register/send-otp
{
  "email": "user@example.com"
}

Response:
{
  "success": true,
  "message": "OTP sent to your email for registration",
  "data": {
    "otpId": 123,
    "email": "user@example.com"
  }
}

# Step 2: Verify registration OTP
POST /api/auth/register/verify-otp
{
  "email": "user@example.com",
  "otp": "123456"
}

Response:
{
  "success": true,
  "message": "Email verified successfully. You can complete registration.",
  "data": {
    "otpVerified": true,
    "email": "user@example.com",
    "verifiedAt": "2024-12-20T10:30:00",
    "registrationStatus": "Email verified. Ready for registration completion"
  }
}
```

### 2. Password Reset Flow
```bash
# Step 1: Send password reset OTP
POST /api/auth/password/send-reset-otp
{
  "email": "user@example.com"
}

Response:
{
  "success": true,
  "message": "Password reset OTP sent to your email",
  "data": {
    "otpId": 124,
    "email": "user@example.com"
  }
}

# Step 2: Verify reset OTP and get reset token
POST /api/auth/password/verify-reset-otp
{
  "email": "user@example.com",
  "otp": "123456"
}

Response:
{
  "success": true,
  "message": "OTP verified. You can now reset your password.",
  "data": {
    "otpVerified": true,
    "email": "user@example.com",
    "resetTokenInfo": {
      "userId": 123,
      "resetToken": "uuid-reset-token-here"
    }
  }
}
```

## ⚙️ Cấu hình

### application.properties
```properties
# OTP Configuration  
app.otp.expiry-minutes=5          # OTP hết hạn sau 5 phút
app.otp.max-attempts=3            # Max 3 lần thử sai
app.otp.rate-limit-per-hour=5     # Max 5 OTP requests/giờ
```

## 📧 Email Templates

### Đăng ký tài khoản
```html
<h3>Mã xác thực đăng ký</h3>
<p>Mã xác thực để hoàn tất đăng ký tài khoản:</p>
<h2 style="color: #007bff;">123456</h2>
<p>Có hiệu lực trong 5 phút</p>
```

### Reset mật khẩu  
```html
<h3>Mã xác thực đặt lại mật khẩu</h3>
<p>Mã xác thực để đặt lại mật khẩu:</p>
<h2 style="color: #007bff;">123456</h2>
<p>Có hiệu lực trong 5 phút</p>
```

## 🛡️ Security Best Practices

### ✅ Implemented
- **No plain OTP storage** - Chỉ lưu hash
- **Rate limiting** - Chống spam requests
- **Attempt limiting** - Chống brute force
- **Auto cleanup** - Không để OTP cũ tồn tại
- **Secure random generation** - Không đoán được
- **Input validation** - Validate tất cả input
- **Error handling** - Không leak information

### 🔒 Production Checklist
- [ ] Configure proper email service (SMTP)
- [ ] Set up monitoring & alerting  
- [ ] Use environment variables for sensitive config
- [ ] Enable database indexes on frequently queried columns
- [ ] Set up proper logging (without OTP values)
- [ ] Configure rate limiting at reverse proxy level
- [ ] Implement CAPTCHA for high-risk operations

## 📊 Monitoring & Analytics

### Metrics to Track
- OTP generation rate per identifier
- Failed verification attempts
- Rate limit hits
- Email delivery success rate
- Average verification time

### Logs to Monitor
```
✅ Good logs:
- "OTP sent successfully to: user@example.com for REGISTER"  
- "OTP verified successfully for user@example.com"
- "Rate limit exceeded for user@example.com"

❌ Bad logs (NEVER do):
- "Generated OTP 123456 for user@example.com"
- "User entered wrong OTP: 123457"
```

## 🧪 Testing

### Unit Tests
```java
@Test
public void testOtpGeneration() {
    String otp = otpService.generateOtp();
    assertTrue(otp.length() == 6);
    assertTrue(Integer.parseInt(otp) >= 100000);
    assertTrue(Integer.parseInt(otp) <= 999999);
}

@Test 
public void testRateLimit() {
    // Send 5 OTPs (should succeed)
    for (int i = 0; i < 5; i++) {
        OtpResult result = otpService.sendOtp("test@example.com", OtpType.REGISTER);
        assertTrue(result.isSuccess());
    }
    
    // 6th OTP should be rate limited
    OtpResult result = otpService.sendOtp("test@example.com", OtpType.REGISTER);
    assertFalse(result.isSuccess());
    assertTrue(result.getMessage().contains("Rate limit"));
}
```

## 🚨 Error Handling

### Common Error Responses
```json
{
  "success": false,
  "message": "Rate limit exceeded. Too many OTP requests.",
  "data": null
}

{
  "success": false, 
  "message": "Invalid OTP. 2 attempts remaining.",
  "data": null
}

{
  "success": false,
  "message": "OTP not found or expired", 
  "data": null
}
```

## 🔄 Integration Examples

### Frontend Integration
```javascript
// Registration Flow
const registerWithOtp = async (email) => {
  try {
    // Step 1: Send registration OTP
    const sendResponse = await fetch('/api/auth/register/send-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email })
    });
    
    const sendResult = await sendResponse.json();
    if (sendResult.success) {
      showMessage('Registration OTP sent to your email');
      showOtpInput(); // Show OTP input form
    } else {
      showError(sendResult.message);
    }
  } catch (error) {
    showError('Network error');
  }
};

const verifyRegistrationOtp = async (email, otp) => {
  try {
    // Step 2: Verify registration OTP
    const verifyResponse = await fetch('/api/auth/register/verify-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email, otp: otp })
    });
    
    const verifyResult = await verifyResponse.json();
    if (verifyResult.success) {
      showMessage('Email verified! You can now complete registration.');
      // Proceed to registration completion
      showRegistrationForm();
    } else {
      showError(verifyResult.message);
    }
  } catch (error) {
    showError('Verification failed');
  }
};

// Password Reset Flow
const resetPasswordWithOtp = async (email) => {
  try {
    // Step 1: Send reset OTP
    const sendResponse = await fetch('/api/auth/password/send-reset-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email })
    });
    
    const sendResult = await sendResponse.json();
    if (sendResult.success) {
      showMessage('Password reset OTP sent to your email');
      showOtpInput(); // Show OTP input form
    } else {
      showError(sendResult.message);
    }
  } catch (error) {
    showError('Network error');
  }
};

const verifyResetOtp = async (email, otp) => {
  try {
    // Step 2: Verify reset OTP and get reset token
    const verifyResponse = await fetch('/api/auth/password/verify-reset-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email, otp: otp })
    });
    
    const verifyResult = await verifyResponse.json();
    if (verifyResult.success && verifyResult.data.resetTokenInfo) {
      showMessage('OTP verified! You can now reset your password.');
      // Store reset token for password change
      const resetToken = verifyResult.data.resetTokenInfo.resetToken;
      showPasswordResetForm(resetToken);
    } else {
      showError(verifyResult.message);
    }
  } catch (error) {
    showError('Verification failed');
  }
};
```

---

**🔐 Hệ thống OTP của bạn giờ đã AN TOÀN và SẴN SÀNG sử dụng!**