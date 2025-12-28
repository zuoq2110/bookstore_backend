# Tóm Tắt Quy Trình Mã Hóa Tin Nhắn

## 📋 Tổng Quan Hệ Thống

Hệ thống chat sử dụng **mã hóa AES-GCM 256-bit** để bảo vệ toàn bộ nội dung tin nhắn trong database MongoDB. Mọi tin nhắn được mã hóa tự động trước khi lưu trữ và giải mã tự động khi truy xuất.

### 🔐 Thuật Toán Mã Hóa
- **Algoritm**: AES-GCM (Galois/Counter Mode)
- **Key Size**: 256 bit
- **IV Length**: 12 bytes (96 bits)
- **Authentication Tag**: 16 bytes (128 bits)
- **Encoding**: Base64

---

## 🏗️ Kiến Trúc Hệ Thống

### Core Components

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   Client Frontend   │    │   Spring Backend    │    │     MongoDB         │
├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤
│ • WebSocket Client  │◄──►│ • ChatController    │    │ • chat_messages     │
│ • Message UI        │    │ • ChatService       │    │ • conversations     │
│ • Real-time Updates │    │ • EncryptionUtil    │◄──►│ • Encrypted Content │
│ • JWT Auth          │    │ • Migration Service │    │ • Indexes           │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
```

### 🔑 MessageEncryptionUtil - Core Engine

```java
@Component
public class MessageEncryptionUtil {
    // Constants
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${app.chat.encryption.key:#{null}}")
    private String encryptionKeyBase64;
}
```

---

## 🔐 Quy Trình Mã Hóa Chi Tiết

### 1. Khởi Tạo Khóa Mã Hóa

#### Tạo Khóa Mới
```java
// Method 1: Sử dụng EncryptionKeyGenerator
public static void main(String[] args) {
    String key = MessageEncryptionUtil.generateNewKey();
    System.out.println("Generated Key: " + key);
}

// Method 2: Tạo khóa trong application
String newKey = MessageEncryptionUtil.generateNewKey();
```

#### Cấu Hình Khóa
```properties
# application.properties
app.chat.encryption.key=YOUR_BASE64_ENCODED_256_BIT_KEY
```

### 2. Quy Trình Encrypt

```java
public String encrypt(String plainText) {
    // 1. Kiểm tra input
    if (plainText == null || plainText.isEmpty()) {
        return plainText;
    }
    
    // 2. Lấy khóa mã hóa
    SecretKey key = getEncryptionKey();
    
    // 3. Tạo IV ngẫu nhiên (12 bytes)
    byte[] iv = new byte[GCM_IV_LENGTH];
    new SecureRandom().nextBytes(iv);
    
    // 4. Khởi tạo Cipher với AES-GCM
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
    
    // 5. Mã hóa dữ liệu
    byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    
    // 6. Kết hợp IV + Encrypted Data
    byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
    System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
    System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
    
    // 7. Encode Base64 để lưu trữ
    return Base64.getEncoder().encodeToString(encryptedWithIv);
}
```

### 3. Quy Trình Decrypt

```java
public String decrypt(String encryptedText) {
    // 1. Kiểm tra input
    if (encryptedText == null || encryptedText.isEmpty()) {
        return encryptedText;
    }
    
    // 2. Lấy khóa mã hóa
    SecretKey key = getEncryptionKey();
    
    // 3. Decode Base64
    byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
    
    // 4. Tách IV và dữ liệu đã mã hóa
    byte[] iv = new byte[GCM_IV_LENGTH];
    System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
    
    byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
    System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
    
    // 5. Khởi tạo Cipher để giải mã
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
    
    // 6. Giải mã dữ liệu
    byte[] decryptedData = cipher.doFinal(encryptedData);
    
    return new String(decryptedData, StandardCharsets.UTF_8);
}
```

---

## 📱 Luồng Gửi Tin Nhắn

### 1. Client Gửi Tin Nhắn

```javascript
// Frontend WebSocket
const message = {
    conversationId: "conv-uuid-123",
    content: "Hello World!",
    receiverId: 456,
    type: "text"
};

stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(message));
```

### 2. Backend Xử Lý

```java
@MessageMapping("/chat.sendMessage")
public void sendMessage(@Payload RealtimeChatMessage message) {
    // 1. Validate message
    if (message.getContent() == null || message.getContent().trim().isEmpty()) {
        return;
    }
    
    // 2. Save to database (tự động mã hóa)
    RealtimeChatMessage savedMessage = chatService.saveMessage(message);
    
    // 3. Send to receiver
    messagingTemplate.convertAndSendToUser(
        String.valueOf(message.getReceiverId()),
        "/queue/messages",
        savedMessage  // Message đã được giải mã để gửi
    );
}
```

### 3. ChatService - Mã Hóa Tự Động

```java
public RealtimeChatMessage saveMessage(RealtimeChatMessage message) {
    // 1. Generate messageId
    if (message.getMessageId() == null) {
        message.setMessageId(UUID.randomUUID().toString());
    }
    
    // 2. Create MongoDB document
    MessageDocument doc = new MessageDocument();
    doc.setMessageId(message.getMessageId());
    doc.setConversationId(message.getConversationId());
    
    // 3. MÃ HÓA TIN NHẮN TRƯỚC KHI LƯU
    String encryptedContent = encryptionUtil.encrypt(message.getContent());
    doc.setContent(encryptedContent);
    
    // 4. Save to MongoDB
    mongoTemplate.save(doc, "chat_messages");
    
    // 5. Update conversation last message (cũng được mã hóa)
    updateConversationLastMessage(message.getConversationId(), message.getContent(), LocalDateTime.now());
    
    return message; // Trả về message gốc (chưa mã hóa) cho client
}
```

### 4. Cập Nhật Last Message

```java
private void updateConversationLastMessage(String conversationId, String lastMessage, LocalDateTime timestamp) {
    Query query = new Query(Criteria.where("conversationId").is(conversationId));
    
    // MÃ HÓA LAST MESSAGE
    String encryptedLastMessage = encryptionUtil.encrypt(lastMessage);
    
    Update update = new Update()
        .set("lastMessage", encryptedLastMessage)
        .set("lastMessageTime", timestamp);
    mongoTemplate.updateFirst(query, update, "conversations");
}
```

---

## 📥 Luồng Đọc Tin Nhắn

### 1. Client Request Tin Nhắn

```javascript
// REST API call
fetch('/api/chat/messages/{conversationId}')
    .then(response => response.json())
    .then(messages => {
        // Messages đã được tự động giải mã
        displayMessages(messages);
    });
