# Tóm Tắt Quy Trình Xác Thực JWT (JSON Web Token)

## 📋 Tổng Quan

Hệ thống sử dụng **JWT (JSON Web Token)** để xác thực và ủy quyền người dùng. JWT Authentication hoạt động theo mô hình **stateless**, không lưu trữ session trên server mà sử dụng token chứa thông tin được ký số để xác thực.

### 🎯 Đặc Điểm JWT System

- **🔐 Stateless Authentication** - Không cần lưu session server-side
- **🔄 Access + Refresh Token** - Dual token strategy cho security cao
- **⚡ Performance** - Giảm database queries cho auth
- **🌐 Cross-Platform** - Hoạt động tốt với web, mobile, microservices
- **🛡️ Security** - Signed tokens với HMAC SHA-256

---

## 🏗️ JWT Architecture Overview

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   Client App        │    │   Spring Backend    │    │      Database       │
├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤
│ • JWT Storage       │    │ • JWT Generation    │    │ • User Data         │
│ • Auto Refresh      │◄──►│ • Token Validation  │◄──►│ • Role & Permissions│
│ • API Requests      │    │ • Security Filter   │    │ • Account Status    │
│ • Token Management  │    │ • Refresh Logic     │    │ • Login History     │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
                                       │
                           ┌─────────────────────┐
                           │   JWT Components    │
                           ├─────────────────────┤
                           │ • Header (alg,typ)  │
                           │ • Payload (claims)  │
                           │ • Signature (HMAC)  │
                           └─────────────────────┘
```

---

## 🔄 Complete JWT Authentication Flow

### 1. Initial Login Process

```mermaid
sequenceDiagram
    participant C as Client
    participant B as Backend
    participant DB as Database
    participant JWT as JWT Service
    
    Note over C,JWT: Standard Login Flow
    
    C->>B: POST /tai-khoan/dang-nhap
    Note over C: {username, password}
    
    B->>DB: Validate credentials
    DB-->>B: User data (if valid)
    
    alt Valid credentials & no 2FA
        B->>JWT: Generate access token
        JWT-->>B: Access JWT (15 min)
        B->>JWT: Generate refresh token  
        JWT-->>B: Refresh JWT (7 days)
        
        B-->>C: 200 OK + tokens
        Note over B: {jwt, refreshToken, user}
        
        C->>C: Store tokens
        Note over C: localStorage/cookies
        
    else Invalid credentials
        B-->>C: 401 Unauthorized
        
    else 2FA required
        B-->>C: 403 + MFA token
        Note over B: Proceed to 2FA flow
    end
```

### 2. JWT Token Structure

```mermaid
graph TD
    A[JWT Token] --> B[Header]
    A --> C[Payload]
    A --> D[Signature]
    
    B --> B1[Algorithm: HS256]
    B --> B2[Type: JWT]
    
    C --> C1[User ID]
    C --> C2[Email]
    C --> C3[Roles]
    C --> C4[Issued At]
    C --> C5[Expires At]
    C --> C6[Token Type]
    
    D --> D1[HMAC SHA-256]
    D --> D2[Secret Key]
    
    style A fill:#e1f5fe
    style B fill:#f3e5f5
    style C fill:#e8f5e8
    style D fill:#fff3e0
```

### 3. API Request with JWT

```mermaid
sequenceDiagram
    participant C as Client
    participant F as Security Filter
    participant JWT as JWT Service
    participant API as API Controller
    participant DB as Database
    
    Note over C,DB: Protected API Request
    
    C->>F: API Request + Authorization Header
    Note over C: Bearer eyJhbGciOiJIUzI1NiIs...
    
    F->>JWT: Extract & validate token
    JWT->>JWT: Check signature
    JWT->>JWT: Check expiration
    JWT->>JWT: Extract claims
    
    alt Token valid
        JWT-->>F: User details
        F->>API: Forward request + user context
        API->>DB: Business logic
        DB-->>API: Data response
        API-->>C: 200 OK + data
        
    else Token invalid/expired
        JWT-->>F: Token invalid
        F-->>C: 401 Unauthorized
        Note over F: {error: "Invalid token"}
        
    else Token expired
        JWT-->>F: Token expired
        F-->>C: 401 + refresh hint
        Note over F: {error: "Token expired", shouldRefresh: true}
    end
