package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.JWTService;
import com.example.web_ban_sach.Service.TaiKhoanService;
import com.example.web_ban_sach.Service.UserService;
import com.example.web_ban_sach.Service.OAuthService;
import com.example.web_ban_sach.Service.TwoFactorAuthService;
import com.example.web_ban_sach.Service.GoogleTokenVerifierService;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dto.AuthResponse;
import com.example.web_ban_sach.dto.OAuthLoginRequest;
import com.example.web_ban_sach.dto.UserDTO;
import com.example.web_ban_sach.dto.GoogleUserInfo;
import com.example.web_ban_sach.dto.MfaRequiredResponse;
import com.example.web_ban_sach.dto.TwoFactorVerificationRequest;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.ThongBao;
import com.example.web_ban_sach.security.JWTResponse;
import com.example.web_ban_sach.security.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/tai-khoan")
@CrossOrigin(origins = "*") // Allow requests from 'http://localhost:3000'
public class TaiKhoanController {
    @Autowired
    private TaiKhoanService taiKhoanService;

    @Autowired
    private JWTService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private OAuthService oauthService;
    @Autowired
    private TwoFactorAuthService twoFactorAuthService;
    @Autowired
    private GoogleTokenVerifierService googleTokenVerifier;


    @PostMapping("/dang-ky")
    public ResponseEntity<?> dangKyNguoiDung(@Validated @RequestBody NguoiDung nguoiDung){
        ResponseEntity<?> response = taiKhoanService.dangKyNguoiDung(nguoiDung);
        return response;

    }
    @GetMapping("/kich-hoat")
    public ResponseEntity<?> kichHoatTaiKhoan(@RequestParam String email, @RequestParam String maKichHoat){
        ResponseEntity<?> response = taiKhoanService.kichHoatTaiKhoan(email, maKichHoat);
        return response;

    }

    @PostMapping("/dang-nhap")
    public ResponseEntity<?> dangNhap(@RequestBody LoginRequest loginRequest){
       try {
           Authentication authentication = authenticationManager.authenticate(
                   new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
           );
           if(authentication.isAuthenticated()){
               // Lấy thông tin user từ database
               NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(loginRequest.getUsername());
               if (nguoiDung == null) {
                   nguoiDung = nguoiDungRepository.findByEmail(loginRequest.getUsername());
               }
               
               if (nguoiDung != null) {
                   // Kiểm tra nếu user có bật 2FA
                   if (nguoiDung.isMfaEnabled()) {
                       // Không cấp JWT ngay, trả về mfa_token tạm thời
                       String mfaToken = twoFactorAuthService.generateMfaToken(nguoiDung.getMaNguoiDung());
                       
                       MfaRequiredResponse mfaResponse = new MfaRequiredResponse(
                           false,
                           "Two-factor authentication required",
                           mfaToken,
                           "MFA_REQUIRED",
                           300 // 5 phút
                       );
                       
                       return ResponseEntity.status(403).body(mfaResponse);
                   }
                   
                   // Nếu không có 2FA, cấp JWT bình thường
                   final String jwt = jwtService.generateToken(loginRequest.getUsername());
                   final String refreshToken = jwtService.generateRefreshToken(loginRequest.getUsername());
                   
                   // Tạo response với đầy đủ thông tin
                   JWTResponse response = new JWTResponse();
                   response.setJwt(jwt);
                   response.setRefreshToken(refreshToken);
                   response.setId(nguoiDung.getMaNguoiDung());
                   response.setEmail(nguoiDung.getEmail());
                   response.setAdmin(nguoiDung.getDanhSachQuyen() != null && 
                           nguoiDung.getDanhSachQuyen().stream()
                           .anyMatch(q -> q.getTenQuyen().equals("ADMIN")));
                   response.setSeller(nguoiDung.isSeller());
                   response.setTenGianHang(nguoiDung.getTenGianHang());
                   
                   return ResponseEntity.ok(response);
               }
               
               final String jwt = jwtService.generateToken(loginRequest.getUsername());
               final String refreshToken = jwtService.generateRefreshToken(loginRequest.getUsername());
               return ResponseEntity.ok(new JWTResponse(jwt, refreshToken));
           }
       }catch (AuthenticationException e){
           return ResponseEntity.badRequest().body(new ThongBao("Tên đăng nhập hoặc mật khẩu không đúng"));
       }

        return ResponseEntity.badRequest().body(new ThongBao("Tên đăng nhập hoặc mật khẩu không đúng"));
    }
    