```

### 2. Backend Giải Mã Tự Động

```java
public List<RealtimeChatMessage> getMessagesForConversation(String conversationId, Integer userId, int page, int size) {
    // 1. Query MongoDB
    Query query = new Query(Criteria.where("conversationId").is(conversationId))
        .with(Sort.by(Sort.Direction.DESC, "createdAt"))
        .skip(page * size)
        .limit(size);
    
    List<MessageDocument> documents = mongoTemplate.find(query, MessageDocument.class, "chat_messages");
    
    // 2. Map và TỰ ĐỘNG GIẢI MÃ
    return documents.stream()
        .map(this::mapToRealtimeChatMessage)  // Giải mã trong method này
        .collect(Collectors.toList());
}
```

### 3. Mapping với Giải Mã

```java
private RealtimeChatMessage mapToRealtimeChatMessage(MessageDocument doc) {
    RealtimeChatMessage message = new RealtimeChatMessage();
    message.setMessageId(doc.getMessageId());
    message.setConversationId(doc.getConversationId());
    
    // TỰ ĐỘNG GIẢI MÃ CONTENT
    String decryptedContent = null;
    if (doc.getContent() != null && !doc.getContent().isEmpty()) {
        try {
            decryptedContent = encryptionUtil.decrypt(doc.getContent());
        } catch (Exception e) {
            // Fallback cho tin nhắn cũ chưa được mã hóa
            decryptedContent = doc.getContent();
        }
    }
    message.setContent(decryptedContent);
    
    return message;
}
```

---

## 🗄️ Cấu Trúc Database

### Messages Collection (Encrypted)

```javascript
{
  "_id": ObjectId("675fc2e4e8b7a12345678901"),
  "messageId": "msg-uuid-12345",
  "conversationId": "conv-uuid-67890",
  "senderId": 123,
  "receiverId": 456,
  "senderName": "John Doe",
  "content": "j8fk2Jd9xLm3pQ7W8nRs1vBcH4yT6gE9...", // ĐÃ MÃ HÓA
  "messageType": "text",
  "status": "sent",
  "createdAt": ISODate("2024-12-15T10:30:00Z"),
  "updatedAt": ISODate("2024-12-15T10:30:00Z")
}
```

### Conversations Collection (Encrypted Last Message)

```javascript
{
  "_id": ObjectId("675fc2e4e8b7a12345678902"),
  "conversationId": "conv-uuid-67890",
  "userId": 123,
  "sellerId": 456,
  "userName": "John Doe",
  "sellerName": "Shop ABC",
  "lastMessage": "m9Hk3Nd8yLp4qR8X9oSt2wCdI5zU7hF0...", // ĐÃ MÃ HÓA
  "lastMessageTime": ISODate("2024-12-15T10:30:00Z"),
  "participants": [
    {
      "userId": 123,
      "unreadCount": 0
    },
    {
      "userId": 456, 
      "unreadCount": 1
    }
  ],
  "createdAt": ISODate("2024-12-15T09:00:00Z"),
  "updatedAt": ISODate("2024-12-15T10:30:00Z")
}
```

---

## 🔄 Migration Dữ Liệu Cũ

### 1. Kiểm Tra Trạng Thái

```bash
# Admin API
GET /api/admin/chat-encryption/status

