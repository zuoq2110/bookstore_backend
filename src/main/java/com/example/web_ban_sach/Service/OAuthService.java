package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dto.GoogleUserInfo;
import com.example.web_ban_sach.dto.OAuthLoginRequest;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Luôn kiểm tra email trước, nếu trùng thì liên kết OAuth với tài khoản hiện có
     */
    @Transactional
    private NguoiDung findOrCreateUser(
            String email, 
            String displayName, 
            String provider,
            String providerId,
            String avatarUrl) {
        
        // Tìm user theo email (ưu tiên cao nhất)
        NguoiDung existingUser = nguoiDungRepository.findByEmail(email);

        if (existingUser != null) {
            // User với email này đã tồn tại
            log.info("Found existing user with email: {}", email);
            
            // Kiểm tra xem user đã có OAuth provider này chưa
            if (existingUser.getOauthProvider() == null) {
                // User chưa có OAuth provider -> Liên kết OAuth với tài khoản hiện có
                log.info("Linking {} OAuth to existing account: {}", provider, email);
                existingUser.setOauthProvider(provider);
                existingUser.setOauthProviderId(providerId);
                
                // Cập nhật avatar nếu user chưa có hoặc avatar mới tốt hơn
                if ((existingUser.getAvatar() == null || existingUser.getAvatar().isEmpty()) 
                    && avatarUrl != null && !avatarUrl.isEmpty()) {
                    existingUser.setAvatar(avatarUrl);
                    log.info("Updated avatar from {} for user: {}", provider, email);
                }
                
            } else if (!provider.equals(existingUser.getOauthProvider())) {
                // User đã có OAuth provider khác
                log.warn("User {} already has {} OAuth, attempted to link with {}", 
                        email, existingUser.getOauthProvider(), provider);
                
                // Có thể thêm logic để hỗ trợ multiple OAuth providers trong tương lai
                // Hiện tại chỉ cho phép 1 OAuth provider per user
                
                // Vẫn cho phép đăng nhập nhưng không thay đổi OAuth info
                log.info("Allowing login with existing OAuth provider for: {}", email);
                
            } else {
                // Cùng OAuth provider -> Cập nhật thông tin
                log.info("Refreshing {} OAuth info for user: {}", provider, email);
                existingUser.setOauthProviderId(providerId); // Update provider ID nếu thay đổi
                
                // Update avatar
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    existingUser.setAvatar(avatarUrl);
                }
            }
            
            return nguoiDungRepository.save(existingUser);
            
        } else {
            // Email chưa tồn tại -> Tạo user mới với OAuth
            log.info("Creating new OAuth user with email: {}", email);
            
            NguoiDung newUser = new NguoiDung();
            newUser.setEmail(email);
            newUser.setTen(displayName != null ? displayName : email.split("@")[0]);
            newUser.setTenDangNhap(email); // Sử dụng email làm username cho OAuth users
            newUser.setOauthProvider(provider);
            newUser.setOauthProviderId(providerId);
            newUser.setAvatar(avatarUrl);
            
            // Generate random password (user won't use it)
            String randomPassword = UUID.randomUUID().toString();
            newUser.setMatKhau(passwordEncoder.encode(randomPassword));
            
            // Set default values
            newUser.setDaKichHoat(true);  // Activated (Google đã verify email)
            newUser.setSeller(false);
            
            log.info("Created new OAuth user: {}", email);
            return nguoiDungRepository.save(newUser);
        }
    }
}