```

### 4. Token Refresh Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant B as Backend
    participant JWT as JWT Service
    participant DB as Database
    
    Note over C,DB: Automatic Token Refresh
    
    C->>C: Detect access token expired
    Note over C: API returns 401
    
    C->>B: POST /tai-khoan/refresh
    Note over C: {refreshToken}
    
    B->>JWT: Validate refresh token
    JWT->>JWT: Check signature & expiration
    
    alt Refresh token valid
        JWT->>DB: Check user still active
        DB-->>JWT: User status
        
        alt User active
            JWT->>JWT: Generate new access token
            JWT->>JWT: Generate new refresh token
            JWT-->>B: New token pair
            
            B-->>C: 200 OK + new tokens
            Note over B: {jwt, refreshToken}
            
            C->>C: Update stored tokens
            C->>C: Retry original request
            
        else User inactive/deleted
            JWT-->>B: User invalid
            B-->>C: 401 + force re-login
        end
        
    else Refresh token invalid/expired
        JWT-->>B: Token invalid
        B-->>C: 401 + force re-login
        
        C->>C: Clear all tokens
        C->>C: Redirect to login
    end
```

---

## 🔧 JWT Implementation Details

### 1. JWT Token Generation

```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration:900}") // 15 minutes
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration:604800}") // 7 days  
    private Long refreshTokenExpiration;
    
    // Generate Access Token
    public String generateAccessToken(NguoiDung nguoiDung) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", nguoiDung.getMaNguoiDung());
        claims.put("email", nguoiDung.getEmail());
        claims.put("roles", getUserRoles(nguoiDung));
        claims.put("tokenType", "access");
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(nguoiDung.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
    
    // Generate Refresh Token
    public String generateRefreshToken(NguoiDung nguoiDung) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", nguoiDung.getMaNguoiDung());
        claims.put("tokenType", "refresh");
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(nguoiDung.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration * 1000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
    
    private List<String> getUserRoles(NguoiDung nguoiDung) {
        List<String> roles = new ArrayList<>();
        if (nguoiDung.isAdmin()) roles.add("ADMIN");
        if (nguoiDung.isSeller()) roles.add("SELLER");
        roles.add("USER");
        return roles;
    }
}
```

### 2. JWT Token Validation

```java
@Component
public class JwtTokenValidator {
    
    @Autowired
    private JwtService jwtService;
    
    public boolean validateToken(String token) {
        try {
            Claims claims = jwtService.getClaimsFromToken(token);
            
            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                return false;
            }
            
            // Check token type for access tokens
            String tokenType = (String) claims.get("tokenType");
            if (!"access".equals(tokenType)) {
                return false;
            }
            
            // Check if user still exists and is active
            Integer userId = (Integer) claims.get("userId");
            NguoiDung user = userService.findById(userId);
            if (user == null || !user.isDaKichHoat()) {
                return false;
            }
            
            return true;
            
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        }
    }
    
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
    }
}
```

