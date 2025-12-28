package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.Service.OtpService.OtpResult;
import com.example.web_ban_sach.Service.OtpService.OtpVerificationResult;
import com.example.web_ban_sach.entity.OtpVerification;
import com.example.web_ban_sach.entity.OtpVerification.OtpType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisOtpService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // OTP expires in 5 minutes
    private static final long OTP_EXPIRATION_MINUTES = 5;
    
    // Rate limiting: max 3 OTP requests per email per 10 minutes
    private static final long RATE_LIMIT_WINDOW_MINUTES = 10;
    private static final int MAX_OTP_REQUESTS = 3;
    
    /**
     * Send OTP and store in Redis with TTL
     */
    public OtpResult sendOtp(String email, OtpType otpType) {
        try {
            // Check rate limiting with safe casting
            String rateLimitKey = "otp_rate_limit:" + email;
            Object requestCountObj = redisTemplate.opsForValue().get(rateLimitKey);
            
            // 🔒 Safe casting: Redis might store as Integer or Long
            Long requestCount = null;
            if (requestCountObj != null) {
                if (requestCountObj instanceof Integer) {
                    requestCount = ((Integer) requestCountObj).longValue();
                } else if (requestCountObj instanceof Long) {
                    requestCount = (Long) requestCountObj;
                } else {
                    // Fallback: parse as number
                    requestCount = Long.valueOf(requestCountObj.toString());
                }
            }
            
            if (requestCount != null && requestCount >= MAX_OTP_REQUESTS) {
                return new OtpResult(false, "Too many OTP requests. Please wait " + RATE_LIMIT_WINDOW_MINUTES + " minutes.", null);
            }
            
            // Generate OTP
            String otp = generateOtp();
            Integer otpId = generateOtpId(); // Generate Integer ID instead of UUID
            
            // Create OTP data
            Map<String, Object> otpData = new HashMap<>();
            otpData.put("otpId", otpId); // Integer ID
            otpData.put("email", email);
            otpData.put("otp", otp);
            otpData.put("otpType", otpType.name());
            otpData.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            otpData.put("verified", false);
            otpData.put("attempts", 0);
            
            // Store in Redis with TTL
            String otpKey = "otp:" + email + ":" + otpType.name();
            String otpDataJson = objectMapper.writeValueAsString(otpData);
            
            redisTemplate.opsForValue().set(otpKey, otpDataJson, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
            
            // Update rate limiting
            if (requestCount == null) {
                redisTemplate.opsForValue().set(rateLimitKey, 1L, RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().increment(rateLimitKey);
            }
            
            // Send email
            boolean emailSent = sendOtpEmail(email, otp, otpType);
            
            if (emailSent) {
                System.out.println("✅ OTP sent successfully to " + email + " and stored in Redis with " + OTP_EXPIRATION_MINUTES + " min TTL");
                return new OtpResult(true, "OTP sent successfully", otpId);
            } else {
                // Remove from Redis if email failed
                redisTemplate.delete(otpKey);
                return new OtpResult(false, "Failed to send OTP email", null);
            }
            
        } catch (Exception e) {
            System.err.println("Error sending OTP: " + e.getMessage());
            e.printStackTrace();
            return new OtpResult(false, "System error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Verify OTP from Redis
     */
    public OtpVerificationResult verifyOtp(String email, String inputOtp, OtpType otpType) {
        try {
            String otpKey = "otp:" + email + ":" + otpType.name();
            String otpDataJson = (String) redisTemplate.opsForValue().get(otpKey);
            
            if (otpDataJson == null) {
                return new OtpVerificationResult(false, "OTP expired or not found", null);
            }
            
            Map<String, Object> otpData = objectMapper.readValue(otpDataJson, Map.class);
            
            // Check if already verified
            boolean verified = Boolean.parseBoolean(String.valueOf(otpData.get("verified")));
            if (verified) {
                return new OtpVerificationResult(false, "OTP already used", null);
            }
            
            // Check attempts - handle both String and Number from JSON
            int attempts = 0;
            Object attemptsObj = otpData.get("attempts");
            if (attemptsObj != null) {
                if (attemptsObj instanceof Number) {
                    attempts = ((Number) attemptsObj).intValue();
                } else {
                    attempts = Integer.parseInt(String.valueOf(attemptsObj));
                }
            }
            if (attempts >= 3) {
                redisTemplate.delete(otpKey); // Remove after too many attempts
                return new OtpVerificationResult(false, "Too many incorrect attempts. OTP invalidated.", null);
            }
            
            String storedOtp = (String) otpData.get("otp");
            
            if (!inputOtp.equals(storedOtp)) {
                // Increment attempts
                otpData.put("attempts", attempts + 1);
                String updatedJson = objectMapper.writeValueAsString(otpData);
                
                // Preserve existing TTL
                Long remainingTTL = redisTemplate.getExpire(otpKey);
                if (remainingTTL != null && remainingTTL > 0) {
                    redisTemplate.opsForValue().set(otpKey, updatedJson, remainingTTL, TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().set(otpKey, updatedJson, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
                }
                
                return new OtpVerificationResult(false, "Invalid OTP. " + (2 - attempts) + " attempts remaining.", null);
            }
            
            // OTP is correct - mark as verified
            otpData.put("verified", true);
            otpData.put("verifiedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            String verifiedJson = objectMapper.writeValueAsString(otpData);
            redisTemplate.opsForValue().set(otpKey, verifiedJson, 30, TimeUnit.MINUTES); // Keep for 30 min after verification
            
            // Create OtpVerification entity for compatibility
            OtpVerification otpRecord = createOtpVerificationFromData(otpData, email);
            
            System.out.println("✅ OTP verified successfully for " + email + " (type: " + otpType + ")");
            return new OtpVerificationResult(true, "OTP verified successfully", otpRecord);
            
        } catch (Exception e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
            e.printStackTrace();
            return new OtpVerificationResult(false, "System error during verification", null);
        }
    }
    
    /**
     * Check if OTP is verified (for security checks)
     */
    public boolean isOtpVerified(String email, OtpType otpType) {
        try {
            String otpKey = "otp:" + email + ":" + otpType.name();
            String otpDataJson = (String) redisTemplate.opsForValue().get(otpKey);
            
            if (otpDataJson == null) {
                return false;
            }
            
            Map<String, Object> otpData = objectMapper.readValue(otpDataJson, Map.class);
            Object verifiedObj = otpData.get("verified");
            return verifiedObj != null && Boolean.parseBoolean(String.valueOf(verifiedObj));
            
        } catch (Exception e) {
            System.err.println("Error checking OTP verification status: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clean up OTP after successful registration
     */
    public void cleanupOtp(String email, OtpType otpType) {
        try {
            String otpKey = "otp:" + email + ":" + otpType.name();
            redisTemplate.delete(otpKey);
            System.out.println("🧹 Cleaned up OTP for " + email + " (type: " + otpType + ")");
        } catch (Exception e) {
            System.err.println("Error cleaning up OTP: " + e.getMessage());
        }
    }
    
    /**
     * Get OTP statistics for monitoring
     */
    public Map<String, Object> getOtpStats(String email, OtpType otpType) {
        try {
            String otpKey = "otp:" + email + ":" + otpType.name();
            String rateLimitKey = "otp_rate_limit:" + email;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("otpExists", redisTemplate.hasKey(otpKey));
            stats.put("otpTTL", redisTemplate.getExpire(otpKey));
            stats.put("requestCount", redisTemplate.opsForValue().get(rateLimitKey));
            stats.put("rateLimitTTL", redisTemplate.getExpire(rateLimitKey));
            
            return stats;
        } catch (Exception e) {
            System.err.println("Error getting OTP stats: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    private Integer generateOtpId() {
        Random random = new Random();
        return 100000 + random.nextInt(900000); // 6-digit Integer ID
    }
    
    private boolean sendOtpEmail(String email, String otp, OtpType otpType) {
        try {
            String subject = getEmailSubject(otpType);
            String body = getEmailBody(otp, otpType);
            
            // EmailService.sendMessage requires 4 params: from, to, subject, text
            emailService.sendMessage("noreply@webbansach.com", email, subject, body);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            return false;
        }
    }
    
    private String getEmailSubject(OtpType otpType) {
        switch (otpType) {
            case REGISTER:
                return "📧 Your Registration OTP - Web Ban Sach";
            case RESET_PASSWORD:
                return "🔐 Your Password Reset OTP - Web Ban Sach";
            default:
                return "🔑 Your OTP Code - Web Ban Sach";
        }
    }
    
    private String getEmailBody(String otp, OtpType otpType) {
        String purpose = otpType == OtpType.REGISTER ? "complete your registration" : "reset your password";
        
        return String.format(
            """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
                <h2 style="color: #333; text-align: center;">🔐 OTP Verification</h2>
                
                <p>Hello,</p>
                
                <p>Your OTP code to <strong>%s</strong> is:</p>
                
                <div style="text-align: center; margin: 30px 0;">
                    <span style="font-size: 32px; font-weight: bold; color: #007bff; background: #f8f9fa; padding: 15px 30px; border-radius: 8px; letter-spacing: 5px;">%s</span>
                </div>
                
                <p><strong>⏰ This OTP will expire in %d minutes.</strong></p>
                
                <p style="color: #666; font-size: 14px;">
                    🔒 For your security, please do not share this OTP with anyone.<br>
                    💡 If you didn't request this OTP, please ignore this email.
                </p>
                
                <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                
                <p style="color: #888; font-size: 12px; text-align: center;">
                    This is an automated email from Web Ban Sach. Please do not reply to this email.
                </p>
            </div>
            """,
            purpose, otp, OTP_EXPIRATION_MINUTES
        );
    }
    
    /**
     * Create OtpVerification entity from Redis data for compatibility
     */
    private OtpVerification createOtpVerificationFromData(Map<String, Object> otpData, String email) {
        OtpVerification verification = new OtpVerification();
        
        // Set basic fields
        verification.setIdentifier(email);
        verification.setOtpType(OtpType.valueOf((String) otpData.get("otpType")));
        
        // Parse timestamps
        try {
            String createdAtStr = (String) otpData.get("createdAt");
            verification.setCreatedAt(LocalDateTime.parse(createdAtStr));
            
            String verifiedAtStr = (String) otpData.get("verifiedAt");
            if (verifiedAtStr != null) {
                verification.setVerifiedAt(LocalDateTime.parse(verifiedAtStr));
            }
        } catch (Exception e) {
            verification.setCreatedAt(LocalDateTime.now());
            verification.setVerifiedAt(LocalDateTime.now());
        }
        
        // Set attempts
        Object attemptsObj = otpData.get("attempts");
        if (attemptsObj instanceof Number) {
            verification.setAttempts(((Number) attemptsObj).intValue());
        } else {
            verification.setAttempts(Integer.parseInt(String.valueOf(attemptsObj)));
        }
        
        // Set as used/verified
        Object verifiedObj = otpData.get("verified");
        boolean isVerified = verifiedObj != null && Boolean.parseBoolean(String.valueOf(verifiedObj));
        verification.setIsUsed(isVerified);
        
        // Set expiration (5 minutes from creation)
        verification.setExpiredAt(verification.getCreatedAt().plusMinutes(OTP_EXPIRATION_MINUTES));
        
        // Set dummy hash (not used in Redis flow)
        verification.setOtpHash("redis_otp_hash");
        
        return verification;
    }
}