    /**
     * 🔹 Xác thực mã 2FA sau khi đăng nhập
     * POST /tai-khoan/verify-2fa
     */
    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody TwoFactorVerificationRequest request) {
        try {
            // Validate input
            if (request.getMfaToken() == null || request.getVerificationCode() == null) {
                return ResponseEntity.badRequest().body(new ThongBao("MFA token và mã xác thực không được để trống"));
            }
            
            if (request.getVerificationCode().length() < 6) {
                return ResponseEntity.badRequest().body(new ThongBao("Mã xác thực không hợp lệ"));
            }
            
            // Validate MFA token và lấy userId
            Optional<Integer> userIdOpt = twoFactorAuthService.validateMfaToken(request.getMfaToken());
            if (!userIdOpt.isPresent()) {
                return ResponseEntity.badRequest().body(new ThongBao("MFA token không hợp lệ hoặc đã hết hạn"));
            }
            
            // Lấy thông tin user
            NguoiDung nguoiDung = nguoiDungRepository.findById(userIdOpt.get()).orElse(null);
            if (nguoiDung == null) {
                return ResponseEntity.badRequest().body(new ThongBao("User không tìm thấy"));
            }
            
            // Xác thực mã 2FA
            boolean isValid = twoFactorAuthService.verifyTwoFactorCode(nguoiDung, request.getVerificationCode());
            if (!isValid) {
                return ResponseEntity.badRequest().body(new ThongBao("Mã xác thực không đúng"));
            }
            
            // Cấp JWT chính thức
            String username = nguoiDung.getEmail() != null ? nguoiDung.getEmail() : nguoiDung.getTenDangNhap();
            final String jwt = jwtService.generateToken(username);
            final String refreshToken = jwtService.generateRefreshToken(username);
            
            // Tạo response với đầy đủ thông tin
            JWTResponse response = new JWTResponse();
            response.setJwt(jwt);
            response.setRefreshToken(refreshToken);
            response.setId(nguoiDung.getMaNguoiDung());
            response.setEmail(nguoiDung.getEmail());
            response.setAdmin(nguoiDung.getDanhSachQuyen() != null && 
                    nguoiDung.getDanhSachQuyen().stream()
                    .anyMatch(q -> q.getTenQuyen().equals("ADMIN")));
            response.setSeller(nguoiDung.isSeller());
            response.setTenGianHang(nguoiDung.getTenGianHang());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ThongBao("Lỗi xác thực 2FA"));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request){
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Refresh token không được để trống"));
            }
            
            // Validate refresh token
            String username = jwtService.extractUsername(refreshToken);
            
            if (username == null) {
                return ResponseEntity.badRequest().body(new ThongBao("Refresh token không hợp lệ"));
            }
            
            // Check if token is refresh token and not expired
            if (!jwtService.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest().body(new ThongBao("Token không phải là refresh token"));
            }
            
            // Generate new access token
            String newAccessToken = jwtService.generateToken(username);
            
            // Generate new refresh token (Refresh Token Rotation - BẢO MẬT HƠN)
            String newRefreshToken = jwtService.generateRefreshToken(username);
            
            // Get user info
            NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(username);
            if (nguoiDung == null) {
                nguoiDung = nguoiDungRepository.findByEmail(username);
            }
            
            if (nguoiDung != null) {
                JWTResponse response = new JWTResponse();
                response.setJwt(newAccessToken);
                response.setRefreshToken(newRefreshToken);
                response.setId(nguoiDung.getMaNguoiDung());
                response.setEmail(nguoiDung.getEmail());
                response.setAdmin(nguoiDung.getDanhSachQuyen() != null && 
                        nguoiDung.getDanhSachQuyen().stream()
                        .anyMatch(q -> q.getTenQuyen().equals("ADMIN")));
                response.setSeller(nguoiDung.isSeller());
                response.setTenGianHang(nguoiDung.getTenGianHang());
                
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(new JWTResponse(newAccessToken, newRefreshToken));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ThongBao("Refresh token không hợp lệ hoặc đã hết hạn"));
        }
    }

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
            String jwt = jwtService.generateToken(user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getEmail());

            // 3. Convert to DTO
            UserDTO userDTO = convertToDTO(user);

            // 4. Return response
            AuthResponse response = new AuthResponse();
            response.setJwt(jwt);
            response.setRefreshToken(refreshToken);
            response.setUser(userDTO);
            
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
        dto.setHoDem(user.getHoDem());
        dto.setTenDangNhap(user.getTenDangNhap());
        dto.setSoDienThoai(user.getSoDienThoai());
        dto.setDiaChiGiaoHang(user.getDiaChiGiaoHang());
        dto.setDiaChiMuaHang(user.getDiaChiMuaHang());
        dto.setNgaySinh(user.getNgaySinh());
        dto.setGioiTinh(user.getGioiTinh());
        dto.setAnhDaiDien(user.getAvatar());
        dto.setTenGianHang(user.getTenGianHang());
        dto.setMoTaGianHang(user.getMoTaGianHang());
        dto.setDiaChiGianHang(user.getDiaChiGianHang());
        dto.setSoDienThoaiGianHang(user.getSoDienThoaiGianHang());
        
        // Set isAdmin
        boolean isAdmin = user.getDanhSachQuyen() != null && 
                user.getDanhSachQuyen().stream()
                .anyMatch(q -> q.getTenQuyen().equals("ADMIN"));
        dto.setIsAdmin(isAdmin);
        dto.setIsSeller(user.isSeller());
        
        return dto;
    }

    /**
     * Endpoint để link OAuth provider với tài khoản hiện có
     * POST /tai-khoan/link-oauth
     */
    @PostMapping("/link-oauth")
    public ResponseEntity<?> linkOAuthProvider(@RequestBody Map<String, Object> request) {
        try {
            String provider = (String) request.get("provider");
            String idToken = (String) request.get("idToken");
            String email = (String) request.get("email");
            String currentUserEmail = (String) request.get("currentUserEmail");
            String password = (String) request.get("password");

            if (provider == null || idToken == null || email == null || currentUserEmail == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields",
                    "message", "provider, idToken, email, currentUserEmail, and password are required"
                ));
            }

            // 1. Verify current user's password
            NguoiDung currentUser = nguoiDungRepository.findByEmail(currentUserEmail);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found",
                    "message", "Current user not found"
                ));
            }

            // Verify password (simplified - in real app use authentication)
            // if (!passwordEncoder.matches(password, currentUser.getMatKhau())) {
            //     return ResponseEntity.badRequest().body(Map.of(
            //         "error", "Invalid password",
            //         "message", "Current password is incorrect"
            //     ));
            // }

            // 2. Verify OAuth token
            GoogleUserInfo googleUser = null;
            if ("google".equalsIgnoreCase(provider)) {
                googleUser = googleTokenVerifier.verifyToken(idToken);
                if (!email.equals(googleUser.getEmail())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Email mismatch",
                        "message", "OAuth email does not match provided email"
                    ));
                }
            }

            // 3. Check if OAuth email matches current user or different user
            if (!email.equals(currentUserEmail)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email mismatch", 
                    "message", "OAuth email must match current user's email for account linking"
                ));
            }

            // 4. Link OAuth provider
            if (currentUser.getOauthProvider() == null) {
                currentUser.setOauthProvider(provider);
                currentUser.setOauthProviderId(googleUser != null ? googleUser.getSub() : "unknown");
                nguoiDungRepository.save(currentUser);

                return ResponseEntity.ok(Map.of(
                    "message", "OAuth provider linked successfully",
                    "provider", provider,
                    "email", email
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "OAuth already linked",
                    "message", "Account already has an OAuth provider: " + currentUser.getOauthProvider()
                ));
            }

        } catch (Exception e) {
            log.error("OAuth linking failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "OAuth linking failed",
                "message", e.getMessage()
            ));
        }
    }

}