# Response
{
  "success": true,
  "unencryptedMessages": 150,
  "totalMessages": 1500,
  "encryptionPercentage": 90
}
```

### 2. Migration Service

```java
@Service
public class ChatEncryptionMigrationService {
    
    public void migrateAllMessages() {
        // Migrate chat messages
        migrateChatMessages();
        
        // Migrate conversation last messages  
        migrateConversationLastMessages();
    }
    
    private void migrateChatMessages() {
        List<MessageDocument> messages = mongoTemplate.findAll(MessageDocument.class, "chat_messages");
        
        for (MessageDocument message : messages) {
            try {
                // Test if already encrypted
                encryptionUtil.decrypt(message.getContent());
                continue; // Already encrypted
            } catch (Exception e) {
                // Not encrypted - need to encrypt
                String encryptedContent = encryptionUtil.encrypt(message.getContent());
                
                Query query = new Query(Criteria.where("messageId").is(message.getMessageId()));
                Update update = new Update().set("content", encryptedContent);
                mongoTemplate.updateFirst(query, update, "chat_messages");
            }
        }
    }
}
```

### 3. Chạy Migration

```bash
# Admin API
POST /api/admin/chat-encryption/migrate

# Response
{
  "success": true,
  "message": "Message encryption migration completed successfully"
}
```

---

## 🔒 Bảo Mật và Best Practices

### 1. Quản Lý Khóa

```bash
# Production Environment Variables
export CHAT_ENCRYPTION_KEY="your_base64_encoded_256_bit_key"

# application.properties
app.chat.encryption.key=${CHAT_ENCRYPTION_KEY}
```

**⚠️ LƯU Ý QUAN TRỌNG:**
- Không bao giờ commit khóa vào source code
- Lưu khóa trong secret management system
- Backup khóa an toàn - mất khóa = mất tất cả data
- Rotation khóa định kỳ (nâng cao)

### 2. Backward Compatibility

```java
// Hệ thống tự động xử lý tin nhắn cũ chưa mã hóa
try {
    decryptedContent = encryptionUtil.decrypt(doc.getContent());
} catch (Exception e) {
    // Fallback cho tin nhắn cũ
    decryptedContent = doc.getContent();
    
    // Log để tracking migration progress
    logger.info("Found unencrypted message: " + doc.getMessageId());
}
```

### 3. Error Handling

```java
// Encryption Error Handling
public String encrypt(String plainText) {
    try {
        // Encryption logic
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    } catch (Exception e) {
        logger.error("Failed to encrypt message", e);
        throw new RuntimeException("Encryption failed", e);
    }
}
```

### 4. Performance Considerations

- IV được generate random cho mỗi message → không thể deduplicate
- Encryption/Decryption overhead minimal với AES-GCM
- Index trên encrypted field không hoạt động → search cần decrypt
- Connection pool tối ưu cho MongoDB operations

---

## 🧪 Testing và Validation

### 1. Unit Tests

```java
@Test
public void testEncryptionDecryption() {
    String original = "Hello World! 🌍";
    String encrypted = encryptionUtil.encrypt(original);
    String decrypted = encryptionUtil.decrypt(encrypted);
    
    // Assertions
    assertEquals(original, decrypted);
    assertNotEquals(original, encrypted);
    assertTrue(encrypted.length() > original.length());
    
    // Base64 validation
    assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
}

