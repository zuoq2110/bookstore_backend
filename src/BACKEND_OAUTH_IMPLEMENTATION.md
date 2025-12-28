# Backend Spring Boot - OAuth 2.0 Google Sign-In Implementation

## 📋 Tổng quan

Backend cần:
1. Verify ID token từ Google
2. Tạo hoặc cập nhật user trong database
3. Generate JWT token cho session
4. Return user info và tokens

---

## 🔧 Bước 1: Thêm Dependencies

**File: `pom.xml`**

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Google API Client - Verify Google ID Token -->
    <dependency>
        <groupId>com.google.api-client</groupId>
        <artifactId>google-api-client</artifactId>
        <version>2.2.0</version>
    </dependency>

    <!-- JPA/Hibernate -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Database driver (MySQL/PostgreSQL) -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok (optional) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 🔧 Bước 2: Cấu hình Application Properties

**File: `application.yml` hoặc `application.properties`**

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

# Google OAuth Configuration
google:
  client-id: YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
  # List of allowed client IDs (Web, Android, iOS)
  allowed-client-ids:
    - YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
    - YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com
    - YOUR_IOS_CLIENT_ID.apps.googleusercontent.com

# JWT Configuration
jwt:
  secret: your-secret-key-at-least-256-bits-long-for-hs256-algorithm
  expiration: 86400000 # 24 hours in milliseconds
  refresh-expiration: 604800000 # 7 days in milliseconds
```

---

## 🔧 Bước 3: DTO Classes

### OAuthLoginRequest.java
```java
package com.bookstore.dto;

import lombok.Data;

@Data
public class OAuthLoginRequest {
    private String provider;      // "google" hoặc "apple"
    private String idToken;        // ID token từ OAuth provider
    private String accessToken;    // Access token (optional)
    private String email;          // Email từ OAuth
    private String displayName;    // Tên hiển thị
}
```

### AuthResponse.java
```java
package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String jwt;
    private String refreshToken;
    private UserDTO user;
}
```

### GoogleUserInfo.java
```java
package com.bookstore.dto;

import lombok.Data;

@Data
public class GoogleUserInfo {
    private String sub;           // Google user ID
    private String email;
    private boolean emailVerified;
    private String name;
    private String picture;
    private String givenName;
    private String familyName;
    private String locale;
}
```

---

## 🔧 Bước 4: Google Token Verifier Service

### GoogleTokenVerifierService.java
```java
package com.bookstore.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.bookstore.dto.GoogleUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GoogleTokenVerifierService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("#{'${google.allowed-client-ids}'.split(',')}")
    private List<String> allowedClientIds;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(allowedClientIds)
                .build();
        
        log.info("Google Token Verifier initialized with client IDs: {}", allowedClientIds);
    }

    /**
     * Verify Google ID Token và trả về user info
     */
    public GoogleUserInfo verifyToken(String idTokenString) throws Exception {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken == null) {
                log.error("Invalid Google ID token");
                throw new IllegalArgumentException("Invalid ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify email
            if (!payload.getEmailVerified()) {
                log.error("Email not verified for user: {}", payload.getEmail());
                throw new IllegalArgumentException("Email not verified");
            }

            // Convert to GoogleUserInfo
            GoogleUserInfo userInfo = new GoogleUserInfo();
            userInfo.setSub(payload.getSubject());
            userInfo.setEmail(payload.getEmail());
            userInfo.setEmailVerified(payload.getEmailVerified());
            userInfo.setName((String) payload.get("name"));
            userInfo.setPicture((String) payload.get("picture"));
            userInfo.setGivenName((String) payload.get("given_name"));
            userInfo.setFamilyName((String) payload.get("family_name"));
            userInfo.setLocale((String) payload.get("locale"));

            log.info("Successfully verified Google token for user: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            throw new Exception("Failed to verify Google ID token", e);
        }
    }
}
```

---

## 🔧 Bước 5: OAuth Authentication Service

### OAuthService.java
```java
package com.bookstore.service;

