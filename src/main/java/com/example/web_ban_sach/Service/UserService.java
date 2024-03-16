package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.entity.NguoiDung;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    public NguoiDung findByUsername(String tenDangNhap);

    public ResponseEntity<?> updateProfile(JsonNode jsonNode);

    public ResponseEntity<?> changeAvatar(JsonNode jsonNode);
}
