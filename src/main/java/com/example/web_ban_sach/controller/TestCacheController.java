package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SachCacheService;
import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.entity.Sach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/test-cache")
@CrossOrigin(origins = "*")
public class TestCacheController {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SachCacheService sachCacheService;
    
    @Autowired
    private SachRepository sachRepository;
    
    /**
     * Test cache với API public
     * GET http://localhost:8080/test-cache/get?id=1
     */
    @GetMapping("/get")
    @Cacheable(value = "test", key = "#id")
    public ResponseEntity<?> testGet(@RequestParam String id) {
        // Simulate slow query
        try {
            Thread.sleep(2000); // 2 seconds delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("message", "Data from DATABASE (slow)");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("cached", false);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Clear cache
     * DELETE http://localhost:8080/test-cache/clear
     */
    @DeleteMapping("/clear")
    @CacheEvict(value = "test", allEntries = true)
    public ResponseEntity<?> clearCache() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Cache cleared successfully!");
        result.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }
    
    /**
     * Clear ALL caches in Redis
     * DELETE http://localhost:8080/test-cache/clear-all
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAllCache() {
        try {
            // Xóa tất cả các cache được quản lý bởi CacheManager
            cacheManager.getCacheNames().forEach(cacheName -> {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "All caches cleared successfully!");
            result.put("caches", cacheManager.getCacheNames());
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to clear cache: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * FLUSHALL - Xóa toàn bộ Redis database
     * DELETE http://localhost:8080/test-cache/flush-all
     * ⚠️ WARNING: Xóa TẤT CẢ dữ liệu trong Redis!
     */
    @DeleteMapping("/flush-all")
    public ResponseEntity<?> flushAll() {
        try {
            Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushAll();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "⚠️ ALL Redis data FLUSHED!");
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to flush Redis: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * List all cache keys
     * GET http://localhost:8080/test-cache/keys
     */
    @GetMapping("/keys")
    public ResponseEntity<?> listKeys() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalKeys", keys != null ? keys.size() : 0);
            result.put("keys", keys);
            result.put("cacheNames", cacheManager.getCacheNames());
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to list keys: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get cache info
     * GET http://localhost:8080/test-cache/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getCacheInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Test cache endpoint - No authentication required");
        result.put("usage", "Call /test-cache/get?id=1 twice to see caching in action");
        result.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🚀 TEST HIỆU SUẤT: So sánh Redis Cache vs Database
     * GET http://localhost:8080/test-cache/performance?page=0&size=10&iterations=10
     */
    @GetMapping("/performance")
    public ResponseEntity<?> testPerformance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "10") int iterations) {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rounds = new ArrayList<>();
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Xóa cache trước khi test
        Objects.requireNonNull(cacheManager.getCache("sach")).clear();
        Objects.requireNonNull(cacheManager.getCache("sach-light")).clear();
        
        long totalCacheTime = 0;
        long totalCacheLightTime = 0;
        long totalDbTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            Map<String, Object> round = new HashMap<>();
            round.put("round", i + 1);
            
            // Test 1: Database (không cache)
            Objects.requireNonNull(cacheManager.getCache("sach")).clear();
            Objects.requireNonNull(cacheManager.getCache("sach-light")).clear();
            long dbStart = System.currentTimeMillis();
            List<?> dbResult = sachRepository.findAll(pageable).getContent();
            long dbEnd = System.currentTimeMillis();
            long dbTime = dbEnd - dbStart;
            totalDbTime += dbTime;
            
            // Test 2: Cache Full Entity (lần đầu - cache miss)
            Objects.requireNonNull(cacheManager.getCache("sach")).clear();
            long cacheMissStart = System.currentTimeMillis();
            sachCacheService.findAllWithCache(pageable);
            long cacheMissEnd = System.currentTimeMillis();
            long cacheMissTime = cacheMissEnd - cacheMissStart;
            
            // Test 3: Cache Full Entity (lần 2 - cache hit)
            long cacheHitStart = System.currentTimeMillis();
            sachCacheService.findAllWithCache(pageable);
            long cacheHitEnd = System.currentTimeMillis();
            long cacheHitTime = cacheHitEnd - cacheHitStart;
            totalCacheTime += cacheHitTime;
            
            // Test 4: Cache Lightweight DTO (cache miss)
            Objects.requireNonNull(cacheManager.getCache("sach-light")).clear();
            long cacheLightMissStart = System.currentTimeMillis();
            sachCacheService.findAllLightweight(pageable);
            long cacheLightMissTime = System.currentTimeMillis() - cacheLightMissStart;
            
            // Test 5: Cache Lightweight DTO (cache hit)
            long cacheLightHitStart = System.currentTimeMillis();
            sachCacheService.findAllLightweight(pageable);
            long cacheLightHitTime = System.currentTimeMillis() - cacheLightHitStart;
            totalCacheLightTime += cacheLightHitTime;
            
            round.put("database_ms", dbTime);
            round.put("cache_miss_ms", cacheMissTime);
            round.put("cache_full_hit_ms", cacheHitTime);
            round.put("cache_light_miss_ms", cacheLightMissTime);
            round.put("cache_light_hit_ms", cacheLightHitTime);
            round.put("records", ((List<?>) dbResult).size());
            round.put("speedup_full", String.format("%.2fx", (double) dbTime / cacheHitTime));
            round.put("speedup_light", String.format("%.2fx", (double) dbTime / cacheLightHitTime));
            
            rounds.add(round);
        }
        
