# Backend WebSocket Implementation Requirements

## 📋 Tổng quan
Backend cần implement WebSocket server sử dụng **STOMP protocol** để hỗ trợ real-time chat giữa users và sellers.

## 🛠️ Các thành phần cần implement

### 1. WebSocket Configuration
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker destinations
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 2. Chat Controller
```java
@Controller
public class ChatController {
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload RealtimeChatMessage message, 
                           SimpMessageHeaderAccessor headerAccessor) {
        // 1. Validate message
        // 2. Save to database
        // 3. Send to receiver's queue
        messagingTemplate.convertAndSendToUser(
            String.valueOf(message.getReceiverId()),
            "/queue/messages",
            message
        );
    }
    
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload TypingIndicator typing,
                                    SimpMessageHeaderAccessor headerAccessor) {
        // Send typing indicator to conversation participants
        messagingTemplate.convertAndSend(
            "/user/" + getOtherUserId(typing.getConversationId(), typing.getUserId()) + "/queue/typing",
            typing
        );
    }
    
    @MessageMapping("/chat.markReadUntil")
    public void markReadUntil(@Payload ReadUntilRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        // 1. Validate request
        // 2. Update all messages status up to readUntilMessageId
        // 3. Notify sender about read status
        chatService.markMessagesAsReadUntil(
            request.getConversationId(),
            request.getReadUntilMessageId(),
            request.getReaderId()
        );
    }
    
    @MessageMapping("/chat.createConversation")
    public void createConversation(@Payload CreateConversationRequest request,
                                 SimpMessageHeaderAccessor headerAccessor) {
        // 1. Create conversation if not exists
        // 2. Notify both users about new conversation
        ConversationModel conversation = chatService.createConversation(
            request.getUserId(),
            request.getSellerId()
        );
        
        // Notify both participants
        messagingTemplate.convertAndSendToUser(
            String.valueOf(request.getUserId()),
            "/queue/conversations",
            conversation
        );
        messagingTemplate.convertAndSendToUser(
            String.valueOf(request.getSellerId()),
            "/queue/conversations",
            conversation
        );
    }
}
```

### 3. MongoDB Document Classes

#### MessageDocument
```java
@Document(collection = "chat_messages")
public class MessageDocument {
    @Id
    private String id; // MongoDB ObjectId
    
    @Indexed(unique = true)
    private String messageId; // UUID for client reference
    
    @Indexed
    private String conversationId;
    
    @Indexed
    private Integer senderId;
    
    @Indexed
    private Integer receiverId;
    
    private String senderName;
    private String content;
    private String messageType; // text, image, file
    
    @Indexed
    private String status; // sending, sent, delivered, read, failed
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors, getters, setters
}
```

#### ConversationDocument
```java
@Document(collection = "conversations")
public class ConversationDocument {
    @Id
    private String id; // MongoDB ObjectId
    
    @Indexed(unique = true)
    private String conversationId; // UUID for client reference
    
    @Indexed
    private Integer userId;
    
    @Indexed
    private Integer sellerId;
    
    private String userName;
    private String sellerName;
    private String lastMessage;
    
    @Indexed
    private LocalDateTime lastMessageTime;
    
    private List<ParticipantDocument> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors, getters, setters
}
```

#### ParticipantDocument (Embedded in ConversationDocument)
```java
public class ParticipantDocument {
    @Indexed
    private Integer userId;
    private Integer unreadCount;
    private String lastReadMessageId;
    private LocalDateTime lastReadAt;
    
    public ParticipantDocument(Integer userId, Integer unreadCount, String lastReadMessageId, LocalDateTime lastReadAt) {
        this.userId = userId;
        this.unreadCount = unreadCount;
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = lastReadAt;
    }
    
    // Getters, setters
}
```

#### ReadUntilRequest
```java
public class ReadUntilRequest {
    private String conversationId;
    private Integer readerId;
    private String readUntilMessageId;
    private LocalDateTime timestamp;
    
    // Getters, setters, constructors
}
```

#### ConversationModel
```java
public class ConversationModel {
    private String conversationId;
    private Integer userId;
    private Integer sellerId;
    private String userName;
    private String sellerName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private LocalDateTime createdAt;
    
    // Getters, setters, constructors
}
```

