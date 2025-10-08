package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SachService;
import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.dao.SellerBooksRepository;
import com.example.web_ban_sach.entity.Sach;
import com.example.web_ban_sach.entity.SellerBooks;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            
            // Lấy danh sách sách
            org.springframework.data.domain.Page<Sach> sachPage = sachRepository.findAll(pageable);
            
            // Convert sang Map và thêm thông tin seller
            java.util.List<Map<String, Object>> sachListWithSeller = sachPage.getContent().stream()
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
            response.put("totalElements", sachPage.getTotalElements());
            response.put("totalPages", sachPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
}
