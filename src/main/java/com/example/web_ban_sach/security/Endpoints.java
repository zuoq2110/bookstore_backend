package com.example.web_ban_sach.security;

public class Endpoints {
    public static final String front_end_host = "http://localhost:3000";
    public static final String[] PUBLIC_GET_ENDPOINTS = {
            "/sach",
            "sach/**",
            "the-loai",
            "/hinh-anh",
            "/sach/**",
            "/hinh-anh/**",
            "/nguoi-dung/search/existsByTenDangNhap",
            "/nguoi-dung/search/existsByEmail",
            "/tai-khoan/kich-hoat",
            "/nguoi-dung/**",
            "/gio-hang/**",
            "/su-danh-gia/**",
            "/don-hang/**",
    };
    public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/tai-khoan/dang-ky",
            "/tai-khoan/dang-nhap",
            "/gio-hang/them",
            "/don-hang/**",

    };
    public static final String[] PUBLIC_PUT_ENDPOINTS = {
            "/gio-hang/**",
            "nguoi-dung/**"

    };
    public static final String[] PUBLIC_DELETE_ENDPOINTS = {
            "/gio-hang/**"

    };

    public static final String[] ADMIN_GET_ENDPOINTS = {
            "/nguoi-dung",
            "/nguoi-dung/**",
    };
    public static final String[] ADMIN_POST_ENDPOINTS = {
            "/sach",
            "/sach/**"
    };
}
