package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.TwoFactorAuthService;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dto.TwoFactorSetupRequest;
import com.example.web_ban_sach.dto.TwoFactorSetupResponse;
import com.example.web_ban_sach.dto.TwoFactorVerificationRequest;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.Service.JWTService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/2fa")
@CrossOrigin("*")
public class TwoFactorAuthController {
    
    @Autowired
    private TwoFactorAuthService twoFactorAuthService;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private JWTService jwtService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 🔹 STEP 1: Thiết lập 2FA - Tạo QR Code
     * POST /api/2fa/setup
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setupTwoFactor() {
        try {
            // Lấy user từ JWT token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            NguoiDung user = nguoiDungRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(createResponse(false, "User not found", null));
            }
            
            // Kiểm tra nếu 2FA đã được kích hoạt
            if (user.isMfaEnabled()) {
                return ResponseEntity.badRequest().body(createResponse(false, "2FA is already enabled", null));
            }
            
            // Tạo QR Code URL
            String qrCodeUrl = twoFactorAuthService.generateQrCodeUrl(user);
            
            TwoFactorSetupResponse response = new TwoFactorSetupResponse(
                true,
                "Scan QR code with your authenticator app",
                qrCodeUrl,
                user.getMfaSecret(), // Secret key để backup
                null // Backup codes sẽ được cấp sau khi xác nhận
            );
            
            return ResponseEntity.ok(createResponse(true, "QR code generated", response));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(createResponse(false, "Error setting up 2FA", null));
        }
    }
    
    /**
     * 🔹 STEP 2: Xác nhận thiết lập 2FA - Kích hoạt chính thức
     * POST /api/2fa/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmTwoFactorSetup(@RequestBody TwoFactorSetupRequest request) {
        try {
            // Validate input
            if (request.getVerificationCode() == null || request.getVerificationCode().length() != 6) {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid verification code", null));
            }
            
            // Lấy user từ JWT token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            NguoiDung user = nguoiDungRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(createResponse(false, "User not found", null));
            }
            
            // Xác nhận mã và kích hoạt 2FA
            List<String> backupCodes = twoFactorAuthService.confirmTwoFactorSetup(user, request.getVerificationCode());
            
            if (backupCodes != null) {
                TwoFactorSetupResponse response = new TwoFactorSetupResponse(
                    true,
                    "2FA enabled successfully",
                    null,
                    null,
                    backupCodes
                );
                
                return ResponseEntity.ok(createResponse(true, "2FA enabled successfully", response));
            } else {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid verification code", null));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(createResponse(false, "Error confirming 2FA setup", null));
        }
    }
    
    /**
     * 🔹 Tắt 2FA
     * POST /api/2fa/disable
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableTwoFactor(@RequestBody TwoFactorSetupRequest request) {
        try {
            // Validate input
            if (request.getVerificationCode() == null || request.getVerificationCode().length() < 6) {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid verification code", null));
            }
            
            // Lấy user từ JWT token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            NguoiDung user = nguoiDungRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(createResponse(false, "User not found", null));
            }
            
            if (!user.isMfaEnabled()) {
                return ResponseEntity.badRequest().body(createResponse(false, "2FA is not enabled", null));
            }
            
            // Tắt 2FA
            boolean success = twoFactorAuthService.disableTwoFactor(user, request.getVerificationCode());
            
            if (success) {
                return ResponseEntity.ok(createResponse(true, "2FA disabled successfully", null));
            } else {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid verification code", null));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(createResponse(false, "Error disabling 2FA", null));
        }
    }
    
    /**
     * 🔹 Tạo backup codes mới
     * POST /api/2fa/regenerate-backup-codes
     */
    @PostMapping("/regenerate-backup-codes")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(@RequestBody TwoFactorSetupRequest request) {
        try {
            // Validate input
            if (request.getVerificationCode() == null || request.getVerificationCode().length() < 6) {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid verification code", null));
            }
            
            // Lấy user từ JWT token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            NguoiDung user = nguoiDungRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(createResponse(false, "User not found", null));
            }
            
            if (!user.isMfaEnabled()) {
                return ResponseEntity.badRequest().body(createResponse(false, "2FA is not enabled", null));
            }
            
            // Xác thực mã trước khi tạo backup codes mới
            if (!twoFactorAuthService.verifyTwoFactorCode(user, request.getVerificationCode())) {
                return ResponseEntity.badRequest().body(createResponse(false, "Invalid verification code", null));
            }
            
            // Tạo backup codes mới
            List<String> newBackupCodes = twoFactorAuthService.regenerateBackupCodes(user);
            
            return ResponseEntity.ok(createResponse(true, "New backup codes generated", newBackupCodes));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(createResponse(false, "Error regenerating backup codes", null));
        }
    }
    
    /**
     * 🔹 Kiểm tra trạng thái 2FA
     * GET /api/2fa/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTwoFactorStatus() {
        try {
            // Lấy user từ JWT token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            NguoiDung user = nguoiDungRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(createResponse(false, "User not found", null));
            }
            
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("mfaEnabled", user.isMfaEnabled());
            statusData.put("hasBackupCodes", user.getBackupCodes() != null && !user.getBackupCodes().isEmpty());
            
            return ResponseEntity.ok(createResponse(true, "2FA status retrieved", statusData));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(createResponse(false, "Error getting 2FA status", null));
        }
    }
    
    // === UTILITY METHODS ===
    
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}