package com.example.web_ban_sach.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verification")
public class OtpVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String identifier; // email or phone
    
    @Column(nullable = false)
    private String otpHash; // hashed OTP, not plain text
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpType otpType;
    
    @Column(nullable = false)
    private LocalDateTime expiredAt;
    
    @Column(nullable = false)
    private Integer attempts = 0;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime verifiedAt;
    
    @Column(nullable = false)
    private Boolean isUsed = false;
    
    // Constructors
    public OtpVerification() {}
    
    public OtpVerification(String identifier, String otpHash, OtpType otpType, LocalDateTime expiredAt) {
        this.identifier = identifier;
        this.otpHash = otpHash;
        this.otpType = otpType;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    public String getOtpHash() {
        return otpHash;
    }
    
    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }
    
    public OtpType getOtpType() {
        return otpType;
    }
    
    public void setOtpType(OtpType otpType) {
        this.otpType = otpType;
    }
    
    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
    
    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
    
    public Integer getAttempts() {
        return attempts;
    }
    
    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    public Boolean getIsUsed() {
        return isUsed;
    }
    
    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }
    
    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
    
    public void incrementAttempts() {
        this.attempts++;
    }
    
    public enum OtpType {
        REGISTER,
        RESET_PASSWORD,
        CHANGE_EMAIL,
        LOGIN_VERIFICATION
    }
}