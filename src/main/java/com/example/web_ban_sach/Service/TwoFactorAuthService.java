package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.util.MessageEncryptionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TwoFactorAuthService {
    
    @Autowired
    private GoogleAuthenticator googleAuthenticator;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private MessageEncryptionUtil messageEncryptionUtil;
    
    private static final String APP_NAME = "WebBanSach";
    private static final String MFA_TOKEN_PREFIX = "mfa_token:";
    private static final long MFA_TOKEN_EXPIRATION_MINUTES = 5;
    
    /**
     * Giai đoạn 1: Thiết lập 2FA - Tạo Secret Key và QR Code URL
     */
    public String generateQrCodeUrl(NguoiDung nguoiDung) {
        // Tạo secret key mới
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secretKey = key.getKey();
        
        // Mã hóa secret key trước khi lưu vào database
        String encryptedSecret = messageEncryptionUtil.encrypt(secretKey);
        
        // Lưu encrypted secret key vào database với mfa_enabled = false
        nguoiDung.setMfaSecret(encryptedSecret);
        nguoiDung.setMfaEnabled(false); // Chưa kích hoạt chính thức
        nguoiDungRepository.save(nguoiDung);
        
        // Tạo QR Code URL với secret key gốc (chưa mã hóa)
        String qrCodeUrl = generateTotpUrl(nguoiDung.getEmail(), secretKey);
        
        return qrCodeUrl;
    }
    
    /**
     * Giai đoạn 1: Xác nhận lần đầu - Kích hoạt 2FA
     * Trả về backup codes gốc để người dùng lưu lại
     */
    public List<String> confirmTwoFactorSetup(NguoiDung nguoiDung, String verificationCode) {
        if (nguoiDung.getMfaSecret() == null) {
            return null;
        }
        
        try {
            // Giải mã secret key để verify
            String decryptedSecret = messageEncryptionUtil.decrypt(nguoiDung.getMfaSecret());
            
            // Verify mã 6 số
            int code = Integer.parseInt(verificationCode.trim());
            boolean isValidCode = googleAuthenticator.authorize(decryptedSecret, code);
            
            if (isValidCode) {
                // Kích hoạt 2FA chính thức
                nguoiDung.setMfaEnabled(true);
                
                // Tạo backup codes và hash chúng
                List<String> backupCodes = generateBackupCodes();
                List<String> hashedBackupCodes = new ArrayList<>();
                for (String backupCode : backupCodes) {
                    hashedBackupCodes.add(passwordEncoder.encode(backupCode));
                }
                
                try {
                    nguoiDung.setBackupCodes(objectMapper.writeValueAsString(hashedBackupCodes));
                } catch (Exception e) {
                    throw new RuntimeException("Error saving backup codes", e);
                }
                
                nguoiDungRepository.save(nguoiDung);
                // Trả về backup codes gốc để hiển thị cho user
                return backupCodes;
            }
        } catch (NumberFormatException e) {
            return null; // Invalid number format
        }
        
        return null;
    }
    
    /**
     * Giai đoạn 2: Xác thực mã 2FA khi đăng nhập
     */
    public boolean verifyTwoFactorCode(NguoiDung nguoiDung, String verificationCode) {
        if (!nguoiDung.isMfaEnabled() || nguoiDung.getMfaSecret() == null) {
            return false;
        }
        
        try {
            // Giải mã secret key để verify
            String decryptedSecret = messageEncryptionUtil.decrypt(nguoiDung.getMfaSecret());
            
            // Kiểm tra mã từ Authenticator app
            int code = Integer.parseInt(verificationCode.trim());
            boolean isValidCode = googleAuthenticator.authorize(decryptedSecret, code);
            
            if (isValidCode) {
                return true;
            }
        } catch (NumberFormatException e) {
            // If not a number, try as backup code
        }
        
        // Kiểm tra backup codes nếu mã Authenticator không đúng
        return verifyBackupCode(nguoiDung, verificationCode);
    }
    
    /**
     * Tạo và lưu MFA token tạm thời
     */
    public String generateMfaToken(int userId) {
        String mfaToken = generateRandomToken();
        String key = MFA_TOKEN_PREFIX + mfaToken;
        
        // Lưu userId vào Redis với TTL 5 phút
        redisTemplate.opsForValue().set(key, userId, MFA_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        
        return mfaToken;
    }
    
    /**
     * Xác thực MFA token và lấy userId
     */
    public Optional<Integer> validateMfaToken(String mfaToken) {
        String key = MFA_TOKEN_PREFIX + mfaToken;
        Object userIdObj = redisTemplate.opsForValue().get(key);
        
        if (userIdObj != null) {
            // Xóa token sau khi sử dụng
            redisTemplate.delete(key);
            return Optional.of((Integer) userIdObj);
        }
        
        return Optional.empty();
    }
    
    /**
     * Tắt 2FA
     */
    public boolean disableTwoFactor(NguoiDung nguoiDung, String verificationCode) {
        if (!verifyTwoFactorCode(nguoiDung, verificationCode)) {
            return false;
        }
        
        nguoiDung.setMfaEnabled(false);
        nguoiDung.setMfaSecret(null);
        nguoiDung.setBackupCodes(null);
        nguoiDungRepository.save(nguoiDung);
        
        return true;
    }
    
    /**
     * Tạo backup codes mới
     */
    public List<String> regenerateBackupCodes(NguoiDung nguoiDung) {
        List<String> backupCodes = generateBackupCodes();
        List<String> hashedBackupCodes = new ArrayList<>();
        
        // Hash tất cả backup codes trước khi lưu
        for (String code : backupCodes) {
            hashedBackupCodes.add(passwordEncoder.encode(code));
        }
        
        try {
            nguoiDung.setBackupCodes(objectMapper.writeValueAsString(hashedBackupCodes));
            nguoiDungRepository.save(nguoiDung);
            // Trả về codes gốc (chưa hash) để hiển thị cho user
            return backupCodes;
        } catch (Exception e) {
            throw new RuntimeException("Error regenerating backup codes", e);
        }
    }
    
    // === PRIVATE METHODS ===
    
    /**
     * Tạo TOTP URL theo format chuẩn
     */
    private String generateTotpUrl(String email, String secretKey) {
        try {
            String encodedEmail = URLEncoder.encode(email, "UTF-8");
            String encodedAppName = URLEncoder.encode(APP_NAME, "UTF-8");
            
            return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodedAppName,
                encodedEmail,
                secretKey,
                encodedAppName
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding TOTP URL", e);
        }
    }
    
    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 8; i++) { // Tạo 8 backup codes
            String code = String.format("%08d", random.nextInt(100000000));
            codes.add(code);
        }
        
        return codes;
    }
    
    private boolean verifyBackupCode(NguoiDung nguoiDung, String inputCode) {
        if (nguoiDung.getBackupCodes() == null) {
            return false;
        }
        
        try {
            List<String> hashedBackupCodes = objectMapper.readValue(
                nguoiDung.getBackupCodes(), 
                new TypeReference<List<String>>() {}
            );
            
            // Duyệt qua từng hash để tìm mã khớp
            for (int i = 0; i < hashedBackupCodes.size(); i++) {
                String hashedCode = hashedBackupCodes.get(i);
                if (passwordEncoder.matches(inputCode.trim(), hashedCode)) {
                    // Xóa backup code đã sử dụng (dùng 1 lần duy nhất)
                    hashedBackupCodes.remove(i);
                    nguoiDung.setBackupCodes(objectMapper.writeValueAsString(hashedBackupCodes));
                    nguoiDungRepository.save(nguoiDung);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b & 0xff));
        }
        
        return token.toString();
    }
}