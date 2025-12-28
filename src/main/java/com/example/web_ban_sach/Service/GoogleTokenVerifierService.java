package com.example.web_ban_sach.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.example.web_ban_sach.dto.GoogleUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class GoogleTokenVerifierService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.allowed-client-ids}")
    private String allowedClientIdsStr;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        List<String> allowedClientIds = Arrays.asList(allowedClientIdsStr.split(","));
        
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(allowedClientIds)
                .build();
        
        log.info("Google Token Verifier initialized with client IDs: {}", allowedClientIds);
    }

    /**
     * Verify Google ID Token và trả về user info
     */
    public GoogleUserInfo verifyToken(String idTokenString) throws Exception {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken == null) {
                log.error("Invalid Google ID token");
                throw new IllegalArgumentException("Invalid ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify email
            if (!payload.getEmailVerified()) {
                log.error("Email not verified for user: {}", payload.getEmail());
                throw new IllegalArgumentException("Email not verified");
            }

            // Convert to GoogleUserInfo
            GoogleUserInfo userInfo = new GoogleUserInfo();
            userInfo.setSub(payload.getSubject());
            userInfo.setEmail(payload.getEmail());
            userInfo.setEmailVerified(payload.getEmailVerified());
            userInfo.setName((String) payload.get("name"));
            userInfo.setPicture((String) payload.get("picture"));
            userInfo.setGivenName((String) payload.get("given_name"));
            userInfo.setFamilyName((String) payload.get("family_name"));
            userInfo.setLocale((String) payload.get("locale"));

            log.info("Successfully verified Google token for user: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            throw new Exception("Failed to verify Google ID token", e);
        }
    }
}