### 3. Security Filter Integration

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenValidator jwtTokenValidator;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        
        // Extract token from Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            
            try {
                Claims claims = jwtTokenValidator.getClaimsFromToken(token);
                username = claims.getSubject();
            } catch (Exception e) {
                log.debug("Cannot extract username from JWT: {}", e.getMessage());
            }
        }
        
        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            if (jwtTokenValidator.validateToken(token)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 📡 JWT API Endpoints

### 1. Login Endpoint

```bash
POST /tai-khoan/dang-nhap
Content-Type: application/json

{
  "username": "user@email.com",
  "password": "password123"
}

# Success Response
HTTP 200 OK
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoidXNlckBlbWFpbC5jb20iLCJyb2xlcyI6WyJVU0VSIl0sInRva2VuVHlwZSI6ImFjY2VzcyIsInN1YiI6InVzZXJAZW1haWwuY29tIiwiaWF0IjoxNjQwOTk1MjAwLCJleHAiOjE2NDA5OTYxMDB9.signature",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInRva2VuVHlwZSI6InJlZnJlc2giLCJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsImlhdCI6MTY0MDk5NTIwMCwiZXhwIjoxNjQxNjAwMDAwfQ.signature",
  "id": 1,
  "email": "user@email.com", 
  "admin": false,
  "seller": false
}

# Error Response
HTTP 401 Unauthorized
{
  "error": "Invalid credentials"
}
```

### 2. Token Refresh Endpoint

```bash
POST /tai-khoan/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

# Success Response  
HTTP 200 OK
{
  "jwt": "NEW_ACCESS_TOKEN_HERE",
  "refreshToken": "NEW_REFRESH_TOKEN_HERE"
}

# Error Response
HTTP 401 Unauthorized
{
  "error": "Invalid or expired refresh token"
}
```

### 3. Token Validation Endpoint

```bash
GET /tai-khoan/validate
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

# Success Response
HTTP 200 OK
{
  "valid": true,
  "user": {
    "id": 1,
    "email": "user@email.com",
    "roles": ["USER"],
    "expiresIn": 847  // seconds remaining
  }
}

# Error Response
HTTP 401 Unauthorized
{
  "valid": false,
  "error": "Token expired"
}
```

### 4. Logout Endpoint

```bash
POST /tai-khoan/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

# Success Response
HTTP 200 OK
{
  "message": "Logged out successfully"
}
```

---

## 💻 Frontend JWT Integration

### 1. Token Storage & Management

```javascript
class JWTTokenManager {
    static ACCESS_TOKEN_KEY = 'access_token';
    static REFRESH_TOKEN_KEY = 'refresh_token';
    
    // Store tokens after login
    static storeTokens(accessToken, refreshToken) {
        localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
        localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
    
    // Get access token
    static getAccessToken() {
        return localStorage.getItem(this.ACCESS_TOKEN_KEY);
    }
    
    // Get refresh token
    static getRefreshToken() {
        return localStorage.getItem(this.REFRESH_TOKEN_KEY);
    }
    
    // Check if access token is expired
    static isTokenExpired(token) {
        if (!token) return true;
        
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expirationTime = payload.exp * 1000; // Convert to milliseconds
            return Date.now() >= expirationTime;
        } catch (e) {
            return true;
        }
    }
    
    // Clear all tokens (logout)
    static clearTokens() {
        localStorage.removeItem(this.ACCESS_TOKEN_KEY);
        localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }
    
    // Get user info from token
    static getUserFromToken(token) {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return {
                userId: payload.userId,
                email: payload.email,
                roles: payload.roles
            };
        } catch (e) {
            return null;
        }
    }
}
```

### 2. Automatic Token Refresh

```javascript
class APIClient {
    static baseURL = 'http://localhost:8080';
    static isRefreshing = false;
    static refreshPromise = null;
    
    // Main API request method with auto-refresh
    static async request(endpoint, options = {}) {
        let accessToken = JWTTokenManager.getAccessToken();
        
        // Check if token needs refresh before request
        if (!accessToken || JWTTokenManager.isTokenExpired(accessToken)) {
            accessToken = await this.refreshTokenIfNeeded();
            if (!accessToken) {
                this.redirectToLogin();
                return;
            }
        }
        
        // Add Authorization header
        const headers = {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
            ...options.headers
        };
        
        const requestOptions = {
            ...options,
            headers
        };
        
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, requestOptions);
            
            // Handle 401 - token might be expired
            if (response.status === 401) {
                // Try to refresh token once
                const newAccessToken = await this.refreshTokenIfNeeded();
                if (newAccessToken) {
                    // Retry original request with new token
                    requestOptions.headers.Authorization = `Bearer ${newAccessToken}`;
                    return await fetch(`${this.baseURL}${endpoint}`, requestOptions);
                } else {
                    this.redirectToLogin();
                    return;
                }
            }
            
            return response;
            
        } catch (error) {
            console.error('API request error:', error);
            throw error;
        }
    }
    
    // Token refresh logic
    static async refreshTokenIfNeeded() {
        if (this.isRefreshing) {
            return this.refreshPromise;
        }
        
        this.isRefreshing = true;
        
        this.refreshPromise = this.performTokenRefresh();
        
        try {
            const newAccessToken = await this.refreshPromise;
            return newAccessToken;
        } finally {
            this.isRefreshing = false;
            this.refreshPromise = null;
        }
    }
    
    static async performTokenRefresh() {
        const refreshToken = JWTTokenManager.getRefreshToken();
        
        if (!refreshToken) {
            this.redirectToLogin();
            return null;
        }
        
        try {
            const response = await fetch(`${this.baseURL}/tai-khoan/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });
            
            if (response.ok) {
                const data = await response.json();
                JWTTokenManager.storeTokens(data.jwt, data.refreshToken);
                return data.jwt;
            } else {
                // Refresh failed - redirect to login
                this.redirectToLogin();
                return null;
            }
            
        } catch (error) {
            console.error('Token refresh error:', error);
            this.redirectToLogin();
            return null;
        }
    }
    
    static redirectToLogin() {
        JWTTokenManager.clearTokens();
        window.location.href = '/login';
    }
}
```

### 3. Login Implementation

```javascript
async function loginUser(username, password) {
    try {
        const response = await fetch('/tai-khoan/dang-nhap', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        if (response.status === 200) {
            // Standard login success
            const data = await response.json();
            JWTTokenManager.storeTokens(data.jwt, data.refreshToken);
            
            // Redirect to dashboard
            window.location.href = '/dashboard';
            
        } else if (response.status === 403) {
            // 2FA required
            const mfaData = await response.json();
            if (mfaData.errorCode === 'MFA_REQUIRED') {
                showTwoFactorForm(mfaData.mfaToken);
            }
            
        } else {
            // Login failed
            const errorData = await response.json();
            showError(errorData.message || 'Login failed');
        }
        
    } catch (error) {
        console.error('Login error:', error);
        showError('Network error. Please try again.');
    }
}

// Usage examples
async function getUserProfile() {
    const response = await APIClient.request('/api/user/profile');
    if (response.ok) {
        return await response.json();
    }
}

async function updateUserData(userData) {
    const response = await APIClient.request('/api/user/update', {
        method: 'PUT',
        body: JSON.stringify(userData)
    });
    return response;
}
```

---

## 🔒 JWT Security Considerations

### 1. Token Security Best Practices

```java
@Configuration
public class JwtSecurityConfig {
    
    // Use strong secret key (at least 256 bits)
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @PostConstruct
    public void validateSecretKey() {
        if (jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 chars)");
        }
    }
    
    // Token expiration settings
    @Bean
    public JwtTokenSettings jwtTokenSettings() {
        return JwtTokenSettings.builder()
            .accessTokenExpiration(Duration.ofMinutes(15))  // Short-lived
            .refreshTokenExpiration(Duration.ofDays(7))     // Longer-lived  
            .issuer("web-ban-sach")
            .algorithm(SignatureAlgorithm.HS256)
            .build();
    }
}
```

### 2. XSS Protection

```javascript
// Secure token storage options
class SecureTokenStorage {
    