### 4. Service Layer (MongoDB)
```java
@Service
public class ChatService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    public ConversationModel createConversation(Integer userId, Integer sellerId) {
        // 1. Check if conversation already exists
        Query query = new Query(Criteria.where("userId").is(userId)
                                      .and("sellerId").is(sellerId));
        ConversationDocument existing = mongoTemplate.findOne(query, ConversationDocument.class, "conversations");
        
        if (existing != null) {
            return mapToConversationModel(existing);
        }
        
        // 2. Create new conversation
        ConversationDocument conversation = new ConversationDocument();
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setSellerId(sellerId);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        
        // Initialize participants
        List<ParticipantDocument> participants = Arrays.asList(
            new ParticipantDocument(userId, 0, null, null),
            new ParticipantDocument(sellerId, 0, null, null)
        );
        conversation.setParticipants(participants);
        
        mongoTemplate.save(conversation, "conversations");
        return mapToConversationModel(conversation);
    }
    
    public RealtimeChatMessage saveMessage(RealtimeChatMessage message) {
        // 1. Generate messageId if null
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        
        // 2. Fill senderName from user service
        String senderName = userService.getUserNameById(message.getSenderId());
        message.setSenderName(senderName);
        
        // 3. Create document and save to MongoDB
        MessageDocument doc = new MessageDocument();
        doc.setMessageId(message.getMessageId());
        doc.setConversationId(message.getConversationId());
        doc.setSenderId(message.getSenderId());
        doc.setReceiverId(message.getReceiverId());
        doc.setSenderName(senderName);
        doc.setContent(message.getContent());
        doc.setMessageType(message.getType());
        doc.setStatus(message.getStatus());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        
        mongoTemplate.save(doc, "chat_messages");
        
        // 4. Update conversation last message
        updateConversationLastMessage(message.getConversationId(), message.getContent(), LocalDateTime.now());
        
        return message;
    }
    
    public void markMessagesAsReadUntil(String conversationId, String readUntilMessageId, Integer readerId) {
        // 1. Find the readUntil message to get its timestamp
        Query messageQuery = new Query(Criteria.where("messageId").is(readUntilMessageId));
        MessageDocument readUntilMessage = mongoTemplate.findOne(messageQuery, MessageDocument.class, "chat_messages");
        
        if (readUntilMessage == null) return;
        
        // 2. Update all messages in conversation up to that timestamp
        Query updateQuery = new Query(Criteria.where("conversationId").is(conversationId)
                                            .and("createdAt").lte(readUntilMessage.getCreatedAt())
                                            .and("receiverId").is(readerId)
                                            .and("status").ne("read"));
        
        Update update = new Update().set("status", "read")
                                   .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateMulti(updateQuery, update, "chat_messages");
        
        // 3. Update participant's unread count and last read info
        updateParticipantReadStatus(conversationId, readerId, readUntilMessageId, LocalDateTime.now());
    }
    
    public List<ConversationModel> getConversationsForUser(Integer userId) {
        // Query conversations where user is participant
        Query query = new Query(Criteria.where("participants.userId").is(userId));
        query.with(Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        
        List<ConversationDocument> docs = mongoTemplate.find(query, ConversationDocument.class, "conversations");
        return docs.stream()
                   .map(this::mapToConversationModel)
                   .collect(Collectors.toList());
    }
    
    public List<RealtimeChatMessage> getMessagesForConversation(String conversationId, Integer userId, int page, int size) {
        // 1. Validate user has access to conversation
        if (!hasAccessToConversation(conversationId, userId)) {
            throw new UnauthorizedException("User does not have access to this conversation");
        }
        
        // 2. Query messages with pagination
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.skip(page * size).limit(size);
        
        List<MessageDocument> docs = mongoTemplate.find(query, MessageDocument.class, "chat_messages");
        
        return docs.stream()
                   .map(this::mapToRealtimeChatMessage)
                   .collect(Collectors.toList());
    }
    
    private void updateConversationLastMessage(String conversationId, String lastMessage, LocalDateTime timestamp) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        Update update = new Update().set("lastMessage", lastMessage)
                                   .set("lastMessageTime", timestamp)
                                   .set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, "conversations");
    }
    
    private void updateParticipantReadStatus(String conversationId, Integer userId, String lastReadMessageId, LocalDateTime readAt) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                                      .and("participants.userId").is(userId));
        Update update = new Update()
                .set("participants.$.lastReadMessageId", lastReadMessageId)
                .set("participants.$.lastReadAt", readAt)
                .set("participants.$.unreadCount", 0)
                .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, "conversations");
    }
}
```

