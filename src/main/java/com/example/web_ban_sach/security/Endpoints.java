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
            "sach-yeu-thich/**",
            "/sach-yeu-thich/lay-sach/**",
            "/chi-tiet-don-hang/**",
            "/quyen/**",
            "/the-loai",
            "/the-loai/**"
    };
    public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/tai-khoan/dang-ky",
            "/tai-khoan/dang-nhap",
            "/gio-hang/them",
            "/don-hang/**",
            "/sach-yeu-thich/**",
            "/su-danh-gia/**",
            "sach/them-sach/**",

    };
    public static final String[] PUBLIC_PUT_ENDPOINTS = {
            "/gio-hang/**",
            "nguoi-dung/**",
            "don-hang/**",
            "/su-danh-gia/**",

    };
    public static final String[] PUBLIC_DELETE_ENDPOINTS = {
            "/gio-hang/**",
            "/sach-yeu-thich/**",


    };

    public static final String[] ADMIN_GET_ENDPOINTS = {
            "/nguoi-dung",
            "/nguoi-dung/**",
            "/the-loai/**",
            "/**",
    };
    public static final String[] ADMIN_POST_ENDPOINTS = {
            "/sach",
            "/sach/**",
            "/sach/them-sach/**",
            "/**",
            "/the-loai",
    };
    public static final String[] ADMIN_PUT_ENDPOINTS = {
            "/sach/cap-nhat/**",
            "/**",
    };
    public static final String[] ADMIN_DELETE_ENDPOINTS = {
            "/sach/**",
            "/**",
    };
}