import com.bookstore.dto.GoogleUserInfo;
import com.bookstore.dto.OAuthLoginRequest;
import com.bookstore.model.NguoiDung;
import com.bookstore.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final GoogleTokenVerifierService googleTokenVerifier;
    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Xử lý đăng nhập OAuth
     */
    @Transactional
    public NguoiDung authenticateWithOAuth(OAuthLoginRequest request) throws Exception {
        
        if ("google".equalsIgnoreCase(request.getProvider())) {
            return authenticateWithGoogle(request);
        } else if ("apple".equalsIgnoreCase(request.getProvider())) {
            return authenticateWithApple(request);
        } else {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + request.getProvider());
        }
    }

    /**
     * Đăng nhập với Google
     */
    private NguoiDung authenticateWithGoogle(OAuthLoginRequest request) throws Exception {
        log.info("Authenticating with Google for token: {}...", 
                request.getIdToken().substring(0, Math.min(20, request.getIdToken().length())));

        // 1. Verify Google ID token
        GoogleUserInfo googleUser = googleTokenVerifier.verifyToken(request.getIdToken());

        // 2. Tìm hoặc tạo user
        return findOrCreateUser(
                googleUser.getEmail(),
                googleUser.getName(),
                "google",
                googleUser.getSub(),
                googleUser.getPicture()
        );
    }

    /**
     * Đăng nhập với Apple (implement tương tự)
     */
    private NguoiDung authenticateWithApple(OAuthLoginRequest request) throws Exception {
        // TODO: Implement Apple Sign-In verification
        // Tương tự Google nhưng dùng Apple's public keys để verify JWT
        throw new UnsupportedOperationException("Apple Sign-In not yet implemented");
    }

    /**
     * Tìm hoặc tạo user trong database
     */
    @Transactional
    private NguoiDung findOrCreateUser(
            String email, 
            String displayName, 
            String provider,
            String providerId,
            String avatarUrl) {
        
        // Tìm user theo email
        Optional<NguoiDung> existingUser = nguoiDungRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // User đã tồn tại - cập nhật thông tin
            NguoiDung user = existingUser.get();
            
            // Update OAuth info nếu chưa có
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(provider);
                user.setOauthProviderId(providerId);
            }
            
            // Update avatar nếu có
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAnhDaiDien(avatarUrl);
            }
            
            // Update last login
            user.setNgayCapNhat(LocalDateTime.now());
            
            log.info("Existing user found and updated: {}", email);
            return nguoiDungRepository.save(user);
            
        } else {
            // Tạo user mới
            NguoiDung newUser = new NguoiDung();
            newUser.setEmail(email);
            newUser.setTen(displayName != null ? displayName : email.split("@")[0]);
            newUser.setOauthProvider(provider);
            newUser.setOauthProviderId(providerId);
            newUser.setAnhDaiDien(avatarUrl);
            
            // Generate random password (user won't use it)
            String randomPassword = UUID.randomUUID().toString();
            newUser.setMatKhau(passwordEncoder.encode(randomPassword));
            
            // Set default values
            newUser.setTrangThaiTaiKhoan(true);  // Activated
            newUser.setIsAdmin(false);
            newUser.setIsSeller(false);
            newUser.setNgayTao(LocalDateTime.now());
            newUser.setNgayCapNhat(LocalDateTime.now());
            
            log.info("Creating new user from OAuth: {}", email);
            return nguoiDungRepository.save(newUser);
        }
    }
}
```

---

## 🔧 Bước 6: Update Entity NguoiDung

Thêm các trường OAuth vào entity:

```java
@Entity
@Table(name = "nguoi_dung")
public class NguoiDung {
    
    // ... existing fields ...
    
    @Column(name = "oauth_provider")
    private String oauthProvider;  // "google", "apple", null
    
    @Column(name = "oauth_provider_id")
    private String oauthProviderId;  // Google sub hoặc Apple user ID
    
    @Column(name = "anh_dai_dien")
    private String anhDaiDien;  // Avatar URL
    
    // ... getters/setters ...
}
```

**SQL để update database:**
```sql
ALTER TABLE nguoi_dung 
ADD COLUMN oauth_provider VARCHAR(20) NULL,
ADD COLUMN oauth_provider_id VARCHAR(255) NULL,
ADD COLUMN anh_dai_dien VARCHAR(500) NULL;

