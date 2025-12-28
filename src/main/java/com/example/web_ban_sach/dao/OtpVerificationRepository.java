package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.OtpVerification;
import com.example.web_ban_sach.entity.OtpVerification.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Integer> {
    
    /**
     * Find valid OTP by identifier and type (not used, not expired)
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.identifier = :identifier " +
           "AND o.otpType = :type AND o.isUsed = false AND o.expiredAt > :now " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findValidOtp(@Param("identifier") String identifier, 
                                          @Param("type") OtpType type, 
                                          @Param("now") LocalDateTime now);
    
    /**
     * Count OTP requests in last N minutes for rate limiting
     */
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.identifier = :identifier " +
           "AND o.otpType = :type AND o.createdAt > :since")
    long countRecentOtpRequests(@Param("identifier") String identifier, 
                               @Param("type") OtpType type, 
                               @Param("since") LocalDateTime since);
    
    /**
     * Find all expired OTPs for cleanup
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.expiredAt < :now OR o.isUsed = true")
    List<OtpVerification> findExpiredOrUsedOtps(@Param("now") LocalDateTime now);
    
    /**
     * Delete expired and used OTPs
     */
    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiredAt < :now OR o.isUsed = true")
    int deleteExpiredAndUsedOtps(@Param("now") LocalDateTime now);
    
    /**
     * Find all OTPs by identifier for cleanup when user is verified
     */
    List<OtpVerification> findByIdentifierAndOtpType(String identifier, OtpType otpType);
    
    /**
     * Mark all OTPs as used for an identifier and type
     */
    @Modifying
    @Query("UPDATE OtpVerification o SET o.isUsed = true WHERE o.identifier = :identifier " +
           "AND o.otpType = :type AND o.isUsed = false")
    int markAllOtpsAsUsed(@Param("identifier") String identifier, @Param("type") OtpType type);
    
    /**
     * Check if there's a recent verified OTP for security validation
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
           "FROM OtpVerification o WHERE o.identifier = :identifier " +
           "AND o.otpType = :type AND o.verifiedAt IS NOT NULL " +
           "AND o.verifiedAt > :since")
    boolean existsByIdentifierAndOtpTypeAndVerifiedAtIsNotNullAndVerifiedAtAfter(
        @Param("identifier") String identifier, 
        @Param("type") OtpType type, 
        @Param("since") LocalDateTime since);
}