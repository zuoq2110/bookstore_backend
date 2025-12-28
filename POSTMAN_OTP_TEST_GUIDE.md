# Postman Test Guide - OTP Registration Flow

## 🚀 Complete OTP Registration Testing

### Prerequisites
1. Ensure application is running on `http://localhost:8080`
2. Gmail SMTP is configured correctly in `application.properties`
3. Database is connected and running

---

## 📧 **Step 1: Send Registration OTP**

### Request
```
POST http://localhost:8080/api/auth/register/send-otp
Content-Type: application/json

{
  "email": "your-test-email@gmail.com"
}
```

### Expected Response (Success)
```json
{
  "success": true,
  "message": "OTP sent to your email for registration",
  "data": {
    "otpId": 123,
    "email": "your-test-email@gmail.com"
  }
}
```

### Expected Response (Error)
```json
{
  "success": false,
  "message": "Rate limit exceeded. Too many OTP requests."
}
```

### ✅ **What to verify:**
- Status code: `200 OK` for success
- Check email inbox for OTP (6-digit code)
- Response contains `otpId` and `email`

---

## 🔐 **Step 2: Verify OTP**

### Request
```
POST http://localhost:8080/api/auth/register/verify-otp
Content-Type: application/json

{
  "email": "your-test-email@gmail.com",
  "otp": "123456"
}
```

### Expected Response (Success)
```json
{
  "success": true,
  "message": "Email verified successfully. You can complete registration.",
  "data": {
    "otpVerified": true,
    "email": "your-test-email@gmail.com",
    "verifiedAt": "2024-12-21T10:30:00",
    "registrationStatus": "Email verified. Ready for registration completion"
  }
}
```

### Expected Response (Wrong OTP)
```json
{
  "success": false,
  "message": "Invalid OTP. 2 attempts remaining."
}
```

### ✅ **What to verify:**
- Status code: `200 OK` for success
- `otpVerified: true`
- `registrationStatus` indicates ready for completion

---

## 📝 **Step 3: Complete Registration**

### Request
```
POST http://localhost:8080/api/auth/register/complete
Content-Type: application/json

{
  "email": "your-test-email@gmail.com",
  "matKhau": "password123",
  "hoTen": "Nguyễn Văn A",
  "tenDangNhap": "nguyenvana",
  "soDienThoai": "0123456789",
  "gioiTinh": "M",
  "diaChi": "123 ABC Street"
}
```

### Required Fields
- `email` ✅ 
- `matKhau` ✅
- `hoTen` ✅

### Optional Fields
- `tenDangNhap` (auto-generated if not provided)
- `soDienThoai`
- `gioiTinh` (`M` or `F`)
- `diaChi`

### Expected Response (Success)
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công!",
  "data": {
    "userId": 123,
    "email": "your-test-email@gmail.com",
    "hoTen": "Nguyễn Văn A",
    "tenDangNhap": "nguyenvana",
    "registrationStatus": "Registration completed successfully",
    "createdAt": "2024-12-21T10:35:00",
    "daKichHoat": true
  }
}
```

### Expected Response (Error - Email exists)
```json
{
  "success": false,
  "message": "Email đã được sử dụng"
}
```

### ✅ **What to verify:**
- Status code: `200 OK` for success
- User is created in database with `daKichHoat = true`
- `userId` is returned
- No activation email sent (since already verified via OTP)

---

## 🧪 **Alternative Tests**

### Test 1: Invalid Email Format
```json
POST /api/auth/register/send-otp
{
  "email": "invalid-email"
}
```
**Expected:** `400 Bad Request`

### Test 2: Empty OTP
```json
POST /api/auth/register/verify-otp
{
  "email": "test@gmail.com",
  "otp": ""
}
```
**Expected:** `400 Bad Request` - "OTP is required"

### Test 3: Expired OTP
- Send OTP
- Wait 6+ minutes (OTP expires after 5 minutes)
- Try to verify
**Expected:** `400 Bad Request` - "OTP not found or expired"

### Test 4: Rate Limiting
- Send 6 OTPs to same email within 1 hour
**Expected:** 6th request returns "Rate limit exceeded"

### Test 5: Username Conflict
```json
POST /api/auth/register/complete
{
  "email": "test2@gmail.com",
  "matKhau": "password123",
  "hoTen": "Test User",
  "tenDangNhap": "existing_username"
}
```
**Expected:** `400 Bad Request` - "Tên đăng nhập đã tồn tại"

---

## 🔍 **Debug Information**

### Check Server Logs for:
```
[log] Attempting to send OTP email to: test@gmail.com
[log] OTP email sent successfully to: test@gmail.com
[log] OTP sent successfully to: test@gmail.com for REGISTER
```

### Check Database Tables:
1. **otp_verification** - OTP records
2. **nguoi_dung** - User accounts
3. **nguoi_dung_quyen** - User roles

### Common Issues:
1. **401 Unauthorized**: Check if `/api/auth/**` is in public endpoints
2. **Email not sent**: Check SMTP configuration and Gmail app password
3. **500 Error**: Check database connection and role "CUSTOMER" exists

---

## 📋 **Test Checklist**

- [ ] Step 1: Send OTP successfully
- [ ] Email received with 6-digit OTP
- [ ] Step 2: Verify OTP successfully  
- [ ] Step 3: Complete registration successfully
- [ ] User created in database with correct data
- [ ] User can login with new account
- [ ] Test error cases (wrong OTP, duplicate email, etc.)
- [ ] Test rate limiting
- [ ] Test field validation

---

## 🚨 **If Tests Fail**

1. **Check application.properties** for email config
2. **Verify database connection** and tables exist
3. **Check logs** for detailed error messages
4. **Test email endpoint**: `/api/auth/test-email`
5. **Verify security config** allows `/api/auth/**` 

## 🎯 **Success Criteria**
✅ Complete 3-step registration flow works end-to-end
✅ User account created and activated automatically  
✅ No activation email needed (OTP replaces it)
✅ Can login immediately after registration