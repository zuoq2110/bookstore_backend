# Hướng dẫn Test Chat API với JWT

## Bước 1: Đăng nhập để lấy JWT Token

**POST** `http://localhost:8080/tai-khoan/dang-nhap`

Headers:
```
Content-Type: application/json
```

Body:
```json
{
    "tenDangNhap": "your_username",
    "matKhau": "your_password"
}
```

Response:
```json
{
    "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1
}
```

**Lưu JWT token để dùng cho các API tiếp theo!**

---

## Bước 2: Tạo hội thoại mới

**POST** `http://localhost:8080/api/chat/conversations`

Headers:
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Body:
```json
{
    "sellerId": 50,
    "userName": "Test User", 
    "sellerName": "Test Seller"
}
```

Response:
```json
{
    "id": "conversation_id_123",
    "participants": [...],
    "lastMessage": null,
    "createdAt": "2025-12-09T10:00:00"
}
```

**Lưu conversation ID để dùng cho các API tiếp theo!**

---

## Bước 3: Lấy danh sách hội thoại

**GET** `http://localhost:8080/api/chat/conversations`

Headers:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Response:
```json
[
    {
        "id": "conversation_id_123",
        "participants": [...],
        "lastMessage": {...},
        "createdAt": "2025-12-09T10:00:00"
    }
]
```

---

## Bước 4: Gửi tin nhắn

**POST** `http://localhost:8080/api/chat/conversations/{conversationId}/messages`

Headers:
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Body:
```json
{
    "content": "Hello, this is a test message",
    "messageType": "text",
    "receiverId": 50
}
```

Response:
```json
{
    "id": "message_id_456",
    "conversationId": "conversation_id_123",
    "senderId": 1,
    "receiverId": 50,
    "content": "Hello, this is a test message",
    "type": "text",
    "status": "sent",
    "createdAt": "2025-12-09T10:01:00"
}
```

---

## Bước 5: Lấy tin nhắn trong hội thoại

**GET** `http://localhost:8080/api/chat/conversations/{conversationId}/messages?page=0&size=50`

Headers:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Response:
```json
[
    {
        "id": "message_id_456",
        "conversationId": "conversation_id_123",
        "senderId": 1,
        "receiverId": 50,
        "content": "Hello, this is a test message",
        "type": "text",
        "status": "sent",
        "createdAt": "2025-12-09T10:01:00"
    }
]
```

---

## Bước 6: Đánh dấu tin nhắn đã đọc

**POST** `http://localhost:8080/api/chat/conversations/{conversationId}/read`

Headers:
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Body:
```json
{
    "messageId": "message_id_456"
}
```

Response:
```json
{
    "success": true,
    "message": "Messages marked as read"
}
```

---

## Debug Tips:

1. **Kiểm tra JWT Token**: Đảm bảo copy đúng toàn bộ token từ response login
2. **Authorization Header**: Phải có "Bearer " (có dấu cách) trước token
3. **Token hết hạn**: Nếu bị 401, thử đăng nhập lại để lấy token mới
4. **Console Log**: Check console Spring Boot để xem debug log nếu có lỗi
5. **Conversation ID**: Thay `{conversationId}` bằng ID thực tế từ response tạo conversation

## Postman Collection:

Có thể import các request này vào Postman và tạo environment variable cho JWT token để dễ dàng test.

Variables:
- `baseUrl`: http://localhost:8080
- `jwtToken`: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
- `conversationId`: conversation_id_123