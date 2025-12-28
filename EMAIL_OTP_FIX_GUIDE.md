# Email OTP Sending Issue - Fix Guide

## Problem Summary
You were getting a **401 Unauthorized** error with an empty response body when trying to send registration OTP, and after fixing the email issue, **registration was not saving users to database**.

## Root Causes & Fixes Applied

### 1. ✅ EmailServiceImpl - Incomplete Error Handling
**Issue**: The `emailSender.send(message)` call was outside the try-catch block, so SMTP errors weren't being caught properly.

**Fix Applied**:
- Moved `emailSender.send(message)` inside the try-catch block
- Added proper exception logging with stack traces
- Now catches both `MessagingException` and general `Exception`

**File**: [EmailServiceImpl.java](src/main/java/com/example/web_ban_sach/Service/EmailServiceImpl.java)

### 2. ✅ Security Configuration - 401 Unauthorized
**Issue**: `/api/auth/**` endpoints were not included in public endpoints, causing 401 errors.

**Fix Applied**:
- Added `/api/auth/**` to `PUBLIC_POST_ENDPOINTS` in [Endpoints.java](src/main/java/com/example/web_ban_sach/security/Endpoints.java)
- Now OTP endpoints are accessible without authentication

### 3. ✅ Missing Registration Completion Endpoint
**Issue**: After OTP verification, there was no endpoint to actually create the user account in database.

**Fix Applied**:
- Added `/api/auth/register/complete` endpoint in [AuthOtpController.java](src/main/java/com/example/web_ban_sach/controller/AuthOtpController.java)
- Handles full user creation with password, name, optional fields
- Assigns default "CUSTOMER" role
- Saves user to database

### 4. ✅ Enhanced Error Logging
**Issue**: Error messages weren't detailed enough for debugging.

**Fix Applied**:
- Added comprehensive logging in OtpService and AuthOtpController
- Better error messages for client debugging

## Complete OTP Registration Flow

### Step 1: Send Registration OTP
```bash
POST /api/auth/register/send-otp
{
  "email": "user@example.com"
}
```

### Step 2: Verify OTP
```bash
POST /api/auth/register/verify-otp
{
  "email": "user@example.com", 
  "otp": "123456"
}
```

### Step 3: Complete Registration (NEW!)
```bash
POST /api/auth/register/complete
{
  "email": "user@example.com",
  "matKhau": "password123",
  "hoTen": "Nguyen Van A",
  "tenDangNhap": "nguyenvana", // optional
  "soDienThoai": "0123456789", // optional
  "gioiTinh": "M", // optional: M/F
  "diaChi": "123 ABC Street" // optional
}
```

**Response**:
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công!",
  "data": {
    "userId": 123,
    "email": "user@example.com",
    "hoTen": "Nguyen Van A",
    "tenDangNhap": "nguyenvana",
    "registrationStatus": "Registration completed successfully",
    "createdAt": "2024-12-21T10:30:00"
  }
}
```

## Verification Checklist

### Step 1: Verify Gmail Configuration
Check your `application.properties` email settings:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=zuoq2110@gmail.com
spring.mail.password=pxwrchckhjpqnxrx
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Step 2: Common Gmail Issues
If you still get errors, check:

1. **Gmail App Password Expired?**
   - Go to: https://myaccount.google.com/apppasswords
   - Make sure you have 2-Step Verification enabled
   - Generate a new app password for "Mail" and "Windows/Linux"
   - Copy the 16-character password (no spaces) and update `spring.mail.password`

2. **Less Secure Apps?**
   - If you're not using an app password, enable "Less secure app access"
   - Go to: https://myaccount.google.com/lesssecureapps

3. **Wrong Port?**
   - Port 587 (TLS) is correct
   - Port 465 (SSL) also works if you change `starttls.enable` to `starttls.required`

### Step 3: Test the Fix
1. Rebuild the project: `mvn clean install`
2. Run the application
3. Test with the new test endpoint:
   ```bash
   curl -X POST http://localhost:8080/api/auth/test-email \
     -H "Content-Type: application/json" \
     -d '{"email":"your-test-email@gmail.com"}'
   ```

4. Check the console logs for detailed error messages

## What to Look for in Logs

### Successful Email Send:
```
[log] Sending registration OTP for email: ntduong@gmail.com
[log] Attempting to send OTP email to: ntduong@gmail.com
[log] OTP email sent successfully to: ntduong@gmail.com
[log] OTP sent successfully to: ntduong@gmail.com for REGISTER
```

### Failed Email Send (with Details):
```
[log] MessagingException while sending email to ntduong@gmail.com: 535 5.7.8 Username and password not accepted
```

or

```
[log] Failed to send OTP email to ntduong@gmail.com: javax.mail.AuthenticationFailedException
```

## If Issues Persist

1. **Enable Debug Logging**: Add to `application.properties`:
   ```properties
   logging.level.org.springframework.mail=DEBUG
   logging.level.jakarta.mail=DEBUG
   ```

2. **Test SMTP Connection**: Use a tool like `telnet`:
   ```bash
   telnet smtp.gmail.com 587
   ```

3. **Check Firewall**: Ensure port 587 is open for outbound connections

4. **Verify from Email Address**: In OtpService, the email is sent from `noreply@webbansach.com`, which may need to be changed to your Gmail address or an authorized sender address

## Files Modified
- [EmailServiceImpl.java](src/main/java/com/example/web_ban_sach/Service/EmailServiceImpl.java) - Fixed email sending
- [OtpService.java](src/main/java/com/example/web_ban_sach/Service/OtpService.java) - Enhanced error logging
- [AuthOtpController.java](src/main/java/com/example/web_ban_sach/controller/AuthOtpController.java) - Added `/register/complete` endpoint
- [Endpoints.java](src/main/java/com/example/web_ban_sach/security/Endpoints.java) - Made `/api/auth/**` public

## Frontend Integration Example
```javascript
// Complete registration flow
async function registerUser(email, password, fullName, phone) {
  try {
    // Step 1: Send OTP
    const otpResponse = await fetch('/api/auth/register/send-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    
    if (!otpResponse.ok) return false;
    
    // Step 2: Get OTP from user input and verify
    const otp = prompt("Enter OTP from email:");
    const verifyResponse = await fetch('/api/auth/register/verify-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, otp })
    });
    
    if (!verifyResponse.ok) return false;
    
    // Step 3: Complete registration
    const completeResponse = await fetch('/api/auth/register/complete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        matKhau: password,
        hoTen: fullName,
        soDienThoai: phone
      })
    });
    
    return completeResponse.ok;
  } catch (error) {
    console.error('Registration failed:', error);
    return false;
  }
}
```
