package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SellerService;
import com.example.web_ban_sach.entity.AddBookRequest;
import com.example.web_ban_sach.entity.AddBookWithImageRequest;
import com.example.web_ban_sach.entity.SellerRegisterRequest;
import com.example.web_ban_sach.entity.ThongBao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/seller")
public class SellerController {
    
    @Autowired
    private SellerService sellerService;
    
    /**
     * Dashboard - Tổng quan thống kê
     * GET /seller/dashboard/{sellerId}
     */
    @GetMapping("/dashboard/{sellerId}")
    public ResponseEntity<?> getDashboard(@PathVariable int sellerId) {
        try {
            return sellerService.getDashboard(sellerId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Lấy danh sách sách của seller
     * GET /seller/books?page=1&limit=20&status=active
     */
    @GetMapping("/books")
    public ResponseEntity<?> getBooks(
            @RequestParam(required = true) int sellerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        try {
            return sellerService.getBooks(sellerId, page, limit, status);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Thêm sách mới với cover image (JSON + base64)
     * POST /seller/books?sellerId=123
     * Content-Type: application/json
     * Body: {
     *   "bookData": {...},
     *   "coverImage": "base64_string_or_empty"
     * }
     */
    @PostMapping(value = "/books", consumes = "application/json")
    public ResponseEntity<?> addBook(
            @RequestParam(required = true) int sellerId,
            @RequestBody AddBookWithImageRequest request) {
        try {
            return sellerService.addBook(sellerId, request);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Thêm sách mới với cover image (Multipart/form-data)
     * POST /seller/books?sellerId=123
     * Content-Type: multipart/form-data
     * Form fields:
     *   - bookData: JSON string
     *   - coverImage: File (optional)
     */
    @PostMapping(value = "/books", consumes = "multipart/form-data")
    public ResponseEntity<?> addBookMultipart(
            @RequestParam(required = true) int sellerId,
            @RequestPart("bookData") String bookDataJson,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) {
        try {
            // Parse JSON string to AddBookRequest
            ObjectMapper objectMapper = new ObjectMapper();
            AddBookRequest bookData = objectMapper.readValue(bookDataJson, AddBookRequest.class);
            
            return sellerService.addBookWithFile(sellerId, bookData, coverImage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Xóa sách (soft delete)
     * DELETE /seller/books/{bookId}?sellerId=123
     */
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<?> deleteBook(
            @PathVariable int bookId,
            @RequestParam(required = true) int sellerId) {
        try {
            return sellerService.deleteBook(sellerId, bookId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Lấy thống kê theo period
     * GET /seller/statistics?sellerId=123&period=7days
     * period: 7days, 30days, 3months, 1year
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(
            @RequestParam(required = true) int sellerId,
            @RequestParam(defaultValue = "30days") String period) {
        try {
            return sellerService.getStatistics(sellerId, period);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
}
