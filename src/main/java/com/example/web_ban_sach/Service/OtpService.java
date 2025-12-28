package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.OtpVerificationRepository;
import com.example.web_ban_sach.entity.OtpVerification;
import com.example.web_ban_sach.entity.OtpVerification.OtpType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class OtpService {
    
    @Autowired
    private OtpVerificationRepository otpRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;
    
    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${app.otp.rate-limit-per-hour:5}")
    private int rateLimitPerHour;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 🔐 2.1 Generate OTP - Random 6 digits, unpredictable using SecureRandom
     */
    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
    
    /**
     * 🔐 2.2 Hash OTP securely - Never store plain OTP
     */
    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }
    
    /**
     * 🔐 2.3 Send OTP - Generate, save hash, send via email
     */
    @Transactional
    public OtpResult sendOtp(String identifier, OtpType otpType) {
        try {
            // 🔐 2.5 Rate limiting check
            if (!checkRateLimit(identifier, otpType)) {
                return new OtpResult(false, "Rate limit exceeded. Too many OTP requests.", null);
            }
            
            // Invalidate any existing OTPs for this identifier and type
            otpRepository.markAllOtpsAsUsed(identifier, otpType);
            
            // Generate new OTP
            String otp = generateOtp();
            String otpHash = hashOtp(otp);
            
            // Create OTP record with hash (not plain OTP)
            LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
            OtpVerification otpRecord = new OtpVerification(identifier, otpHash, otpType, expiredAt);
            otpRepository.save(otpRecord);
            
            // Send OTP via email (don't log the actual OTP)
            boolean sent = sendOtpEmail(identifier, otp, otpType);
            
            if (sent) {
                // Log success without OTP value
                System.out.println("OTP sent successfully to: " + identifier + " for " + otpType);
                return new OtpResult(true, "OTP sent successfully", otpRecord.getId());
            } else {
                return new OtpResult(false, "Failed to send OTP", null);
            }
            
        } catch (Exception e) {
            System.err.println("Error sending OTP: " + e.getMessage());
            return new OtpResult(false, "Internal error occurred", null);
        }
    }
    
    /**
     * 🔐 2.4 Verify OTP with security checks
     */
    @Transactional
    public OtpVerificationResult verifyOtp(String identifier, String inputOtp, OtpType otpType) {
        try {
            // Find valid OTP record
            Optional<OtpVerification> otpRecordOpt = otpRepository.findValidOtp(
                identifier, otpType, LocalDateTime.now()
            );
            
            if (otpRecordOpt.isEmpty()) {
                return new OtpVerificationResult(false, "OTP not found or expired", null);
            }
            
            OtpVerification otpRecord = otpRecordOpt.get();
            
            // Check if too many attempts
            if (otpRecord.getAttempts() >= maxAttempts) {
                return new OtpVerificationResult(false, "Too many failed attempts", null);
            }
            
            // Hash input OTP and compare
            String inputOtpHash = hashOtp(inputOtp);
            
            if (inputOtpHash.equals(otpRecord.getOtpHash())) {
                // ✅ OTP verified successfully
                otpRecord.setVerifiedAt(LocalDateTime.now());
                otpRecord.setIsUsed(true);
                otpRepository.save(otpRecord);
                
                // 🔐 2.6 Cleanup - mark all other OTPs as used
                otpRepository.markAllOtpsAsUsed(identifier, otpType);
                
                return new OtpVerificationResult(true, "OTP verified successfully", otpRecord);
            } else {
                // ❌ Wrong OTP
                otpRecord.incrementAttempts();
                otpRepository.save(otpRecord);
                
                int remainingAttempts = maxAttempts - otpRecord.getAttempts();
                if (remainingAttempts <= 0) {
                    return new OtpVerificationResult(false, "OTP verification failed. No attempts remaining.", null);
                } else {
                    return new OtpVerificationResult(false, 
                        "Invalid OTP. " + remainingAttempts + " attempts remaining.", null);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
            return new OtpVerificationResult(false, "Internal error occurred", null);
        }
    }
    
    /**
     * 🔐 2.5 Rate limiting - prevent abuse
     */
    private boolean checkRateLimit(String identifier, OtpType otpType) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentRequests = otpRepository.countRecentOtpRequests(identifier, otpType, oneHourAgo);
        return recentRequests < rateLimitPerHour;
    }
    
    /**
     * 🔐 2.3 Send OTP via email (don't log OTP value)
     */
    private boolean sendOtpEmail(String email, String otp, OtpType otpType) {
        try {
            String subject = getEmailSubject(otpType);
            String content = getEmailContent(otp, otpType);
            
            System.out.println("Attempting to send OTP email to: " + email);
            emailService.sendMessage("noreply@webbansach.com", email, subject, content);
            System.out.println("OTP email sent successfully to: " + email);
            return true;
        } catch (RuntimeException e) {
            System.err.println("Failed to send OTP email to " + email + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error sending OTP email to " + email + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private String getEmailSubject(OtpType otpType) {
        return switch (otpType) {
            case REGISTER -> "Mã xác thực đăng ký tài khoản";
            case RESET_PASSWORD -> "Mã xác thực đặt lại mật khẩu";
            case CHANGE_EMAIL -> "Mã xác thực thay đổi email";
            case LOGIN_VERIFICATION -> "Mã xác thực đăng nhập";
        };
    }
    
    private String getEmailContent(String otp, OtpType otpType) {
        String purpose = switch (otpType) {
            case REGISTER -> "hoàn tất đăng ký tài khoản";
            case RESET_PASSWORD -> "đặt lại mật khẩu";
            case CHANGE_EMAIL -> "thay đổi email";
            case LOGIN_VERIFICATION -> "xác thực đăng nhập";
        };
        
        return String.format("""
            <h3>Mã xác thực OTP</h3>
            <p>Mã xác thực của bạn để %s là:</p>
            <h2 style="color: #007bff; font-family: monospace;">%s</h2>
            <p><strong>Lưu ý:</strong></p>
            <ul>
                <li>Mã có hiệu lực trong %d phút</li>
                <li>Không chia sẻ mã này với ai khác</li>
                <li>Nếu bạn không yêu cầu mã này, hãy bỏ qua email này</li>
            </ul>
            <hr>
            <p><small>Email tự động từ hệ thống Web Bán Sách</small></p>
            """, purpose, otp, otpExpiryMinutes);
    }
    
    /**
     * 🔐 2.6 Cleanup expired OTPs - called by scheduled task
     */
    @Transactional
    public int cleanupExpiredOtps() {
        try {
            int deletedCount = otpRepository.deleteExpiredAndUsedOtps(LocalDateTime.now());
            if (deletedCount > 0) {
                System.out.println("Cleaned up " + deletedCount + " expired/used OTPs");
            }
            return deletedCount;
        } catch (Exception e) {
            System.err.println("Error cleaning up OTPs: " + e.getMessage());
            return 0;
        }
    }
    
    // Result classes
    public static class OtpResult {
        private final boolean success;
        private final String message;
        private final Integer otpId;
        
        public OtpResult(boolean success, String message, Integer otpId) {
            this.success = success;
            this.message = message;
            this.otpId = otpId;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Integer getOtpId() { return otpId; }
    }
    
    public static class OtpVerificationResult {
        private final boolean success;
        private final String message;
        private final OtpVerification otpRecord;
        
        public OtpVerificationResult(boolean success, String message, OtpVerification otpRecord) {
            this.success = success;
            this.message = message;
            this.otpRecord = otpRecord;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public OtpVerification getOtpRecord() { return otpRecord; }
    }
}