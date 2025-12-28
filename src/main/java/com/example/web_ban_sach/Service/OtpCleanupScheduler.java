package com.example.web_ban_sach.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 🔐 2.6 Cleanup OTP - Scheduled task to clean expired OTPs
 */
@Component
public class OtpCleanupScheduler {
    
    @Autowired
    private OtpService otpService;
    
    /**
     * Run every 30 minutes to cleanup expired and used OTPs
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutes
    public void cleanupExpiredOtps() {
        try {
            otpService.cleanupExpiredOtps();
        } catch (Exception e) {
            System.err.println("Error in OTP cleanup scheduler: " + e.getMessage());
        }
    }
}