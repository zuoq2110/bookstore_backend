package com.example.web_ban_sach.dto;

import lombok.Data;

@Data
public class GoogleUserInfo {
    private String sub;           // Google user ID
    private String email;
    private boolean emailVerified;
    private String name;
    private String picture;
    private String givenName;
    private String familyName;
    private String locale;
}
