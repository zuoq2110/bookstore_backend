package com.example.web_ban_sach.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class MessageEncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    
    @Value("${app.chat.encryption.key:#{null}}")
    private String encryptionKeyBase64;
    
    private SecretKey getEncryptionKey() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isEmpty()) {
            throw new RuntimeException("Encryption key is not configured. Please set app.chat.encryption.key in application.properties");
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyBase64);
            return new SecretKeySpec(decodedKey, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode encryption key", e);
        }
    }
    
    /**
     * Encrypts a message using AES-GCM
     * @param plainText The message to encrypt
     * @return Base64 encoded string containing IV + encrypted data
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            SecretKey key = getEncryptionKey();
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            
            // Encrypt the data
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            // Return as Base64
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message", e);
        }
    }
    
    /**
     * Decrypts a message using AES-GCM
     * @param encryptedText Base64 encoded string containing IV + encrypted data
     * @return Decrypted message
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            SecretKey key = getEncryptionKey();
            
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            
            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }
    
    /**
     * Generates a new AES-256 key and returns it as Base64
     * This method is for key generation only - call it once and store the result
     */
    public static String generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256); // AES-256
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}