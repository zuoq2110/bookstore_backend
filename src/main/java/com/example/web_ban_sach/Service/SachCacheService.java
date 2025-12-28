package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.dto.SachCacheDTO;
import com.example.web_ban_sach.entity.Sach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service để cache dữ liệu sách
 * Cache ở service layer để tránh lỗi serialization với ResponseEntity
 */
@Service
public class SachCacheService {
    
    @Autowired
    private SachRepository sachRepository;
    
    /**
     * Cache danh sách sách (List, không phải Page để dễ serialize)
     * Load eager tất cả lazy collections để tránh LazyInitializationException
     * Load từng bước để tránh MultipleBagFetchException
     * Key format: sach_list_page_size_sort_dir
     */
    @Cacheable(value = "sach", key = "'list_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString().replaceAll('[^a-zA-Z0-9]', '_')")
    @Transactional(readOnly = true)
    public List<Sach> findAllWithCache(Pageable pageable) {
        // Extract sort parameters
        String sortBy = "maSach";  // default
        String sortDir = "desc";   // default
        
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().iterator().next();
            sortBy = order.getProperty();
            sortDir = order.getDirection().isAscending() ? "asc" : "desc";
        }
        
        // Bước 1: Lấy tất cả IDs với sorting
        List<Integer> allIds = sachRepository.findAllSachIdsWithSort(sortBy, sortDir);
        
        // Bước 2: Áp dụng pagination trên IDs (đã được sort)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allIds.size());
        List<Integer> pageIds = allIds.subList(start, end);
        