@Test 
public void testNullAndEmptyHandling() {
    assertNull(encryptionUtil.encrypt(null));
    assertEquals("", encryptionUtil.encrypt(""));
}
```

### 2. Integration Tests

```java
@Test
public void testMessageEncryptionFlow() {
    // 1. Save message
    RealtimeChatMessage message = new RealtimeChatMessage();
    message.setContent("Test message");
    RealtimeChatMessage saved = chatService.saveMessage(message);
    
    // 2. Check database has encrypted content
    MessageDocument doc = mongoTemplate.findOne(
        new Query(Criteria.where("messageId").is(saved.getMessageId())),
        MessageDocument.class, 
        "chat_messages"
    );
    
    assertNotEquals("Test message", doc.getContent());
    assertTrue(doc.getContent().length() > "Test message".length());
    
    // 3. Retrieve and verify automatic decryption
    List<RealtimeChatMessage> messages = chatService.getMessagesForConversation(
        saved.getConversationId(), saved.getSenderId(), 0, 10
    );
    
    assertEquals("Test message", messages.get(0).getContent());
}
```

---

## 📊 Monitoring và Logging

### 1. Key Metrics

```java
// Metrics to track
- Encryption success rate
- Decryption failures (potential corruption)
- Migration progress
- Performance impact

// Logging examples
logger.info("Message encrypted successfully: messageId={}", messageId);
logger.warn("Decryption failed for message: {}, fallback to original", messageId);
logger.error("Encryption key not configured or invalid");
```

### 2. Health Checks

```java
@Component
public class EncryptionHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Test encryption/decryption
            String test = "health-check";
            String encrypted = encryptionUtil.encrypt(test);
            String decrypted = encryptionUtil.decrypt(encrypted);
            
            if (test.equals(decrypted)) {
                return Health.up()
                    .withDetail("encryption", "working")
                    .build();
            } else {
                return Health.down()
                    .withDetail("encryption", "failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("encryption", "error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## 🚀 Deployment Checklist

### Pre-Deployment
- [ ] Generate encryption key
- [ ] Configure environment variables
- [ ] Test encryption/decryption in staging
- [ ] Backup existing data
- [ ] Verify MongoDB indexes

### Deployment
- [ ] Deploy application với encryption enabled
- [ ] Verify health checks pass
- [ ] Test new message encryption
- [ ] Run migration for existing data
- [ ] Monitor logs for errors

### Post-Deployment
- [ ] Verify all messages encrypted in database
- [ ] Test message retrieval và display
- [ ] Monitor performance metrics
- [ ] Document key storage location
- [ ] Setup key rotation schedule (future)

---

## 🔗 Related Files

### Core Implementation
- [`MessageEncryptionUtil.java`](src/main/java/com/example/web_ban_sach/util/MessageEncryptionUtil.java) - Engine mã hóa chính
- [`EncryptionKeyGenerator.java`](src/main/java/com/example/web_ban_sach/util/EncryptionKeyGenerator.java) - Tạo khóa mã hóa
- [`ChatService.java`](src/main/java/com/example/web_ban_sach/Service/chat/ChatService.java) - Service tích hợp encryption
- [`ChatEncryptionMigrationService.java`](src/main/java/com/example/web_ban_sach/Service/chat/ChatEncryptionMigrationService.java) - Migration dữ liệu cũ

### Controllers
- [`ChatController.java`](src/main/java/com/example/web_ban_sach/controller/chat/ChatController.java) - WebSocket controller
- [`ChatRestController.java`](src/main/java/com/example/web_ban_sach/controller/chat/ChatRestController.java) - REST API
- [`ChatEncryptionController.java`](src/main/java/com/example/web_ban_sach/controller/ChatEncryptionController.java) - Admin encryption APIs

### Configuration
- [`application.properties`](src/main/resources/application.properties) - Encryption key configuration
- [`WebSocketConfig.java`](src/main/java/com/example/web_ban_sach/config/WebSocketConfig.java) - WebSocket setup

### Documentation
- [`CHAT_ENCRYPTION_SETUP.md`](CHAT_ENCRYPTION_SETUP.md) - Setup guide chi tiết
- [`BACKEND_WEBSOCKET_IMPLEMENTATION.md`](src/BACKEND_WEBSOCKET_IMPLEMENTATION.md) - WebSocket implementation

---

## ✅ Kết Luận

Hệ thống mã hóa tin nhắn đã được implement hoàn chỉnh với:

- **End-to-end security**: Tin nhắn được mã hóa trong database
- **Transparent operation**: Tự động mã hóa/giải mã không ảnh hưởng UX
- **Backward compatibility**: Hỗ trợ tin nhắn cũ chưa mã hóa
- **Migration support**: Tools để migrate dữ liệu existing
- **Production ready**: Error handling, monitoring, health checks
- **Performance optimized**: Minimal overhead với AES-GCM

Hệ thống đảm bảo privacy và security cho tất cả communication trong platform.