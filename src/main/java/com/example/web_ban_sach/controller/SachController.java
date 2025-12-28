package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SachService;
import com.example.web_ban_sach.Service.SachCacheService;
import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.dao.SellerBooksRepository;
import com.example.web_ban_sach.entity.Sach;
import com.example.web_ban_sach.entity.SellerBooks;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/sach")
public class SachController {
    @Autowired
    private SachService sachService;
    
    @Autowired
    private SachRepository sachRepository;
    
    @Autowired
    private SellerBooksRepository sellerBooksRepository;
    
    @Autowired
    private SachCacheService sachCacheService;
    
    @PostMapping("/them-sach")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
       return sachService.save(jsonNode);
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> update(@RequestBody JsonNode jsonNode){
        return sachService.update(jsonNode);
    }
    
    /**
     * Override endpoint GET /sach/{id} để thêm thông tin seller
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSachById(@PathVariable int id) {
        Optional<Sach> sachOpt = sachRepository.findById(id);
        
        if (!sachOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Sach sach = sachOpt.get();
        
        // Tìm seller book (nếu có)
        Optional<SellerBooks> sellerBooksOpt = sellerBooksRepository.findFirstBySachMaSachAndActive(id);
        
        // Tạo response với thông tin seller
        Map<String, Object> response = new HashMap<>();
        response.put("maSach", sach.getMaSach());
        response.put("tenSach", sach.getTenSach());
        response.put("tenTacGia", sach.getTenTacGia());
        response.put("isbn", sach.getISBN());
        response.put("moTa", sach.getMoTa());
        response.put("giaNiemYet", sach.getGiaNiemYet());
        response.put("giaBan", sach.getGiaBan());
        response.put("soLuong", sach.getSoLuong());
        response.put("trungBinhXepHang", sach.getTrungBinhXepHang());
        response.put("soLuongDaBan", sach.getSoLuongDaBan());
        response.put("giamGia", sach.getGiamGia());
        
        // Lấy ảnh bìa
        String coverImageUrl = null;
        if (sach.getDanhSachHinhAnh() != null && !sach.getDanhSachHinhAnh().isEmpty()) {
            coverImageUrl = sach.getDanhSachHinhAnh().stream()
                    .filter(ha -> ha.isLaIcon())
                    .findFirst()
                    .map(ha -> ha.getDuongDan())
                    .orElse(null);
        }
        response.put("coverImageUrl", coverImageUrl);
        
        // Thêm thông tin seller nếu tồn tại
        if (sellerBooksOpt.isPresent()) {
            SellerBooks sellerBooks = sellerBooksOpt.get();
            response.put("sellerId", sellerBooks.getSeller().getMaNguoiDung());
            response.put("sellerName", sellerBooks.getSeller().getTenGianHang() != null 
                ? sellerBooks.getSeller().getTenGianHang()
                : sellerBooks.getSeller().getHoDem() + " " + sellerBooks.getSeller().getTen());
        } else {
            response.put("sellerId", null);
            response.put("sellerName", null);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Custom endpoint GET /sach/list để thêm thông tin seller cho danh sách
     * Thay thế cho /sach?sort=...&size=...
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAllSach(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "maSach") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            // Tạo Pageable
            org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();
            
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, sort);
            
            // Lấy danh sách sách từ cache (List, không phải Page)
            List<Sach> sachList = sachCacheService.findAllWithCache(pageable);
            long totalElements = sachCacheService.count();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // Convert sang Map và thêm thông tin seller
            java.util.List<Map<String, Object>> sachListWithSeller = sachList.stream()
                .map(sach -> {
                    Map<String, Object> sachMap = new HashMap<>();
                    sachMap.put("maSach", sach.getMaSach());
                    sachMap.put("tenSach", sach.getTenSach());
                    sachMap.put("tenTacGia", sach.getTenTacGia());
                    sachMap.put("isbn", sach.getISBN());
                    sachMap.put("moTa", sach.getMoTa());
                    sachMap.put("giaNiemYet", sach.getGiaNiemYet());
                    sachMap.put("giaBan", sach.getGiaBan());
                    sachMap.put("soLuong", sach.getSoLuong());
                    sachMap.put("trungBinhXepHang", sach.getTrungBinhXepHang());
                    sachMap.put("soLuongDaBan", sach.getSoLuongDaBan());
                    sachMap.put("giamGia", sach.getGiamGia());
                    
                    // Lấy ảnh bìa (cover image)
                    String coverImageUrl = null;
                    if (sach.getDanhSachHinhAnh() != null && !sach.getDanhSachHinhAnh().isEmpty()) {
                        coverImageUrl = sach.getDanhSachHinhAnh().stream()
                                .filter(ha -> ha.isLaIcon())
                                .findFirst()
                                .map(ha -> ha.getDuongDan())
                                .orElse(null);
                    }
                    sachMap.put("coverImageUrl", coverImageUrl);
                    
                    // Tìm seller
                    Optional<SellerBooks> sellerBooksOpt = 
                        sellerBooksRepository.findFirstBySachMaSachAndActive(sach.getMaSach());
                    
                    if (sellerBooksOpt.isPresent()) {
                        SellerBooks sellerBooks = sellerBooksOpt.get();
                        sachMap.put("sellerId", sellerBooks.getSeller().getMaNguoiDung());
                        sachMap.put("sellerName", sellerBooks.getSeller().getTenGianHang() != null 
                            ? sellerBooks.getSeller().getTenGianHang()
                            : sellerBooks.getSeller().getHoDem() + " " + sellerBooks.getSeller().getTen());
                    } else {
                        sachMap.put("sellerId", null);
                        sachMap.put("sellerName", null);
                    }
                    
                    return sachMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            // Tạo response pagination
            Map<String, Object> response = new HashMap<>();
            response.put("content", sachListWithSeller);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint để test performance - Query TRỰC TIẾP DATABASE (KHÔNG dùng cache)
     * Dùng để so sánh với /sach/list (có cache)
     * URL: GET /sach/list-no-cache?page=0&size=6&sortBy=soLuongDaBan&sortDir=desc
     */
    @GetMapping("/list-no-cache")
    public ResponseEntity<?> getAllSachNoCache(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "maSach") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Tạo Pageable
            org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();
            
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, sort);
            
            // Query TRỰC TIẾP database (không cache)
            List<Sach> sachList = sachCacheService.findAllWithoutCache(pageable);
            long totalElements = sachCacheService.count();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            long queryTime = System.currentTimeMillis() - startTime;
            
            // Convert sang Map và thêm thông tin seller
            java.util.List<Map<String, Object>> sachListWithSeller = sachList.stream()
                .map(sach -> {
                    Map<String, Object> sachMap = new HashMap<>();
                    sachMap.put("maSach", sach.getMaSach());
                    sachMap.put("tenSach", sach.getTenSach());
                    sachMap.put("tenTacGia", sach.getTenTacGia());
                    sachMap.put("isbn", sach.getISBN());
                    sachMap.put("moTa", sach.getMoTa());
                    sachMap.put("giaNiemYet", sach.getGiaNiemYet());
                    sachMap.put("giaBan", sach.getGiaBan());
                    sachMap.put("soLuong", sach.getSoLuong());
                    sachMap.put("trungBinhXepHang", sach.getTrungBinhXepHang());
                    sachMap.put("soLuongDaBan", sach.getSoLuongDaBan());
                    sachMap.put("giamGia", sach.getGiamGia());
                    
                    // Lấy ảnh bìa (cover image)
                    String coverImageUrl = null;
                    if (sach.getDanhSachHinhAnh() != null && !sach.getDanhSachHinhAnh().isEmpty()) {
                        coverImageUrl = sach.getDanhSachHinhAnh().stream()
                                .filter(ha -> ha.isLaIcon())
                                .findFirst()
                                .map(ha -> ha.getDuongDan())
                                .orElse(null);
                    }
                    sachMap.put("coverImageUrl", coverImageUrl);
                    
                    // Tìm seller
                    Optional<SellerBooks> sellerBooksOpt = 
                        sellerBooksRepository.findFirstBySachMaSachAndActive(sach.getMaSach());
                    
                    if (sellerBooksOpt.isPresent()) {
                        SellerBooks sellerBooks = sellerBooksOpt.get();
                        sachMap.put("sellerId", sellerBooks.getSeller().getMaNguoiDung());
                        sachMap.put("sellerName", sellerBooks.getSeller().getTenGianHang() != null 
                            ? sellerBooks.getSeller().getTenGianHang()
                            : sellerBooks.getSeller().getHoDem() + " " + sellerBooks.getSeller().getTen());
                    } else {
                        sachMap.put("sellerId", null);
                        sachMap.put("sellerName", null);
                    }
                    
                    return sachMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            // Tạo response pagination với thông tin performance
            Map<String, Object> response = new HashMap<>();
            response.put("content", sachListWithSeller);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("cached", false);
            response.put("queryTimeMs", queryTime);
            response.put("totalTimeMs", totalTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * API lấy sách theo thể loại với phân trang và cache
     * URL: GET /sach/the-loai/{maTheLoai}?page=0&size=20&sortBy=maSach&sortDir=desc
     */
    @GetMapping("/the-loai/{maTheLoai}")
    public ResponseEntity<?> getSachByTheLoai(
            @PathVariable int maTheLoai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "maSach") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            // Tạo Pageable
            org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();
            
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, sort);
            
            // Lấy danh sách sách theo thể loại từ cache
            List<Sach> sachList = sachCacheService.findByTheLoaiWithCache(maTheLoai, pageable);
            long totalElements = sachCacheService.countByTheLoai(maTheLoai);
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // Convert sang Map và thêm thông tin seller
            java.util.List<Map<String, Object>> sachListWithSeller = sachList.stream()
                .map(sach -> {
                    Map<String, Object> sachMap = new HashMap<>();
                    sachMap.put("maSach", sach.getMaSach());
                    sachMap.put("tenSach", sach.getTenSach());
                    sachMap.put("tenTacGia", sach.getTenTacGia());
                    sachMap.put("isbn", sach.getISBN());
                    sachMap.put("moTa", sach.getMoTa());
                    sachMap.put("giaNiemYet", sach.getGiaNiemYet());
                    sachMap.put("giaBan", sach.getGiaBan());
                    sachMap.put("soLuong", sach.getSoLuong());
                    sachMap.put("trungBinhXepHang", sach.getTrungBinhXepHang());
                    sachMap.put("soLuongDaBan", sach.getSoLuongDaBan());
                    sachMap.put("giamGia", sach.getGiamGia());
                    
                    // Lấy ảnh bìa
                    String coverImageUrl = null;
                    if (sach.getDanhSachHinhAnh() != null && !sach.getDanhSachHinhAnh().isEmpty()) {
                        coverImageUrl = sach.getDanhSachHinhAnh().stream()
                                .filter(ha -> ha.isLaIcon())
                                .findFirst()
                                .map(ha -> ha.getDuongDan())
                                .orElse(null);
                    }
                    sachMap.put("coverImageUrl", coverImageUrl);
                    
                    // Tìm seller
                    Optional<SellerBooks> sellerBooksOpt = 
                        sellerBooksRepository.findFirstBySachMaSachAndActive(sach.getMaSach());
                    
                    if (sellerBooksOpt.isPresent()) {
                        SellerBooks sellerBooks = sellerBooksOpt.get();
                        sachMap.put("sellerId", sellerBooks.getSeller().getMaNguoiDung());
                        sachMap.put("sellerName", sellerBooks.getSeller().getTenGianHang() != null 
                            ? sellerBooks.getSeller().getTenGianHang()
                            : sellerBooks.getSeller().getHoDem() + " " + sellerBooks.getSeller().getTen());
                    } else {
                        sachMap.put("sellerId", null);
                        sachMap.put("sellerName", null);
                    }
                    
                    return sachMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            // Tạo response pagination
            Map<String, Object> response = new HashMap<>();
            response.put("content", sachListWithSeller);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("maTheLoai", maTheLoai);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
}