        if (pageIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Bước 3: Load sách với hình ảnh
        List<Sach> sachList = sachRepository.findByMaSachListWithImages(pageIds);
        
        // Bước 4: Load thể loại (tránh MultipleBagFetchException)
        // Query này trả về các entity khác trong Hibernate session
        List<Sach> sachWithCategories = sachRepository.findByMaSachListWithCategories(pageIds);
        
        // Bước 5: Merge data thủ công - tạo Map để lookup nhanh
        java.util.Map<Integer, List<com.example.web_ban_sach.entity.TheLoai>> categoryMap = 
            sachWithCategories.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Sach::getMaSach,
                    Sach::getDanhSachTheLoai
                ));
        
        // Bước 6: Sort lại theo order của pageIds để maintain sorting
        java.util.Map<Integer, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < pageIds.size(); i++) {
            orderMap.put(pageIds.get(i), i);
        }
        
        sachList.sort((s1, s2) -> {
            Integer order1 = orderMap.get(s1.getMaSach());
            Integer order2 = orderMap.get(s2.getMaSach());
            return order1.compareTo(order2);
        });
        
        // Bước 6: Set thể loại cho từng sách
        sachList.forEach(sach -> {
            List<com.example.web_ban_sach.entity.TheLoai> categories = categoryMap.get(sach.getMaSach());
            if (categories != null) {
                sach.setDanhSachTheLoai(categories);
            }
        });
        
        return sachList;
    }
    
    /**
     * Lấy danh sách sách KHÔNG dùng cache (query trực tiếp DB)
     * Dùng để so sánh performance với cached version
     * KHÔNG có @Cacheable - bypass tất cả Spring Cache
     */
    @Transactional(readOnly = true)
    public List<Sach> findAllWithoutCache(Pageable pageable) {
        // Bước 1: Lấy tất cả IDs
        List<Integer> allIds = sachRepository.findAllSachIds();
        
        // Bước 2: Áp dụng pagination trên IDs
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allIds.size());
        List<Integer> pageIds = allIds.subList(start, end);
        
        if (pageIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Bước 3: Load sách với hình ảnh
        List<Sach> sachList = sachRepository.findByMaSachListWithImages(pageIds);
        
        // Bước 4: Load thể loại (tránh MultipleBagFetchException)
        List<Sach> sachWithCategories = sachRepository.findByMaSachListWithCategories(pageIds);
        
        // Bước 5: Merge data thủ công - tạo Map để lookup nhanh
        java.util.Map<Integer, List<com.example.web_ban_sach.entity.TheLoai>> categoryMap = 
            sachWithCategories.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Sach::getMaSach,
                    Sach::getDanhSachTheLoai
                ));
        
        // Bước 6: Set thể loại cho từng sách
        sachList.forEach(sach -> {
            List<com.example.web_ban_sach.entity.TheLoai> categories = categoryMap.get(sach.getMaSach());
            if (categories != null) {
                sach.setDanhSachTheLoai(categories);
            }
        });
        
        return sachList;
    }
    
    /**
     * Lấy total count (không cache vì thay đổi ít)
     */
    public long count() {
        return sachRepository.count();
    }
    
    /**
     * Cache sách theo ID
     * Load eager danhSachTheLoai và danhSachHinhAnh để tránh LazyInitializationException
     */
    @Cacheable(value = "sach", key = "'id_' + #id")
    @Transactional(readOnly = true)
    public Optional<Sach> findByIdWithCache(int id) {
        return sachRepository.findByIdWithCollections(id);
    }
    
    /**
     * Cache danh sách sách THEO THỂ LOẠI với pagination
     * Load eager tất cả lazy collections
     * Key format: sach_theloai_{maTheLoai}_page_size_sort_dir
     */
    @Cacheable(value = "sach", key = "'theloai_' + #maTheLoai + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString().replaceAll('[^a-zA-Z0-9]', '_')")
    @Transactional(readOnly = true)
    public List<Sach> findByTheLoaiWithCache(int maTheLoai, Pageable pageable) {
        // Bước 1: Lấy IDs của sách theo thể loại
        List<Integer> allIds = sachRepository.findSachIdsByTheLoai(maTheLoai);
        
        // Bước 2: Áp dụng pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allIds.size());
        List<Integer> pageIds = allIds.subList(start, end);
        
        if (pageIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Bước 3: Load sách với hình ảnh
        List<Sach> sachList = sachRepository.findByMaSachListWithImages(pageIds);
        
        // Bước 4: Load thể loại
        List<Sach> sachWithCategories = sachRepository.findByMaSachListWithCategories(pageIds);
        
        // Bước 5: Merge data
        java.util.Map<Integer, List<com.example.web_ban_sach.entity.TheLoai>> categoryMap = 
            sachWithCategories.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Sach::getMaSach,
                    Sach::getDanhSachTheLoai
                ));
        
        sachList.forEach(sach -> {
            List<com.example.web_ban_sach.entity.TheLoai> categories = categoryMap.get(sach.getMaSach());
            if (categories != null) {
                sach.setDanhSachTheLoai(categories);
            }
        });
        
        return sachList;
    }
    
    /**
     * Đếm số sách theo thể loại (không cache)
     */
    public long countByTheLoai(int maTheLoai) {
        return sachRepository.findSachIdsByTheLoai(maTheLoai).size();
    }
    
    /**
     * ⚡ Cache lightweight - chỉ cache DTO đơn giản
     * Nhanh hơn nhiều cho dataset lớn
     * Key format: sach-light_list_page_size_sort_dir
     */
    @Cacheable(value = "sach-light", key = "'list_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString().replaceAll('[^a-zA-Z0-9]', '_')")
    @Transactional(readOnly = true)
    public List<SachCacheDTO> findAllLightweight(Pageable pageable) {
        // Bước 1: Lấy tất cả IDs
        List<Integer> allIds = sachRepository.findAllSachIds();
        
        // Bước 2: Áp dụng pagination trên IDs
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allIds.size());
        List<Integer> pageIds = allIds.subList(start, end);
        
        if (pageIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Bước 3: Load sách với hình ảnh
        List<Sach> sachList = sachRepository.findByMaSachListWithImages(pageIds);
        
        // Bước 4: Load thể loại
        List<Sach> sachWithCategories = sachRepository.findByMaSachListWithCategories(pageIds);
        
        // Bước 5: Merge data
        java.util.Map<Integer, List<com.example.web_ban_sach.entity.TheLoai>> categoryMap = 
            sachWithCategories.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Sach::getMaSach,
                    Sach::getDanhSachTheLoai
                ));
        
        sachList.forEach(sach -> {
            List<com.example.web_ban_sach.entity.TheLoai> categories = categoryMap.get(sach.getMaSach());
            if (categories != null) {
                sach.setDanhSachTheLoai(categories);
            }
        });
        
        // Bước 6: Convert sang DTO lightweight
        return sachList.stream()
            .map(SachCacheDTO::fromEntity)
            .collect(Collectors.toList());
    }
}