### 5. WebSocket Event Handlers
```java
@Component
public class WebSocketEventListener {
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        // User connected - log connection
        String userId = getUserIdFromSession(event);
        logger.info("User {} connected", userId);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // User disconnected - cleanup if needed
        String userId = getUserIdFromSession(event);
        logger.info("User {} disconnected", userId);
    }
    
    private String getUserIdFromSession(AbstractSubProtocolEvent event) {
        // Extract userId from query parameters
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        return (String) sessionAttributes.get("userId");
    }
}
```

### 6. Authentication Interceptor
```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 1. Extract JWT token from headers
            // 2. Validate token
            // 3. Set user principal
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                // Validate and set user
                accessor.setUser(getUserFromToken(token));
            }
        }
        
        return message;
    }
}
```

## 🔄 Message Flow

### 1. Kết nối WebSocket
```
Client -> GET /ws?userId=123 (with Authorization header)
Server -> Validate JWT token
Server -> Store user session
Client -> Subscribe to:
  - /user/{userId}/queue/messages
  - /user/{userId}/queue/typing
  - /user/{userId}/queue/conversations
```

### 2. Gửi tin nhắn
```
Client -> Send to /app/chat.sendMessage
Server -> Validate & save message
Server -> Send to /user/{receiverId}/queue/messages
Server -> Update conversation last message
```

### 3. Typing indicator
```
Client -> Send to /app/chat.typing
Server -> Forward to other user: /user/{otherUserId}/queue/typing
```

### 4. Mark messages as read (Performance optimized)
```
Client -> Send to /app/chat.markReadUntil
  {
    "conversationId": "conv-123",
    "readerId": 456,
    "readUntilMessageId": "msg-789"
  }
Server -> Update all messages up to msg-789 as read
Server -> Notify sender about read status
```

## 🗄️ MongoDB Schema Design

### Messages Collection
```javascript
// chat_messages collection
{
  _id: ObjectId("..."), // MongoDB auto-generated
  messageId: "msg-uuid-123", // UUID for client reference
  conversationId: "conv-uuid-456", // UUID for conversation
  senderId: 123, // Integer user ID
  receiverId: 456, // Integer user ID
  senderName: "John Doe",
  content: "Hello there!",
  messageType: "text", // text, image, file
  status: "sent", // sending, sent, delivered, read, failed
  createdAt: ISODate("2025-12-08T10:30:00Z"),
  updatedAt: ISODate("2025-12-08T10:30:00Z")
}

// Indexes for performance
db.chat_messages.createIndex({ "conversationId": 1, "createdAt": -1 })
db.chat_messages.createIndex({ "senderId": 1 })
db.chat_messages.createIndex({ "receiverId": 1 })
db.chat_messages.createIndex({ "messageId": 1 }, { unique: true })
db.chat_messages.createIndex({ "status": 1 })
```

### Conversations Collection
```javascript
// conversations collection
{
  _id: ObjectId("..."),
  conversationId: "conv-uuid-456", // UUID for client reference
  userId: 123,
  sellerId: 456,
  userName: "John Doe",
  sellerName: "BookStore Inc",
  lastMessage: "Hello there!",
  lastMessageTime: ISODate("2025-12-08T10:30:00Z"),
  participants: [
    {
      userId: 123,
      unreadCount: 0,
      lastReadMessageId: "msg-uuid-123",
      lastReadAt: ISODate("2025-12-08T10:30:00Z")
    },
    {
      userId: 456,
      unreadCount: 2,
      lastReadMessageId: "msg-uuid-120",
      lastReadAt: ISODate("2025-12-08T10:25:00Z")
    }
  ],
  createdAt: ISODate("2025-12-08T10:00:00Z"),
  updatedAt: ISODate("2025-12-08T10:30:00Z")
}

// Indexes for performance
db.conversations.createIndex({ "conversationId": 1 }, { unique: true })
db.conversations.createIndex({ "userId": 1 })
db.conversations.createIndex({ "sellerId": 1 })
db.conversations.createIndex({ "participants.userId": 1 })
db.conversations.createIndex({ "lastMessageTime": -1 })
```

## 📝 API Endpoints (REST fallback)

### GET /api/conversations
```java
@GetMapping("/conversations")
public ResponseEntity<List<ConversationModel>> getConversations(
    @RequestHeader("Authorization") String token) {
    // Return user's conversations
}
```