    // Option 1: HttpOnly Cookies (recommended for web)
    static storeInCookies(accessToken, refreshToken) {
        // Set httpOnly, secure, sameSite cookies
        document.cookie = `access_token=${accessToken}; HttpOnly; Secure; SameSite=Strict; Max-Age=900`;
        document.cookie = `refresh_token=${refreshToken}; HttpOnly; Secure; SameSite=Strict; Max-Age=604800`;
    }
    
    // Option 2: Encrypted localStorage
    static storeEncrypted(accessToken, refreshToken) {
        const deviceKey = this.getOrCreateDeviceKey();
        
        const encryptedAccess = CryptoJS.AES.encrypt(accessToken, deviceKey).toString();
        const encryptedRefresh = CryptoJS.AES.encrypt(refreshToken, deviceKey).toString();
        
        localStorage.setItem('enc_access_token', encryptedAccess);
        localStorage.setItem('enc_refresh_token', encryptedRefresh);
    }
    
    // Option 3: Memory storage (lost on page refresh)
    static tokenMemoryStore = {
        accessToken: null,
        refreshToken: null
    };
    
    static storeInMemory(accessToken, refreshToken) {
        this.tokenMemoryStore.accessToken = accessToken;
        this.tokenMemoryStore.refreshToken = refreshToken;
    }
}
```

### 3. CSRF Protection

```java
@Component
public class JwtCSRFProtection {
    
    // Add CSRF token to JWT claims for extra protection
    public String generateCSRFToken() {
        return UUID.randomUUID().toString();
    }
    
    public Map<String, Object> addCSRFClaim(Map<String, Object> claims, HttpServletRequest request) {
        String csrfToken = generateCSRFToken();
        claims.put("csrf", csrfToken);
        
        // Store CSRF token in session or header for comparison
        request.getSession().setAttribute("csrf_token", csrfToken);
        
        return claims;
    }
    
