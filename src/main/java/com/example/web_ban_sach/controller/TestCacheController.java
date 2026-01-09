package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Sach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/test-pagination")
@CrossOrigin(origins = "*")
public class TestCacheController {
    
    @Autowired
    private SachRepository sachRepository;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    /**
     * 📚 TEST PHÂN TRANG SÁCH: So sánh lấy tất cả vs có phân trang (KHÔNG dùng cache)
     * GET http://localhost:8080/test-pagination/books?iterations=5&pageSize=20
     */
    @GetMapping("/books")
    public ResponseEntity<?> testBooksPagination(
            @RequestParam(defaultValue = "1") int iterations,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> testResults = new ArrayList<>();
        
        long totalPaginatedTime = 0;
        long totalNonPaginatedTime = 0;
        
        System.out.println("📚 Testing Books: Pagination vs Non-Pagination (NO CACHE)");
        System.out.println("Iterations: " + iterations + ", Page Size: " + pageSize);
        
        for (int i = 0; i < iterations; i++) {
            Map<String, Object> round = new HashMap<>();
            round.put("round", i + 1);
            
            // TEST 1: Có phân trang - chỉ lấy trang đầu (DIRECT REPOSITORY)
            long paginatedStart = System.currentTimeMillis();
            Pageable pageable = PageRequest.of(0, pageSize);
            List<Sach> paginatedBooks = sachRepository.findAll(pageable).getContent();
            long paginatedEnd = System.currentTimeMillis();
            long paginatedTime = paginatedEnd - paginatedStart;
            totalPaginatedTime += paginatedTime;
            
            // TEST 2: Không phân trang - lấy tất cả (DIRECT REPOSITORY)
            long nonPaginatedStart = System.currentTimeMillis();
            List<Sach> allBooks = sachRepository.findAll();
            long nonPaginatedEnd = System.currentTimeMillis();
            long nonPaginatedTime = nonPaginatedEnd - nonPaginatedStart;
            totalNonPaginatedTime += nonPaginatedTime;
            
            round.put("paginatedTime", paginatedTime + "ms");
            round.put("paginatedCount", paginatedBooks.size());
            round.put("nonPaginatedTime", nonPaginatedTime + "ms");
            round.put("nonPaginatedCount", allBooks.size());
            round.put("improvement", String.format("%.2fx faster", (double) nonPaginatedTime / paginatedTime));
            
            testResults.add(round);
            
            System.out.println(String.format("Round %d: Paginated=%dms (%d books) vs All=%dms (%d books)", 
                i + 1, paginatedTime, paginatedBooks.size(), nonPaginatedTime, allBooks.size()));
        }
        
        double avgPaginated = (double) totalPaginatedTime / iterations;
        double avgNonPaginated = (double) totalNonPaginatedTime / iterations;
        double improvement = avgNonPaginated / avgPaginated;
        
        result.put("testType", "Books Pagination Performance (NO CACHE)");
        result.put("iterations", iterations);
        result.put("pageSize", pageSize);
        result.put("results", testResults);
        result.put("summary", Map.of(
            "avgPaginatedTime", Math.round(avgPaginated) + "ms",
            "avgNonPaginatedTime", Math.round(avgNonPaginated) + "ms", 
            "improvement", String.format("%.2fx faster", improvement),
            "timeSaved", Math.round(avgNonPaginated - avgPaginated) + "ms per request"
        ));
        result.put("recommendation", improvement > 2 ? 
            "✅ Phân trang có hiệu suất vượt trội cho sách!" :
            "⚠️ Khác biệt không đáng kể - có thể do dataset nhỏ");
        result.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 👥 TEST PHÂN TRANG USER: So sánh lấy tất cả vs có phân trang (KHÔNG dùng cache)
     * GET http://localhost:8080/test-pagination/users?iterations=5&pageSize=10
     */
    @GetMapping("/users")
    public ResponseEntity<?> testUsersPagination(
            @RequestParam(defaultValue = "1") int iterations,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> testResults = new ArrayList<>();
        
        long totalPaginatedTime = 0;
        long totalNonPaginatedTime = 0;
        
        System.out.println("👥 Testing Users: Pagination vs Non-Pagination (NO CACHE)");
        System.out.println("Iterations: " + iterations + ", Page Size: " + pageSize);
        
        for (int i = 0; i < iterations; i++) {
            Map<String, Object> round = new HashMap<>();
            round.put("round", i + 1);
            
            // TEST 1: Có phân trang - chỉ lấy trang đầu (DIRECT REPOSITORY)
            long paginatedStart = System.currentTimeMillis();
            Pageable pageable = PageRequest.of(0, pageSize);
            List<NguoiDung> paginatedUsers = nguoiDungRepository.findAll(pageable).getContent();
            long paginatedEnd = System.currentTimeMillis();
            long paginatedTime = paginatedEnd - paginatedStart;
            totalPaginatedTime += paginatedTime;
            
            // TEST 2: Không phân trang - lấy tất cả (DIRECT REPOSITORY)
            long nonPaginatedStart = System.currentTimeMillis();
            List<NguoiDung> allUsers = nguoiDungRepository.findAll();
            long nonPaginatedEnd = System.currentTimeMillis();
            long nonPaginatedTime = nonPaginatedEnd - nonPaginatedStart;
            totalNonPaginatedTime += nonPaginatedTime;
            
            round.put("paginatedTime", paginatedTime + "ms");
            round.put("paginatedCount", paginatedUsers.size());
            round.put("nonPaginatedTime", nonPaginatedTime + "ms");
            round.put("nonPaginatedCount", allUsers.size());
            round.put("improvement", String.format("%.2fx faster", (double) nonPaginatedTime / paginatedTime));
            
            testResults.add(round);
            
            System.out.println(String.format("Round %d: Paginated=%dms (%d users) vs All=%dms (%d users)", 
                i + 1, paginatedTime, paginatedUsers.size(), nonPaginatedTime, allUsers.size()));
        }
        
        double avgPaginated = (double) totalPaginatedTime / iterations;
        double avgNonPaginated = (double) totalNonPaginatedTime / iterations;
        double improvement = avgNonPaginated / avgPaginated;
        
        result.put("testType", "Users Pagination Performance (NO CACHE)");
        result.put("iterations", iterations);
        result.put("pageSize", pageSize);
        result.put("results", testResults);
        result.put("summary", Map.of(
            "avgPaginatedTime", Math.round(avgPaginated) + "ms",
            "avgNonPaginatedTime", Math.round(avgNonPaginated) + "ms",
            "improvement", String.format("%.2fx faster", improvement),
            "timeSaved", Math.round(avgNonPaginated - avgPaginated) + "ms per request"
        ));
        result.put("recommendation", improvement > 2 ?
            "✅ Phân trang có hiệu suất vượt trội cho users!" :
            "⚠️ Khác biệt không đáng kể - có thể do dataset nhỏ");
        result.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🎯 TEST TỔNG HỢP: Test cả sách và user cùng lúc - KHÔNG DÙNG CACHE
     * GET http://localhost:8080/test-pagination/comparison?iterations=3
     */
    @GetMapping("/comparison")
    public ResponseEntity<?> testPaginationComparison(@RequestParam(defaultValue = "1") int iterations) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Test Books với page size 20
        long booksStart = System.currentTimeMillis();
        ResponseEntity<?> booksTest = testBooksPagination(iterations, 20);
        long booksEnd = System.currentTimeMillis();
        
        // Test Users với page size 10  
        long usersStart = System.currentTimeMillis();
        ResponseEntity<?> usersTest = testUsersPagination(iterations, 10);
        long usersEnd = System.currentTimeMillis();
        
        // Thống kê database
        long totalBooks = sachRepository.count();
        long totalUsers = nguoiDungRepository.count();
        
        result.put("testType", "Complete Pagination Comparison (NO CACHE)");
        result.put("iterations", iterations);
        result.put("database", Map.of(
            "totalBooks", totalBooks,
            "totalUsers", totalUsers,
            "recommendation", (totalBooks < 100 && totalUsers < 50) ? 
                "⚠️ Dataset nhỏ - khác biệt performance có thể không rõ ràng" :
                "✅ Dataset đủ lớn để thấy được lợi ích của phân trang"
        ));
        result.put("tests", Map.of(
            "booksTest", Map.of(
                "executionTime", (booksEnd - booksStart) + "ms",
                "results", booksTest.getBody()
            ),
            "usersTest", Map.of(
                "executionTime", (usersEnd - usersStart) + "ms", 
                "results", usersTest.getBody()
            )
        ));
        result.put("totalExecutionTime", (usersEnd - booksStart) + "ms");
        result.put("note", "🚫 KHÔNG sử dụng cache - Test thuần hiệu suất database + phân trang");
        result.put("timestamp", LocalDateTime.now().toString());
        
        System.out.println("📊 Complete test finished (NO CACHE) - Books: " + totalBooks + ", Users: " + totalUsers);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 📈 THÔNG TIN DATABASE: Kiểm tra số lượng dữ liệu hiện tại
     * GET http://localhost:8080/test-pagination/info
     */
    @GetMapping("/info") 
    public ResponseEntity<?> getDatabaseInfo() {
        Map<String, Object> result = new HashMap<>();
        
        long totalBooks = sachRepository.count();
        long totalUsers = nguoiDungRepository.count();
        
        result.put("database", Map.of(
            "totalBooks", totalBooks,
            "totalUsers", totalUsers
        ));
        result.put("testRecommendation", Map.of(
            "books", totalBooks < 100 ? 
                "⚠️ Ít sách (" + totalBooks + ") - khác biệt performance có thể không rõ ràng" :
                "✅ Đủ sách (" + totalBooks + ") để thấy lợi ích phân trang",
            "users", totalUsers < 50 ?
                "⚠️ Ít users (" + totalUsers + ") - khác biệt performance có thể không rõ ràng" :
                "✅ Đủ users (" + totalUsers + ") để thấy lợi ích phân trang"
        ));
        result.put("suggestedTests", List.of(
            "GET /test-pagination/books - Test phân trang sách (KHÔNG cache)",
            "GET /test-pagination/users - Test phân trang users (KHÔNG cache)", 
            "GET /test-pagination/comparison - Test tổng hợp (KHÔNG cache)"
        ));
        result.put("note", "🚫 TẤT CẢ test đều KHÔNG dùng cache - chỉ test thuần database performance");
        result.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(result);
    }
}
