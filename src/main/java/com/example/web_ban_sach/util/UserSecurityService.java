package com.example.web_ban_sach.util;

import com.example.web_ban_sach.entity.NguoiDung;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserSecurityService extends UserDetailsService {
    NguoiDung findByTenDangNhap(String tenDangNhap);
}
