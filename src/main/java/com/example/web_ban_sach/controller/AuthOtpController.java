package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.RedisOtpService;
import com.example.web_ban_sach.Service.OtpService.OtpResult;
import com.example.web_ban_sach.Service.OtpService.OtpVerificationResult;
import com.example.web_ban_sach.Service.TaiKhoanService;
import com.example.web_ban_sach.Service.TempJwtService;
import com.example.web_ban_sach.entity.OtpVerification.OtpType;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.dto.SendOtpRequest;
import com.example.web_ban_sach.dto.VerifyOtpRequest;
import com.example.web_ban_sach.dto.FinalRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthOtpController {
    
    @Autowired
    private RedisOtpService redisOtpService;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private TaiKhoanService taiKhoanService;
    
    @Autowired
    private TempJwtService tempJwtService;
    
    /**
     * 🔹 STEP 2: Request OTP via Gmail
     * POST /api/auth/send-otp
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendRegistrationOtp(@RequestBody SendOtpRequest request) {
        try {
            String email = request.getEmail();
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email is required", null));
            }
            
            // Check if user already exists
            NguoiDung existingUser = nguoiDungRepository.findByEmail(email.trim());
            if (existingUser != null) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email already registered", null));
            }
            
            // Send OTP to email via Gmail and store in Redis
            OtpResult result = redisOtpService.sendOtp(email.trim(), OtpType.REGISTER);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(createResponse(true, "OTP sent to your email", 
                    Map.of("otpId", result.getOtpId(), "email", email.trim())));
            } else {
                return ResponseEntity.badRequest().body(createResponse(false, result.getMessage(), null));
            }
            
        } catch (Exception e) {
            System.err.println("Error in sendRegistrationOtp: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse(false, "Failed to send OTP: " + e.getMessage(), null));
        }
    }
    
    /**
     * 🔹 STEP 3: Verify OTP → Return temporary JWT token
     * POST /api/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyRegistrationOtp(@RequestBody VerifyOtpRequest request) {
        try {
            // Validate request
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email is required", null));
            }
            
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "OTP is required", null));
            }
            
            String email = request.getEmail().trim();
            
            // Verify OTP from Redis
            OtpVerificationResult otpResult = redisOtpService.verifyOtp(
                email, 
                request.getOtp().trim(), 
                OtpType.REGISTER
            );
            
            if (!otpResult.isSuccess()) {
                return ResponseEntity.badRequest().body(createResponse(false, otpResult.getMessage(), null));
            }
            
            // ✅ OTP verified → Generate temporary JWT token
            String tempToken = tempJwtService.generateTempToken(email, "registration");
            
            Map<String, Object> data = new HashMap<>();
            data.put("otpVerified", true);
            data.put("email", email);
            data.put("otpVerifiedToken", tempToken);
            data.put("tokenExpiresIn", "10 minutes");
            data.put("message", "OTP verified successfully. Use this token to complete registration.");
            
            return ResponseEntity.ok(createResponse(true, "OTP verified successfully", data));
            
        } catch (Exception e) {
            System.err.println("Error in verifyRegistrationOtp: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse(false, "Internal server error", null));
        }
    }
    
    /**
     * 🔹 STEP 4: Submit registration info with temp JWT
     * POST /api/auth/register
     * Authorization: Bearer temp_jwt
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> completeRegistration(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody FinalRegistrationRequest request) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(createResponse(false, "Authorization header required", null));
            }
            
            // Validate request
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email is required", null));
            }
            
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Password is required", null));
            }
            
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Full name is required", null));
            }
            
            String email = request.getEmail().trim();
            
            // 🔒 SECURITY CHECK: Validate temporary JWT token
            if (!tempJwtService.validateAuthHeader(authHeader, email)) {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid or expired temporary token", null));
            }
            
            // Check if user already exists
            NguoiDung existingUser = nguoiDungRepository.findByEmail(email);
            if (existingUser != null) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email already registered", null));
            }
            
            // Create NguoiDung object for registration
            NguoiDung nguoiDung = new NguoiDung();
            nguoiDung.setEmail(email);
            
            // Split fullName into hoDem and ten
            String fullName = request.getFullName().trim();
            String[] nameParts = fullName.split(" ", 2);
            if (nameParts.length >= 2) {
                nguoiDung.setHoDem(nameParts[0]);
                nguoiDung.setTen(nameParts[1]);
            } else {
                nguoiDung.setHoDem("");
                nguoiDung.setTen(fullName);
            }
            
            nguoiDung.setMatKhau(request.getPassword());
            
            // Use email as username (consistent with OAuth users)
            nguoiDung.setTenDangNhap(email);
            
            // Set phone number if provided (optional)
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
                nguoiDung.setSoDienThoai(request.getPhoneNumber().trim());
            }
            
            // Set email as verified since OTP was verified
            nguoiDung.setDaKichHoat(true);
            
            // Use existing TaiKhoanService to register user
            ResponseEntity<?> registrationResult = taiKhoanService.dangKyNguoiDung(nguoiDung);
            
            if (registrationResult.getStatusCode().is2xxSuccessful()) {
                // Registration successful
                NguoiDung savedUser = nguoiDungRepository.findByEmail(email);
                if (savedUser != null) {
                    // 🧹 Clean up OTP from Redis after successful registration
                    redisOtpService.cleanupOtp(email, OtpType.REGISTER);
                    
                    // Prepare response
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", savedUser.getMaNguoiDung());
                    data.put("email", savedUser.getEmail());
                    data.put("hoTen", (savedUser.getHoDem() + " " + savedUser.getTen()).trim());
                    data.put("tenDangNhap", savedUser.getTenDangNhap());
                    data.put("soDienThoai", savedUser.getSoDienThoai());
                    data.put("registrationStatus", "Registration completed successfully");
                    data.put("createdAt", LocalDateTime.now());
                    data.put("daKichHoat", true);
                    
                    return ResponseEntity.ok(createResponse(true, "Registration completed successfully!", data));
                } else {
                    return ResponseEntity.internalServerError()
                        .body(createResponse(false, "Registration succeeded but user not found", null));
                }
            } else {
                // Registration failed, return the error from TaiKhoanService
                return ResponseEntity.badRequest()
                    .body(createResponse(false, "Registration failed: " + registrationResult.getBody(), null));
            }
            
        } catch (Exception e) {
            System.err.println("Error in completeRegistration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse(false, "System error: " + e.getMessage(), null));
        }
    }

    
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
    
    /**
     * Test endpoint to verify OTP sending capability via Gmail
     */
    @PostMapping("/test-otp")
    public ResponseEntity<Map<String, Object>> testOtp(@RequestBody SendOtpRequest request) {
        try {
            String email = request.getEmail();
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email is required", null));
            }
            
            // Send a test OTP via Gmail and store in Redis
            OtpResult result = redisOtpService.sendOtp(email.trim(), OtpType.REGISTER);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(createResponse(true, "Test OTP sent successfully via Gmail", 
                    Map.of("otpId", result.getOtpId(), "email", email.trim())));
            } else {
                return ResponseEntity.badRequest().body(createResponse(false, result.getMessage(), null));
            }
            
        } catch (Exception e) {
            System.err.println("Error in testOtp: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get OTP statistics for debugging (Redis-based)
     */
    @PostMapping("/otp-stats")
    public ResponseEntity<Map<String, Object>> getOtpStats(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createResponse(false, "Email is required", null));
            }
            
            Map<String, Object> stats = redisOtpService.getOtpStats(email.trim(), OtpType.REGISTER);
            
            return ResponseEntity.ok(createResponse(true, "OTP statistics retrieved from Redis", stats));
            
        } catch (Exception e) {
            System.err.println("Error getting OTP stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(createResponse(false, "Error: " + e.getMessage(), null));
        }
    }
}