# Chat Message Encryption Setup Guide

## Tổng quan
Hệ thống này sử dụng mã hóa AES-GCM để bảo vệ nội dung tin nhắn chat trong database. Tất cả tin nhắn sẽ được mã hóa trước khi lưu và tự động giải mã khi đọc.

## Bước 1: Tạo khóa mã hóa

### Cách 1: Sử dụng EncryptionKeyGenerator
```bash
cd src/main/java/com/example/web_ban_sach/util
javac EncryptionKeyGenerator.java MessageEncryptionUtil.java
java com.example.web_ban_sach.util.EncryptionKeyGenerator
```

### Cách 2: Tạo khóa thủ công
```java
String key = MessageEncryptionUtil.generateNewKey();
System.out.println(key);
```

## Bước 2: Cấu hình khóa mã hóa

Thêm khóa vào `application.properties`:
```properties
# Chat message encryption key (AES-256)
app.chat.encryption.key=YOUR_GENERATED_KEY_HERE
```

**⚠️ LƯU Ý QUAN TRỌNG:**
- Lưu khóa này an toàn và bí mật
- Nếu mất khóa, tất cả tin nhắn đã mã hóa sẽ không thể khôi phục
- Không commit khóa vào git

## Bước 3: Migration dữ liệu cũ (nếu có)

Nếu bạn đã có tin nhắn cũ chưa được mã hóa, cần chạy migration:

### Kiểm tra trạng thái mã hóa:
```bash
GET /api/admin/chat-encryption/status
```

### Chạy migration:
```bash
POST /api/admin/chat-encryption/migrate
```

## Tính năng

### 1. Mã hóa tự động
- Tin nhắn được mã hóa tự động trước khi lưu vào MongoDB
- Tin nhắn cuối cùng của conversation cũng được mã hóa

### 2. Giải mã tự động
- Tin nhắn được giải mã tự động khi đọc
- Hỗ trợ backward compatibility với tin nhắn cũ chưa mã hóa

### 3. Bảo mật
- Sử dụng AES-GCM với khóa 256-bit
- Mỗi tin nhắn có IV (Initialization Vector) riêng
- Authentication tag để đảm bảo tính toàn vẹn

## API Endpoints

### Chat Service (đã được cập nhật)
- `POST /api/chat/send` - Gửi tin nhắn (tự động mã hóa)
- `GET /api/chat/conversations` - Lấy danh sách conversation (tự động giải mã)
- `GET /api/chat/messages/{conversationId}` - Lấy tin nhắn (tự động giải mã)

### Admin Endpoints
- `GET /api/admin/chat-encryption/status` - Kiểm tra trạng thái mã hóa
- `POST /api/admin/chat-encryption/migrate` - Migration dữ liệu cũ

## Cấu trúc Database

### Messages Collection
```javascript
{
  "_id": ObjectId("..."),
  "messageId": "uuid",
  "conversationId": "uuid", 
  "senderId": 123,
  "receiverId": 456,
  "senderName": "User Name",
  "content": "base64_encrypted_content", // ĐÃ MÃ HÓA
  "messageType": "text",
  "status": "sent",
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

### Conversations Collection
```javascript
{
  "_id": ObjectId("..."),
  "conversationId": "uuid",
  "userId": 123,
  "sellerId": 456,
  "userName": "User Name",
  "sellerName": "Seller Name",
  "lastMessage": "base64_encrypted_content", // ĐÃ MÃ HÓA
  "lastMessageTime": ISODate("..."),
  "participants": [...],
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

## Xử lý lỗi

### 1. Lỗi khóa không tồn tại
```
RuntimeException: Encryption key is not configured
```
**Giải pháp:** Thêm `app.chat.encryption.key` vào application.properties

### 2. Lỗi khóa không hợp lệ
```
RuntimeException: Failed to decode encryption key
```
**Giải pháp:** Tạo khóa mới bằng EncryptionKeyGenerator

### 3. Lỗi giải mã tin nhắn cũ
- Hệ thống tự động fallback về tin nhắn gốc nếu giải mã thất bại
- Không ảnh hưởng đến hoạt động của ứng dụng

## Testing

### Test mã hóa/giải mã:
```java
@Autowired
private MessageEncryptionUtil encryptionUtil;

@Test
public void testEncryptionDecryption() {
    String original = "Hello World!";
    String encrypted = encryptionUtil.encrypt(original);
    String decrypted = encryptionUtil.decrypt(encrypted);
    
    assertEquals(original, decrypted);
    assertNotEquals(original, encrypted);
}
```

## Production Deployment

1. **Tạo khóa mã hóa** trước khi deploy
2. **Lưu khóa an toàn** (environment variables, secret management)
3. **Chạy migration** sau khi deploy (nếu có dữ liệu cũ)
4. **Monitor logs** để đảm bảo mã hóa hoạt động đúng

## Environment Variables

```bash
# Production
export CHAT_ENCRYPTION_KEY="your_encryption_key_here"

# application.properties
app.chat.encryption.key=${CHAT_ENCRYPTION_KEY}
```