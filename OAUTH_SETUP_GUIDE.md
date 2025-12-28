# OAuth 2.0 Implementation - Next Steps

## ✅ Implemented Components

1. **Dependencies** - Added Google API Client to `pom.xml`
2. **Configuration** - Updated `application.properties` with OAuth settings
3. **DTOs** - Created OAuthLoginRequest, AuthResponse, GoogleUserInfo, UserDTO
4. **Services** - Created GoogleTokenVerifierService and OAuthService
5. **Entity** - Updated NguoiDung with OAuth fields
6. **Security** - Created JwtTokenProvider for token generation
7. **Controller** - Added `/oauth-login` endpoint to TaiKhoanController
8. **Database** - Created migration SQL script

## 🔧 Configuration Required

### 1. Update Google Client IDs in `application.properties`

```properties
# Replace with your actual Google Client IDs
google.client-id=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
google.allowed-client-ids=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com,YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com,YOUR_IOS_CLIENT_ID.apps.googleusercontent.com
```

**Where to get these:**
- Go to [Google Cloud Console](https://console.cloud.google.com)
- Create a project or select existing one
- Enable Google+ API
- Create OAuth 2.0 credentials for:
  - Web application (for backend verification)
  - Android (if you have Android app)
  - iOS (if you have iOS app)

### 2. Run Database Migration

Execute the SQL migration script:

```bash
# Using MySQL client
mysql -u root -p sach < Database/migration_oauth_support.sql

# Or using your preferred database tool
```

This will add:
- `oauth_provider` column (VARCHAR 20)
- `oauth_provider_id` column (VARCHAR 255)
- Indexes for better performance

### 3. Build the Project

```bash
mvn clean install
```

This will download the new Google API Client dependency and compile all the code.

## 🧪 Testing the OAuth Endpoint

### Using Postman

**Endpoint:** `POST http://localhost:8080/tai-khoan/oauth-login`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "provider": "google",
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ...",
  "email": "user@gmail.com",
  "displayName": "User Name"
}
```

**Expected Response:**
```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "maNguoiDung": 123,
    "email": "user@gmail.com",
    "ten": "User Name",
    "isAdmin": false,
    "isSeller": false,
    "anhDaiDien": "https://lh3.googleusercontent.com/..."
  }
}
```

## 🔒 Security Configuration

The `/oauth-login` endpoint needs to be publicly accessible. Make sure your SecurityConfig allows it:

```java
.authorizeHttpRequests()
    .requestMatchers("/tai-khoan/dang-nhap", "/tai-khoan/dang-ky", "/tai-khoan/oauth-login").permitAll()
```

This should already be configured if you have similar endpoints.

## 📱 Integration with Flutter/Frontend

Your frontend should:

1. Use Google Sign-In SDK to get the ID token
2. Send the ID token to your backend `/oauth-login` endpoint
3. Store the received JWT and refresh token
4. Use the JWT for authenticated API requests

Example Flutter integration:
```dart
final GoogleSignInAccount? googleUser = await GoogleSignIn().signIn();
final GoogleSignInAuthentication googleAuth = await googleUser!.authentication;

// Send to backend
final response = await http.post(
  Uri.parse('http://your-backend/tai-khoan/oauth-login'),
  body: jsonEncode({
    'provider': 'google',
    'idToken': googleAuth.idToken,
    'email': googleUser.email,
    'displayName': googleUser.displayName,
  }),
);
```

## 🐛 Troubleshooting

### Lombok Errors in IDE
The IDE may show compilation errors due to Lombok annotation processor issues. These are false positives. To verify:

```bash
mvn clean compile
```

If Maven compiles successfully, the code is correct.

### Token Verification Fails
- Ensure your Google Client IDs are correct
- Check that the ID token is fresh (they expire quickly)
- Verify the token audience matches your client IDs

### Database Errors
- Ensure the migration script ran successfully
- Check that columns were added: `DESCRIBE nguoi_dung;`

## 📚 Documentation

Refer to `BACKEND_OAUTH_IMPLEMENTATION.md` for detailed implementation guide and best practices.

## ✨ Features Implemented

- ✅ Google OAuth 2.0 Sign-In
- ✅ ID Token verification
- ✅ Automatic user creation/update
- ✅ JWT token generation
- ✅ Refresh token support
- ✅ Profile picture sync
- ✅ Email verification check
- ✅ Support for multiple OAuth providers (Apple ready)

## 🚀 Next Steps

1. Configure Google Client IDs
2. Run database migration
3. Build and test with Postman
4. Integrate with your frontend
5. (Optional) Implement Apple Sign-In
6. Add rate limiting for OAuth endpoints
7. Set up monitoring/logging

---

**Need help?** Check the implementation guide or test the endpoint with Postman first!