### GET /api/conversations/{conversationId}/messages
```java
@GetMapping("/conversations/{conversationId}/messages")
public ResponseEntity<List<RealtimeChatMessage>> getMessages(
    @PathVariable String conversationId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "50") int size) {
    // Return paginated messages
}
```

### POST /api/conversations/{conversationId}/messages
```java
@PostMapping("/conversations/{conversationId}/messages")
public ResponseEntity<RealtimeChatMessage> sendMessage(
    @PathVariable String conversationId,
    @RequestBody SendMessageRequest request) {
    // Fallback for when WebSocket is not available
}
```

## 📦 Dependencies (Maven)
```xml
<dependencies>
    <!-- Spring Boot WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- MongoDB -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- Security for JWT -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt</artifactId>
        <version>0.9.1</version>
    </dependency>
    
    <!-- Jackson for JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

## 🚀 Deployment Notes (MongoDB)

1. **MongoDB Setup**:
   - Install MongoDB 5.0+ for optimal performance
   - Configure replica set for production
   - Set up proper authentication and authorization

2. **Indexing Strategy**:
   - Compound index on (conversationId, createdAt) for message queries
   - Index on messageId for quick lookups
   - Index on participants.userId for conversation queries

3. **Scaling Considerations**:
   - MongoDB sharding by conversationId for horizontal scaling
   - Consider message archiving strategy (move old messages to archive collection)
   - Implement caching layer (Redis) for frequently accessed conversations

4. **Performance Optimization**:
   - Use projection to limit returned fields
   - Implement pagination for message history
   - Consider using MongoDB GridFS for file attachments

5. **Backup Strategy**:
   - Daily automated backups with point-in-time recovery
   - Test restore procedures regularly

## 🔧 Configuration Properties (MongoDB)
```properties
# application.properties
websocket.max-sessions-per-user=5
websocket.heartbeat.interval=30000
websocket.message.max-size=10240
websocket.typing.debounce=2000

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/bookstore_chat
spring.data.mongodb.database=bookstore_chat
spring.data.mongodb.repositories.enabled=true

# MongoDB Connection Pool
spring.data.mongodb.options.connections-per-host=10
spring.data.mongodb.options.min-connections-per-host=5
spring.data.mongodb.options.max-wait-time=120000
spring.data.mongodb.options.max-connection-idle-time=0
spring.data.mongodb.options.max-connection-life-time=0
```

## 🏗️ MongoDB Configuration Class
```java
@Configuration
@EnableMongoRepositories
public class MongoConfig {
    
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), "bookstore_chat");
    }
    
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Create indexes on startup
        createIndexes();
    }
    
    private void createIndexes() {
        MongoTemplate mongoTemplate = mongoTemplate();
        
        // Messages collection indexes
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("conversationId", Sort.Direction.ASC)
                                           .on("createdAt", Sort.Direction.DESC));
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("messageId", Sort.Direction.ASC).unique());
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("senderId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("receiverId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("chat_messages")
                     .ensureIndex(new Index().on("status", Sort.Direction.ASC));
        
        // Conversations collection indexes
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("conversationId", Sort.Direction.ASC).unique());
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("userId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("sellerId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("participants.userId", Sort.Direction.ASC));
        
        mongoTemplate.indexOps("conversations")
                     .ensureIndex(new Index().on("lastMessageTime", Sort.Direction.DESC));
    }
}
```

## ✅ Testing Checklist

- [ ] User can connect/disconnect WebSocket
- [ ] Messages are delivered in real-time
- [ ] Typing indicators work correctly
- [ ] Read receipts update properly with readUntil optimization
- [ ] Conversation creation works
- [ ] Authentication is enforced
- [ ] Error handling for network issues
- [ ] Performance under load (100+ concurrent users)
- [ ] Database queries are optimized
- [ ] Memory leaks are prevented

## 🔄 Migration Plan

1. **Phase 1**: Implement basic WebSocket with message sending
2. **Phase 2**: Add typing indicators and read receipts
3. **Phase 3**: Add conversation management
4. **Phase 4**: Performance optimization and monitoring
5. **Phase 5**: Load testing and production deployment

---

**Lưu ý quan trọng**: 
- Sử dụng `markReadUntil` thay vì `markAsRead` từng message để tối ưu performance
- Username được gửi từ client, server cần validate và có thể override
- Implement proper error handling và reconnection logic
- Consider message persistence và offline message delivery