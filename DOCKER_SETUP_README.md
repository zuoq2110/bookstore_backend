# Docker Setup for Web Ban Sach

## Quick Start

1. **Chuẩn bị môi trường:**
   ```bash
   # Đảm bảo Docker và Docker Compose đã được cài đặt
   docker --version
   docker-compose --version
   ```

2. **Cấu hình environment variables:**
   - Cập nhật file `.env` với thông tin database, Redis, MongoDB passwords
   - Đặt các giá trị phù hợp cho production

3. **Khởi chạy tất cả services:**
   ```bash
   # Build và start tất cả containers
   docker-compose up -d --build
   
   # Hoặc sử dụng script quản lý (Windows)
   docker-manage.bat
   ```

## Services được triển khai

- **MySQL** (port 3306): Database chính cho ứng dụng
- **Redis** (port 6379): Cache và session storage  
- **MongoDB** (port 27017): Database cho chat system
- **Spring Boot App** (port 8080): Ứng dụng chính
- **Nginx** (port 80/443): Reverse proxy và load balancer

## Useful Commands

```bash
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của một service cụ thể
docker-compose logs -f web-ban-sach-app

# Kiểm tra trạng thái services
docker-compose ps

# Restart một service
docker-compose restart web-ban-sach-app

# Xem resource usage
docker stats

# Truy cập database
docker exec -it web_ban_sach_mysql mysql -u root -p

# Truy cập Redis CLI
docker exec -it web_ban_sach_redis redis-cli

# Truy cập MongoDB
docker exec -it web_ban_sach_mongodb mongosh
```

## Development Tips

1. **Hot reload**: Khi thay đổi code, rebuild Spring Boot container:
   ```bash
   docker-compose up -d --build web-ban-sach-app
   ```

2. **Database migration**: SQL files trong `Database/` sẽ tự động chạy khi khởi tạo

3. **Logs debugging**: Application logs được lưu trong volume `app_logs`

4. **Health checks**: Truy cập http://localhost:8080/actuator/health

## Production Deployment

1. Cập nhật `.env` với thông tin production
2. Uncomment HTTPS configuration trong nginx
3. Thêm SSL certificates vào `nginx/ssl/`
4. Set proper security passwords cho databases

## Troubleshooting

- **Port conflicts**: Thay đổi ports trong docker-compose.yml nếu bị conflict
- **Memory issues**: Tăng Docker memory allocation
- **Permission issues**: Chạy `docker-compose` với quyền admin nếu cần