    public boolean validateCSRFToken(String jwtToken, HttpServletRequest request) {
        try {
            Claims claims = jwtService.getClaimsFromToken(jwtToken);
            String tokenCSRF = (String) claims.get("csrf");
            String sessionCSRF = (String) request.getSession().getAttribute("csrf_token");
            
            return tokenCSRF != null && tokenCSRF.equals(sessionCSRF);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 4. Rate Limiting

```java
@Component
public class JwtRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "jwt_rate_limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    
    public boolean isRateLimited(String clientIp) {
        String key = RATE_LIMIT_PREFIX + clientIp;
        String currentCount = (String) redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            // First request
            redisTemplate.opsForValue().set(key, "1", 60, TimeUnit.SECONDS);
            return false;
        } else {
            int count = Integer.parseInt(currentCount);
            if (count >= MAX_REQUESTS_PER_MINUTE) {
                return true; // Rate limited
            } else {
                // Increment counter
                redisTemplate.opsForValue().increment(key);
                return false;
            }
        }
    }
    
    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureEvent event) {
        // Increase rate limiting on failed attempts
        String clientIp = getClientIp(event.getRequest());
        String failKey = RATE_LIMIT_PREFIX + "fail:" + clientIp;
        redisTemplate.opsForValue().increment(failKey);
    }
}
```

---

## 🧪 JWT Testing Guide

### 1. Manual Testing

```bash
# 1. Test login and get tokens
curl -X POST http://localhost:8080/tai-khoan/dang-nhap \
  -H "Content-Type: application/json" \
  -d '{"username": "test@email.com", "password": "password"}'

# 2. Test protected endpoint with JWT
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"

# 3. Test token refresh
curl -X POST http://localhost:8080/tai-khoan/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN_HERE"}'

# 4. Test invalid token
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer invalid_token"
```

### 2. Automated Testing

```java
@SpringBootTest
@AutoConfigureTestDatabase
public class JwtAuthenticationTest {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testJwtTokenGeneration() {
        NguoiDung testUser = createTestUser();
        
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);
        
        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        
        // Validate token structure
        String[] tokenParts = accessToken.split("\\.");
        assertEquals(3, tokenParts.length); // header.payload.signature
    }
    
    @Test
    public void testTokenValidation() {
        NguoiDung testUser = createTestUser();
        String token = jwtService.generateAccessToken(testUser);
        
        boolean isValid = jwtTokenValidator.validateToken(token);
        assertTrue(isValid);
        
        Claims claims = jwtTokenValidator.getClaimsFromToken(token);
        assertEquals(testUser.getEmail(), claims.getSubject());
        assertEquals(testUser.getMaNguoiDung(), claims.get("userId"));
    }
    
    @Test
    public void testProtectedEndpoint() {
        // Login to get token
        LoginRequest loginRequest = new LoginRequest("test@email.com", "password");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
            "/tai-khoan/dang-nhap", loginRequest, AuthResponse.class);
        
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String jwt = loginResponse.getBody().getJwt();
        
        // Test protected endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/user/profile", HttpMethod.GET, entity, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    public void testTokenRefresh() {
        NguoiDung testUser = createTestUser();
        String refreshToken = jwtService.generateRefreshToken(testUser);
        
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/tai-khoan/refresh", request, AuthResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getJwt());
        assertNotNull(response.getBody().getRefreshToken());
    }
}
```

### 3. Postman Testing Collection

```json
{
  "info": {
    "name": "JWT Authentication API"
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    },
    {
      "key": "access_token",
      "value": ""
    },
    {
      "key": "refresh_token", 
      "value": ""
    }
  ],
  "item": [
    {
      "name": "1. Login",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "if (responseCode.code === 200) {",
              "  var jsonData = pm.response.json();",
              "  pm.environment.set('access_token', jsonData.jwt);",
              "  pm.environment.set('refresh_token', jsonData.refreshToken);",
              "}"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\"username\": \"test@email.com\", \"password\": \"password\"}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "{{base_url}}/tai-khoan/dang-nhap",
          "host": ["{{base_url}}"],
          "path": ["tai-khoan", "dang-nhap"]
        }
      }
    },
    {
      "name": "2. Get User Profile (Protected)",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{access_token}}"
          }
        ],
        "url": {
          "raw": "{{base_url}}/api/user/profile",
          "host": ["{{base_url}}"],
          "path": ["api", "user", "profile"]
        }
      }
    },
    {
      "name": "3. Refresh Token",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "if (responseCode.code === 200) {",
              "  var jsonData = pm.response.json();",
              "  pm.environment.set('access_token', jsonData.jwt);",
              "  pm.environment.set('refresh_token', jsonData.refreshToken);",
              "}"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\"refreshToken\": \"{{refresh_token}}\"}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "{{base_url}}/tai-khoan/refresh",
          "host": ["{{base_url}}"],
          "path": ["tai-khoan", "refresh"]
        }
      }
    }
  ]
}
```

---

## 📊 JWT Monitoring & Analytics

### 1. Token Metrics

```java
@Component
public class JwtMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordTokenGeneration(String tokenType) {
        Counter.builder("jwt.token.generated")
            .tag("type", tokenType) // "access" or "refresh"
            .register(meterRegistry)
            .increment();
    }
    
    public void recordTokenValidation(boolean success) {
        Counter.builder("jwt.token.validation")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
    
    public void recordTokenRefresh(boolean success) {
        Counter.builder("jwt.token.refresh")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
    
    public void recordTokenExpiration() {
        Counter.builder("jwt.token.expired")
            .register(meterRegistry)
            .increment();
    }
}
```

### 2. Performance Monitoring

```java
@Component
public class JwtPerformanceMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    @Async
    public void handleSuccessfulAuthentication(AuthenticationSuccessEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("jwt.authentication.duration")
            .tag("outcome", "success")
            .register(meterRegistry));
    }
    
    @EventListener  
    @Async
    public void handleFailedAuthentication(AbstractAuthenticationFailureEvent event) {
        Counter.builder("jwt.authentication.failures")
            .tag("reason", event.getException().getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
    }
}
```

---

## 🐛 Common JWT Issues & Solutions

| Issue | Cause | Solution |
|-------|--------|----------|
| `401 Unauthorized` on all requests | Missing/invalid Authorization header | Check header format: `Bearer <token>` |
| `Token expired` errors | Short token lifetime | Implement auto-refresh or extend expiration |
| `Invalid signature` | Wrong secret key or algorithm | Verify JWT secret in application.properties |
| CORS errors with JWT | Missing CORS config | Add JWT endpoints to CORS allowed headers |
| `Cannot read token` | Malformed token structure | Validate token format (3 parts separated by dots) |
| High memory usage | Storing too much in JWT claims | Reduce claim size, use references instead |

### Debugging JWT Issues

```java
@RestController
public class JwtDebugController {
    
    @GetMapping("/debug/jwt")
    public ResponseEntity<?> debugJwtToken(@RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid Authorization header format");
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Parse without verification first
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return ResponseEntity.badRequest().body("Invalid token structure");
            }
            
            // Decode header and payload
            String header = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("header", header);
            debug.put("payload", payload);
            debug.put("valid", jwtTokenValidator.validateToken(token));
            
            return ResponseEntity.ok(debug);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Token parsing error: " + e.getMessage());
        }
    }
}
```

---

## 📚 Configuration Files

### 1. Application Properties

```properties
# JWT Configuration
jwt.secret=${JWT_SECRET:mySecretKeyThatIsAtLeast256BitsLongForSecurity}
jwt.access-token-expiration=900
jwt.refresh-token-expiration=604800
jwt.issuer=web-ban-sach

# Security Configuration
spring.security.require-ssl=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict

# CORS Configuration (if needed)
cors.allowed-origins=http://localhost:3000,https://yourdomain.com
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=Authorization,Content-Type,X-Requested-With
```

### 2. Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/tai-khoan/dang-nhap", "/tai-khoan/dang-ky", "/tai-khoan/refresh").permitAll()
                .requestMatchers("/tai-khoan/verify-2fa", "/tai-khoan/oauth-login").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/sach/**", "/the-loai/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasRole("SELLER")
                .anyRequest().authenticated()
            )
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}
```

---

## ✅ Kết Luận

Hệ thống JWT Authentication đã được triển khai hoàn chỉnh với:

### 🔑 **Core Features**
- **Dual Token Strategy** - Access token (15 min) + Refresh token (7 days)
- **Automatic Refresh** - Seamless token renewal without user interruption  
- **Stateless Design** - No server-side session storage required
- **Cross-Platform** - Works with web, mobile, và microservices

### 🛡️ **Security Features**
- **HMAC SHA-256** signing algorithm
- **Token expiration** validation
- **CSRF protection** capabilities
- **Rate limiting** support
- **Secure storage** options (HttpOnly cookies, encrypted localStorage)

### 🚀 **Performance Benefits**
- **Reduced database queries** - User info stored in token
- **Scalable architecture** - No session synchronization needed
- **Efficient validation** - In-memory signature verification
- **Redis integration** - For rate limiting and blacklisting

### 🔧 **Developer Experience**
- **Comprehensive APIs** - Login, refresh, validation endpoints
- **Frontend utilities** - Token management, auto-refresh logic
- **Testing tools** - Unit tests, Postman collections
- **Monitoring** - Metrics, performance tracking, debug tools

Hệ thống JWT cung cấp **foundation mạnh mẽ** cho authentication, hỗ trợ tốt cho **scalability** và **security** trong môi trường production.