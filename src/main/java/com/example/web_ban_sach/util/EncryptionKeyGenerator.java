package com.example.web_ban_sach.util;

/**
 * Utility class to generate encryption key for chat messages
 * This class should only be used once to generate the key, then the key should be stored securely
 */
public class EncryptionKeyGenerator {
    
    public static void main(String[] args) {
        // Generate a new AES-256 key
        String encryptionKey = MessageEncryptionUtil.generateNewKey();
        
        System.out.println("Generated AES-256 encryption key:");
        System.out.println(encryptionKey);
        System.out.println();
        System.out.println("Add this to your application.properties:");
        System.out.println("app.chat.encryption.key=" + encryptionKey);
        System.out.println();
        System.out.println("IMPORTANT: Store this key securely and do not share it!");
        System.out.println("If you lose this key, all encrypted messages will be unrecoverable.");
    }
}