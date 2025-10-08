package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface SellerService {
    
    // Đăng ký seller
    ResponseEntity<?> registerSeller(SellerRegisterRequest request);
    
    // Dashboard
    ResponseEntity<?> getDashboard(int sellerId);
    
    // Quản lý sách
    ResponseEntity<?> getBooks(int sellerId, int page, int limit, String status);
    ResponseEntity<?> addBook(int sellerId, AddBookWithImageRequest request);
    ResponseEntity<?> addBookWithFile(int sellerId, AddBookRequest bookData, MultipartFile coverImage);
    ResponseEntity<?> deleteBook(int sellerId, int bookId);
    
    // Thống kê
    ResponseEntity<?> getStatistics(int sellerId, String period);
}