-- Add index for faster lookups
CREATE INDEX idx_oauth_provider ON nguoi_dung(oauth_provider, oauth_provider_id);
```

---

## 🔧 Bước 7: Controller

### AuthController.java
```java
package com.bookstore.controller;

import com.bookstore.dto.AuthResponse;
import com.bookstore.dto.OAuthLoginRequest;
import com.bookstore.dto.UserDTO;
import com.bookstore.model.NguoiDung;
import com.bookstore.service.OAuthService;
import com.bookstore.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/tai-khoan")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthService oauthService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Endpoint OAuth Login
     * POST /tai-khoan/oauth-login
     */
    @PostMapping("/oauth-login")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        try {
            log.info("OAuth login request - Provider: {}, Email: {}", 
                    request.getProvider(), request.getEmail());

            // 1. Authenticate with OAuth provider
            NguoiDung user = oauthService.authenticateWithOAuth(request);

            // 2. Generate JWT tokens
            String jwt = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 3. Convert to DTO
            UserDTO userDTO = convertToDTO(user);

            // 4. Return response
            AuthResponse response = new AuthResponse(jwt, refreshToken, userDTO);
            
            log.info("OAuth login successful for user: {}", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid OAuth request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid request",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("OAuth login failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Authentication failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Convert NguoiDung to UserDTO
     */
    private UserDTO convertToDTO(NguoiDung user) {
        UserDTO dto = new UserDTO();
        dto.setMaNguoiDung(user.getMaNguoiDung());
        dto.setEmail(user.getEmail());
        dto.setTen(user.getTen());
        dto.setIsAdmin(user.getIsAdmin());
        dto.setIsSeller(user.getIsSeller());
        dto.setAnhDaiDien(user.getAnhDaiDien());
        dto.setSoDienThoai(user.getSoDienThoai());
        dto.setDiaChiGiaoHang(user.getDiaChiGiaoHang());
        // ... other fields ...
        return dto;
    }
}
```

---

## 🔧 Bước 8: JWT Token Provider (nếu chưa có)

### JwtTokenProvider.java
```java
package com.bookstore.security;

import com.bookstore.model.NguoiDung;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT access token
     */
    public String generateToken(NguoiDung user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getMaNguoiDung()))
                .claim("email", user.getEmail())
                .claim("isAdmin", user.getIsAdmin())
                .claim("isSeller", user.getIsSeller())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(NguoiDung user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getMaNguoiDung()))
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate and parse JWT token
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }
}
```

---

## 🔧 Bước 9: Security Configuration

### SecurityConfig.java
```java
package com.bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/tai-khoan/dang-nhap", "/tai-khoan/dang-ky", "/tai-khoan/oauth-login").permitAll()
                .anyRequest().authenticated();

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## ✅ Testing

### Test với Postman:

**Endpoint**: `POST http://localhost:8080/tai-khoan/oauth-login`

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "provider": "google",
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ...",
  "email": "user@gmail.com",
  "displayName": "User Name"
}
```

**Expected Response**:
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

---

## 🔒 Security Best Practices

1. ✅ **Always verify ID tokens** trên server side
2. ✅ **Không tin tưởng data từ client** - Always validate
3. ✅ **Store OAuth provider ID** để phòng email changes
4. ✅ **Use HTTPS** trong production
5. ✅ **Rotate JWT secrets** định kỳ
6. ✅ **Implement rate limiting** cho OAuth endpoints
7. ✅ **Log OAuth attempts** để monitor

---

## 📚 Dependencies Summary

```xml
<!-- Minimum required -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
```

---

## 🎯 Checklist Backend

- [ ] Thêm dependencies vào `pom.xml`
- [ ] Cấu hình `application.yml` với Google Client ID
- [ ] Tạo `GoogleTokenVerifierService`
- [ ] Tạo `OAuthService`
- [ ] Update entity `NguoiDung` với OAuth fields
- [ ] Chạy SQL migration để thêm columns
- [ ] Tạo controller endpoint `/oauth-login`
- [ ] Configure Security để permit OAuth endpoint
- [ ] Test với Postman
- [ ] Test với Flutter app

---

**Done!** Backend Spring Boot giờ đã hỗ trợ Google OAuth 2.0 Sign-In! 🚀
