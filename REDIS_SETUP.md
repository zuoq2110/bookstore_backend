# Hướng Dẫn Sử Dụng Redis Cache

## Cài Đặt Redis

### Windows
1. **Tải Redis từ trang chính thức:**
   - Download tại: https://github.com/microsoftarchive/redis/releases
   - Hoặc sử dụng WSL/Docker

2. **Cài đặt qua Docker (Khuyến nghị):**
   ```bash
   docker run --name redis-cache -p 6379:6379 -d redis:latest
   ```

3. **Khởi động Redis:**
   ```bash
   redis-server
   ```

### Kiểm tra Redis đang chạy:
```bash
redis-cli ping
# Kết quả: PONG
```

## Cấu Hình Đã Được Thêm

### 1. Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

### 2. Redis Configuration (application.properties)
```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=60000

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000
spring.cache.redis.cache-null-values=false
```

### 3. Redis Config Class (RedisConfig.java)
- Cấu hình RedisTemplate với Jackson serializer
- Thiết lập CacheManager với TTL tùy chỉnh cho từng cache:
  - `sach`: 30 phút
  - `theloai`: 2 giờ
  - `donhang`: 10 phút
  - `nguoidung`: 15 phút
  - `notifications`: 5 phút

## Cache Annotations Đã Áp Dụng

### 1. SachServiceImpl
```java
@CacheEvict(value = "sach", allEntries = true)
public ResponseEntity<?> save(JsonNode jsonNode) { ... }

@CacheEvict(value = "sach", allEntries = true)
public ResponseEntity<?> update(JsonNode jsonNode) { ... }
```

### 2. UserServiceImpl
```java
@CacheEvict(value = "nguoidung", key = "#jsonNode.get('maNguoiDung').asInt()")
public ResponseEntity<?> doiMatKhau(JsonNode jsonNode) { ... }

@CacheEvict(value = "nguoidung", allEntries = true)
public ResponseEntity<?> save(JsonNode jsonNode, String option) { ... }

@CacheEvict(value = "nguoidung", key = "#id")
public ResponseEntity<?> delete(int id) { ... }
```

### 3. NotificationController
```java
@Cacheable(value = "notifications", key = "#userId + '_' + #page + '_' + #size + '_' + #unreadOnly")
public ResponseEntity<?> getNotifications(...) { ... }

@Cacheable(value = "notifications", key = "'unread_' + #userId")
public ResponseEntity<?> getUnreadCount(@RequestParam int userId) { ... }

@CacheEvict(value = "notifications", allEntries = true)
public ResponseEntity<?> markAsRead(@PathVariable int notificationId) { ... }
```

## Các Annotation Cache

### @Cacheable
- **Mục đích**: Cache kết quả của method
- **Khi nào dùng**: GET/SELECT operations
- **Ví dụ**: 
  ```java
  @Cacheable(value = "sach", key = "#id")
  public Sach findById(int id) { ... }
  ```

### @CachePut
- **Mục đích**: Update cache sau khi method thực thi
- **Khi nào dùng**: UPDATE operations
- **Ví dụ**: 
  ```java
  @CachePut(value = "sach", key = "#sach.id")
  public Sach update(Sach sach) { ... }
  ```

### @CacheEvict
- **Mục đích**: Xóa cache
- **Khi nào dùng**: DELETE operations hoặc khi data thay đổi
- **Ví dụ**: 
  ```java
  @CacheEvict(value = "sach", key = "#id")
  public void delete(int id) { ... }
  
  @CacheEvict(value = "sach", allEntries = true)
  public void deleteAll() { ... }
  ```

### @Caching
- **Mục đích**: Kết hợp nhiều cache operations
- **Ví dụ**: 
  ```java
  @Caching(evict = {
      @CacheEvict(value = "sach", allEntries = true),
      @CacheEvict(value = "theloai", allEntries = true)
  })
  public void updateMultiple() { ... }
  ```

## Kiểm Tra Cache

### 1. Kiểm tra Redis CLI:
```bash
# Connect to Redis
redis-cli

# List all keys
KEYS *

# Get a specific cache value
GET "sach::1"

# Delete a cache
DEL "sach::1"

# Flush all cache
FLUSHALL
```

### 2. Monitor Redis operations:
```bash
redis-cli MONITOR
```

## Best Practices

### 1. Cache Key Strategy
- Sử dụng key có ý nghĩa: `"sach::" + id`
- Bao gồm params trong key: `userId + '_' + page`

### 2. TTL Strategy
- Data ít thay đổi: TTL dài (vd: 2 giờ cho thể loại)
- Data thường xuyên thay đổi: TTL ngắn (vd: 5 phút cho notifications)

### 3. Cache Invalidation
- Xóa cache khi data thay đổi (CREATE/UPDATE/DELETE)
- Sử dụng `allEntries = true` khi ảnh hưởng nhiều records

### 4. Monitoring
- Theo dõi hit/miss ratio
- Kiểm tra memory usage
- Optimize TTL dựa trên usage pattern

## Troubleshooting

### Redis không kết nối được:
1. Kiểm tra Redis đã chạy: `redis-cli ping`
2. Check port 6379 đã mở chưa
3. Verify configuration trong application.properties

### Cache không work:
1. Verify `@EnableCaching` trong RedisConfig
2. Kiểm tra annotations đã đúng chưa
3. Check Redis logs: `redis-cli INFO`

### Memory issues:
```bash
# Check memory usage
redis-cli INFO memory

# Set max memory
redis-cli CONFIG SET maxmemory 256mb
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

## Mở Rộng

### 1. Cache cho Repository Layer
```java
@Repository
public interface SachRepository extends JpaRepository<Sach, Integer> {
    @Cacheable(value = "sach", key = "#id")
    Optional<Sach> findById(Integer id);
    
    @Cacheable(value = "sach")
    List<Sach> findAll();
}
```

### 2. Cache cho Complex Queries
```java
@Cacheable(value = "sachByTheLoai", key = "#theLoaiId + '_' + #page")
public Page<Sach> findByTheLoai(int theLoaiId, Pageable page) { ... }
```

### 3. Conditional Caching
```java
@Cacheable(value = "sach", key = "#id", condition = "#id > 0")
public Sach findById(int id) { ... }

@CacheEvict(value = "sach", key = "#id", beforeInvocation = true)
public void delete(int id) { ... }
```

## Build và Chạy Ứng Dụng

```bash
# 1. Start Redis
docker run --name redis-cache -p 6379:6379 -d redis:latest

# 2. Build project
mvn clean install

# 3. Run application
mvn spring-boot:run

# 4. Test cache working
# Gọi API lần 1 (chậm - từ DB)
# Gọi API lần 2 (nhanh - từ cache)
```

## Monitoring Tools

### Redis Commander (Web UI)
```bash
npm install -g redis-commander
redis-commander
# Open: http://localhost:8081
```

### RedisInsight (Official GUI)
- Download: https://redis.com/redis-enterprise/redis-insight/
