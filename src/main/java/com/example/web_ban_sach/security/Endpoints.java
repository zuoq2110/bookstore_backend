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
            "/the-loai/**",
            // Seller Approval & Notifications
            "/seller-requests/**",
            "/notifications/stream/**",
            "/notifications/**"
    };
    public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/tai-khoan/dang-ky",
            "/tai-khoan/dang-nhap",
            "/gio-hang/them",
            "/don-hang/**",
            "/sach-yeu-thich/**",
            "/su-danh-gia/**",
            "sach/them-sach/**",
            "/nguoi-dung/register-seller",
            // Seller Approval
            "/seller-requests/submit",
            "/seller-requests/*/approve",
            "/seller-requests/*/reject"

    };
    public static final String[] PUBLIC_PUT_ENDPOINTS = {
            "/gio-hang/**",
            "nguoi-dung/**",
            "don-hang/**",
            "/su-danh-gia/**",
            // Notifications
            "/notifications/*/mark-read",
            "/notifications/mark-all-read"

    };
    public static final String[] PUBLIC_DELETE_ENDPOINTS = {
            "/gio-hang/**",
            "/sach-yeu-thich/**",


    };
    
    // Seller endpoints - Yêu cầu isSeller = true
    public static final String[] SELLER_GET_ENDPOINTS = {
            "/seller/dashboard/**",
            "/seller/books/**",
            "/seller/books",
            "/seller/statistics/**",
            "/seller/statistics"
    };
    
    public static final String[] SELLER_POST_ENDPOINTS = {
            "/seller/books/**",
            "/seller/books"
    };
    
    public static final String[] SELLER_PUT_ENDPOINTS = {
            "/seller/books/**"
    };
    
    public static final String[] SELLER_DELETE_ENDPOINTS = {
            "/seller/books/**"
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