        // Tính toán thống kê
        double avgDbTime = totalDbTime / (double) iterations;
        double avgCacheTime = totalCacheTime / (double) iterations;
        double avgCacheLightTime = totalCacheLightTime / (double) iterations;
        double speedupFull = avgDbTime / avgCacheTime;
        double speedupLight = avgDbTime / avgCacheLightTime;
        
        result.put("rounds", rounds);
        result.put("summary", Map.of(
            "iterations", iterations,
            "avg_database_ms", String.format("%.2f", avgDbTime),
            "avg_cache_full_hit_ms", String.format("%.2f", avgCacheTime),
            "avg_cache_light_hit_ms", String.format("%.2f", avgCacheLightTime),
            "speedup_full_entity", String.format("%.2fx", speedupFull),
            "speedup_lightweight", String.format("%.2fx", speedupLight),
            "performance_gain_light", String.format("%.1f%%", (speedupLight - 1) * 100)
        ));
        result.put("recommendation", 
            speedupLight > 2 ? "✅ Lightweight DTO cache mang lại hiệu suất tốt!" :
            speedupLight > 1 ? "⚡ Lightweight DTO cache nhanh hơn DB nhưng có thể tối ưu thêm" :
            "⚠️ Nên sử dụng direct DB query cho dataset này");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 📊 TEST LOAD: Kiểm tra hiệu suất dưới tải cao
     * GET http://localhost:8080/test-cache/load-test?requests=100&concurrent=10
     */
    @GetMapping("/load-test")
    public ResponseEntity<?> loadTest(
            @RequestParam(defaultValue = "100") int requests,
            @RequestParam(defaultValue = "10") int concurrent) {
        
        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(0, 10);
        
        // Warm up cache
        sachCacheService.findAllWithCache(pageable);
        
        long startTime = System.currentTimeMillis();
        int completed = 0;
        
        for (int i = 0; i < requests; i++) {
            sachCacheService.findAllWithCache(pageable);
            completed++;
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        result.put("total_requests", requests);
        result.put("completed", completed);
        result.put("total_time_ms", totalTime);
        result.put("avg_time_per_request_ms", String.format("%.2f", totalTime / (double) requests));
        result.put("requests_per_second", String.format("%.2f", (requests * 1000.0) / totalTime));
        result.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔍 Xem chi tiết một cached object
     * GET http://localhost:8080/test-cache/inspect?id=1
     */
    @GetMapping("/inspect")
    public ResponseEntity<?> inspectCache(@RequestParam int id) {
        Map<String, Object> result = new HashMap<>();
        
        long start = System.currentTimeMillis();
        Optional<Sach> sach = sachCacheService.findByIdWithCache(id);
        long end = System.currentTimeMillis();
        
        if (sach.isPresent()) {
            Sach s = sach.get();
            result.put("found", true);
            result.put("query_time_ms", end - start);
            result.put("data", Map.of(
                "maSach", s.getMaSach(),
                "tenSach", s.getTenSach(),
                "giaBan", s.getGiaBan(),
                "soLuong", s.getSoLuong(),
                "hasTheLoai", s.getDanhSachTheLoai() != null && !s.getDanhSachTheLoai().isEmpty(),
                "theLoaiCount", s.getDanhSachTheLoai() != null ? s.getDanhSachTheLoai().size() : 0,
                "hasHinhAnh", s.getDanhSachHinhAnh() != null && !s.getDanhSachHinhAnh().isEmpty()
            ));
            
            // Check if in Redis
            String cacheKey = "sach::id_" + id;
            Boolean hasKey = redisTemplate.hasKey(cacheKey);
            result.put("cached_in_redis", hasKey != null && hasKey);
        } else {
            result.put("found", false);
            result.put("message", "Sách không tồn tại");
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 📈 TEST NHIỀU SIZE: So sánh với dataset khác nhau
     * GET http://localhost:8080/test-cache/benchmark
     */
    @GetMapping("/benchmark")
    public ResponseEntity<?> benchmark() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> benchmarks = new ArrayList<>();
        
        int[] sizes = {10, 20, 50, 100, 200};
        
        for (int size : sizes) {
            Map<String, Object> bench = new HashMap<>();
            Pageable pageable = PageRequest.of(0, size);
            
            // Clear cache
            Objects.requireNonNull(cacheManager.getCache("sach")).clear();
            Objects.requireNonNull(cacheManager.getCache("sach-light")).clear();
            
            // 1. Database query (5 lần để lấy trung bình)
            long totalDb = 0;
            for (int i = 0; i < 5; i++) {
                long start = System.currentTimeMillis();
                sachRepository.findAll(pageable).getContent();
                totalDb += System.currentTimeMillis() - start;
            }
            long avgDb = totalDb / 5;
            
            // 2. Cache Full Entity miss
            Objects.requireNonNull(cacheManager.getCache("sach")).clear();
            long cacheMissStart = System.currentTimeMillis();
            sachCacheService.findAllWithCache(pageable);
            long cacheMissTime = System.currentTimeMillis() - cacheMissStart;
            
            // 3. Cache Full Entity hit (5 lần để lấy trung bình)
            long totalCacheHit = 0;
            for (int i = 0; i < 5; i++) {
                long start = System.currentTimeMillis();
                sachCacheService.findAllWithCache(pageable);
                totalCacheHit += System.currentTimeMillis() - start;
            }
            long avgCacheHit = totalCacheHit / 5;
            
            // 4. Cache Lightweight miss
            Objects.requireNonNull(cacheManager.getCache("sach-light")).clear();
            long cacheLightMissStart = System.currentTimeMillis();
            sachCacheService.findAllLightweight(pageable);
            long cacheLightMissTime = System.currentTimeMillis() - cacheLightMissStart;
            
            // 5. Cache Lightweight hit (5 lần)
            long totalCacheLightHit = 0;
            for (int i = 0; i < 5; i++) {
                long start = System.currentTimeMillis();
                sachCacheService.findAllLightweight(pageable);
                totalCacheLightHit += System.currentTimeMillis() - start;
            }
            long avgCacheLightHit = totalCacheLightHit / 5;
            
            bench.put("size", size);
            bench.put("db_avg_ms", avgDb);
            bench.put("cache_full_miss_ms", cacheMissTime);
            bench.put("cache_full_hit_ms", avgCacheHit);
            bench.put("cache_light_miss_ms", cacheLightMissTime);
            bench.put("cache_light_hit_ms", avgCacheLightHit);
            bench.put("speedup_full", String.format("%.2fx", (double) avgDb / avgCacheHit));
            bench.put("speedup_light", String.format("%.2fx", (double) avgDb / avgCacheLightHit));
            bench.put("winner", avgCacheLightHit < avgDb ? "Cache Light" : 
                               avgCacheHit < avgDb ? "Cache Full" : "Database");
            
            benchmarks.add(bench);
        }
        
        result.put("benchmarks", benchmarks);
        result.put("conclusion", Map.of(
            "note", "Redis cache hiệu quả nhất với dataset nhỏ-vừa (< 100 records)",
            "recommendation", "Sử dụng cache cho frequent queries với ít data, direct DB cho bulk operations",
            "sweet_spot", "Cache tốt nhất cho 10-50 records trên localhost, 50-200 records trên remote DB"
        ));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * ⚡ TEST CHỈ CACHE HIT: Đo tốc độ thuần túy của Redis
     * GET http://localhost:8080/test-cache/cache-speed?iterations=100
     */
    @GetMapping("/cache-speed")
    public ResponseEntity<?> cacheSpeed(@RequestParam(defaultValue = "100") int iterations) {
        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(0, 10);
        
        // Warm up cache
        sachCacheService.findAllWithCache(pageable);
        
        List<Long> times = new ArrayList<>();
        
        // Test multiple times
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            sachCacheService.findAllWithCache(pageable);
            long end = System.nanoTime();
            times.add((end - start) / 1_000_000); // Convert to ms
        }
        
        // Calculate statistics
        times.sort(Long::compareTo);
        long min = times.get(0);
        long max = times.get(times.size() - 1);
        long median = times.get(times.size() / 2);
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = times.get((int) (times.size() * 0.95));
        long p99 = times.get((int) (times.size() * 0.99));
        
        result.put("iterations", iterations);
        result.put("statistics_ms", Map.of(
            "min", min,
            "max", max,
            "avg", String.format("%.2f", avg),
            "median", median,
            "p95", p95,
            "p99", p99
        ));
        result.put("performance", Map.of(
            "cache_stability", max <= min * 3 ? "✅ Stable" : "⚠️ Unstable",
            "avg_latency", avg < 10 ? "✅ Excellent (<10ms)" : 
                          avg < 50 ? "✅ Good (<50ms)" : "⚠️ Needs optimization"
        ));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🎯 TEST CACHE BY ID: So sánh findById với/không có cache
     * GET http://localhost:8080/test-cache/test-by-id?id=1&iterations=20
     */
    @GetMapping("/test-by-id")
    public ResponseEntity<?> testById(
            @RequestParam int id,
            @RequestParam(defaultValue = "20") int iterations) {
        
        Map<String, Object> result = new HashMap<>();
        
        long totalDbTime = 0;
        long totalCacheTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            // Test Database
            Objects.requireNonNull(cacheManager.getCache("sach")).clear();
            long dbStart = System.currentTimeMillis();
            sachRepository.findById(id);
            long dbTime = System.currentTimeMillis() - dbStart;
            totalDbTime += dbTime;
            
            // Test Cache (warm up first)
            Objects.requireNonNull(cacheManager.getCache("sach")).clear();
            sachCacheService.findByIdWithCache(id); // Warm up
            
            long cacheStart = System.currentTimeMillis();
            sachCacheService.findByIdWithCache(id);
            long cacheTime = System.currentTimeMillis() - cacheStart;
            totalCacheTime += cacheTime;
        }
        
        double avgDb = totalDbTime / (double) iterations;
        double avgCache = totalCacheTime / (double) iterations;
        
        result.put("test_type", "findById");
        result.put("book_id", id);
        result.put("iterations", iterations);
        result.put("avg_database_ms", String.format("%.2f", avgDb));
        result.put("avg_cache_ms", String.format("%.2f", avgCache));
        result.put("speedup", String.format("%.2fx", avgDb / avgCache));
        result.put("time_saved_ms", String.format("%.2f", avgDb - avgCache));
        
        return ResponseEntity.ok(result);
    }
}
