# 📧🔴 REDIS OTP REGISTRATION FLOW TEST GUIDE

## 🎯 Flow Overview
```
STEP 1: User fills Flutter form (memory only)
STEP 2: Request OTP via Gmail → Store in Redis with TTL
STEP 3: Verify OTP from Redis → Get temporary JWT
STEP 4: Submit registration with temp JWT → Clean Redis
```

## 🔴 **Redis Benefits:**
✅ **Auto Expiration**: TTL = 5 minutes  
✅ **Rate Limiting**: Max 3 OTP/email per 10 min  
✅ **Memory Performance**: Faster than DB  
✅ **Auto Cleanup**: No manual deletion needed  
✅ **Attempt Tracking**: Max 3 wrong attempts  

## 🧪 Testing with Postman

### 🔹 STEP 2: Request OTP via Gmail
```http
POST http://localhost:8080/api/auth/send-otp
Content-Type: application/json

{
  "email": "test@gmail.com"
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "OTP sent to your email",
  "data": {
    "otpId": "UUID",
    "email": "test@gmail.com"
  }
}
```
📝 **Redis Storage:** `otp:test@gmail.com:REGISTER` with 5-min TTL

### 🔹 STEP 3: Verify OTP (from Redis)
```http
POST http://localhost:8080/api/auth/verify-otp
Content-Type: application/json

{
  "email": "test@gmail.com",
  "otp": "123456"
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "otpVerified": true,
    "email": "test@gmail.com",
    "otpVerifiedToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenExpiresIn": "10 minutes",
    "message": "OTP verified successfully. Use this token to complete registration."
  }
}
```
📝 **Redis Update:** OTP marked as verified, TTL extended to 30min

### 🔹 STEP 4: Complete Registration (+ Redis Cleanup)
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "email": "test@gmail.com",
  "password": "Strong@123",
  "fullName": "Nguyen Van A",
  "phoneNumber": "0987654321"
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Registration completed successfully!",
  "data": {
    "userId": 123,
    "email": "test@gmail.com",
    "hoTen": "Nguyen Van A",
    "tenDangNhap": "test_1703123456789",
    "soDienThoai": "0987654321",
    "registrationStatus": "Registration completed successfully",
    "createdAt": "2024-12-21T10:30:45",
    "daKichHoat": true
  }
}
```
📝 **Redis Cleanup:** OTP automatically deleted after successful registration

## 🔴 **Redis-Specific Features**

### 🛡️ Rate Limiting Test
```http
POST http://localhost:8080/api/auth/send-otp
# Send 4 times rapidly with same email

4th Response:
{
  "success": false,
  "message": "Too many OTP requests. Please wait 10 minutes."
}
```

### 🔍 OTP Statistics (Debug)
```http
POST http://localhost:8080/api/auth/otp-stats
Content-Type: application/json

{
  "email": "test@gmail.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP statistics retrieved from Redis",
  "data": {
    "otpExists": true,
    "otpTTL": 298,
    "requestCount": 2,
    "rateLimitTTL": 567
  }
}
```

### ⏰ Auto Expiration Test
```
1. Send OTP
2. Wait 5+ minutes
3. Try verify → "OTP expired or not found"
```

### 🚫 Attempt Limiting Test
```
1. Send OTP
2. Try wrong OTP 3 times
3. 4th attempt → "Too many incorrect attempts. OTP invalidated."
```

## 🔧 **Redis Key Structure**
```
otp:email@domain.com:REGISTER      → OTP data (5min TTL)
otp_rate_limit:email@domain.com    → Request count (10min TTL)
```

## ✅ **Key Features**

### 🔒 Security Features
- **Redis TTL**: Auto expiration in 5 minutes
- **Rate limiting**: Max 3 OTP requests per 10 minutes  
- **Attempt tracking**: Max 3 wrong attempts
- **Token validation**: Must match email in JWT
- **Auto cleanup**: OTP deleted after registration

### 📱 UX Benefits
- **One-time form**: User fills form once in Flutter
- **Memory storage**: Flutter keeps data in memory
- **Fast performance**: Redis memory-based storage
- **Real-time**: Instant OTP validation

### 🔴 Redis Advantages
- **High Performance**: Memory-based storage
- **Built-in TTL**: Automatic expiration
- **Rate Limiting**: Prevents abuse
- **Scalability**: Handles high concurrency
- **Monitoring**: Easy stats tracking

### 🛡️ Error Handling
```json
// OTP Expired
{
  "success": false,
  "message": "OTP expired or not found"
}

// Rate Limited
{
  "success": false,
  "message": "Too many OTP requests. Please wait 10 minutes."
}

// Too Many Attempts
{
  "success": false,
  "message": "Too many incorrect attempts. OTP invalidated."
}

// Email already exists
{
  "success": false,
  "message": "Email already registered"
}
```

## 🚀 **Deployment Notes**

### Redis Configuration Required:
```properties
# application.properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.database=0
spring.redis.timeout=2000ms
```

### For Production:
- **Redis Cluster** for high availability
- **Redis Persistence** for backup
- **Monitoring** with Redis stats endpoint
- **Security** with Redis AUTH password

## 🔧 Development Notes

### For Flutter:
```dart
class RegistrationForm {
  String phoneNumber;
  String password;
  String fullName;
}

// Step 1: Collect data
RegistrationForm form = collectUserInput();

// Step 2: Request OTP
sendOtp(form.phoneNumber);

// Step 3: Verify OTP
String tempToken = verifyOtp(form.phoneNumber, otp);

// Step 4: Complete registration
registerUser(tempToken, form);
```

### Security Considerations:
- Temporary JWT uses separate secret key
- Token includes phone number validation
- 10-minute expiration prevents token reuse
- Phone number must match in all steps

## 🚀 Advantages of New Flow

1. **Better UX**: User doesn't re-enter data
2. **Security**: JWT-based temporary authorization
3. **Stateless**: Backend doesn't store temp data
4. **Mobile-friendly**: Fewer screen transitions
5. **Clean code**: Clear separation of concerns
6. **Scalable**: Easy to add more validation steps