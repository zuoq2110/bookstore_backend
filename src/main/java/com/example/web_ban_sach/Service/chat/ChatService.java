package com.example.web_ban_sach.Service.chat;

import com.example.web_ban_sach.dto.chat.ConversationModel;
import com.example.web_ban_sach.dto.chat.RealtimeChatMessage;
import com.example.web_ban_sach.entity.chat.ConversationDocument;
import com.example.web_ban_sach.entity.chat.MessageDocument;
import com.example.web_ban_sach.entity.chat.ParticipantDocument;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.util.MessageEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private MessageEncryptionUtil encryptionUtil;
    
    public ConversationModel createConversation(Integer userId, Integer sellerId) {
        // 1. Check if conversation already exists
        Query query = new Query(Criteria.where("userId").is(userId)
                                      .and("sellerId").is(sellerId));
        ConversationDocument existing = mongoTemplate.findOne(query, ConversationDocument.class, "conversations");
        
        if (existing != null) {
            return mapToConversationModel(existing, userId);
        }
        
        // 2. Get user names
        String userName = getUserNameById(userId);

        String sellerName = getSellerNameById(sellerId);
        System.out.println(sellerName);
        
        // 3. Create new conversation
        ConversationDocument conversation = new ConversationDocument();
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setSellerId(sellerId);
        conversation.setUserName(userName);
        conversation.setSellerName(sellerName);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        
        // Initialize participants
        List<ParticipantDocument> participants = Arrays.asList(
            new ParticipantDocument(userId, 0, null, null),
            new ParticipantDocument(sellerId, 0, null, null)
        );
        conversation.setParticipants(participants);
        
        mongoTemplate.save(conversation, "conversations");
        return mapToConversationModel(conversation, userId);
    }
    
    public RealtimeChatMessage saveMessage(RealtimeChatMessage message) {
        System.out.println("=== SAVING MESSAGE ===");
        System.out.println("Original content: " + message.getContent());
        
        // 1. Generate messageId if null
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        System.out.println("Generated messageId: " + message.getMessageId());
        
        // 2. Fill senderName from user service
        String senderName = getUserNameById(message.getSenderId());
        message.setSenderName(senderName);
        message.setTimestamp(LocalDateTime.now());
        System.out.println("Sender name: " + senderName);
        
        // 3. Create document and save to MongoDB
        MessageDocument doc = new MessageDocument();
        doc.setMessageId(message.getMessageId());
        doc.setConversationId(message.getConversationId());
        doc.setSenderId(message.getSenderId());
        doc.setReceiverId(message.getReceiverId());
        doc.setSenderName(senderName);
        
        // Encrypt message content before saving
        try {
            System.out.println("=== ENCRYPTING MESSAGE ===");
            String encryptedContent = encryptionUtil.encrypt(message.getContent());
            doc.setContent(encryptedContent);
            System.out.println("Message encrypted successfully");
        } catch (Exception e) {
            System.err.println("ERROR encrypting message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        doc.setMessageType(message.getType());
        doc.setStatus(message.getStatus());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        
        try {
            System.out.println("=== SAVING TO MONGODB ===");
            mongoTemplate.save(doc, "chat_messages");
            System.out.println("Message saved to MongoDB successfully");
        } catch (Exception e) {
            System.err.println("ERROR saving to MongoDB: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        // 4. Update conversation last message
        updateConversationLastMessage(message.getConversationId(), message.getContent(), LocalDateTime.now());
        
        // 5. Increment unread count for receiver
        incrementUnreadCount(message.getConversationId(), message.getReceiverId());
        
        System.out.println("=== MESSAGE SAVE COMPLETE ===");
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
                   .map(doc -> mapToConversationModel(doc, userId))
                   .collect(Collectors.toList());
    }
    
    public List<RealtimeChatMessage> getMessagesForConversation(String conversationId, Integer userId, int page, int size) {
        // 1. Validate user has access to conversation
        if (!hasAccessToConversation(conversationId, userId)) {
            throw new RuntimeException("User does not have access to this conversation");
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
    
    public Integer getOtherUserId(String conversationId, Integer currentUserId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        ConversationDocument conversation = mongoTemplate.findOne(query, ConversationDocument.class, "conversations");
        
        if (conversation == null) return null;
        
        return conversation.getUserId().equals(currentUserId) ? 
               conversation.getSellerId() : conversation.getUserId();
    }
    
    public boolean hasAccessToConversation(String conversationId, Integer userId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                                      .and("participants.userId").is(userId));
        return mongoTemplate.exists(query, "conversations");
    }
    
    private String getUserNameById(Integer userId) {
        try {
            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findById(userId);
            if (nguoiDungOpt.isPresent()) {
                NguoiDung nguoiDung = nguoiDungOpt.get();
                String hoDem = nguoiDung.getHoDem() != null ? nguoiDung.getHoDem() : "";
                String ten = nguoiDung.getTen() != null ? nguoiDung.getTen() : "";
                return (hoDem + " " + ten).trim();
            }
            return "Unknown User";
        } catch (Exception e) {
            return "Unknown User";
        }
    }

    private String getSellerNameById(Integer userId) {
        try {
            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findById(userId);
            if (nguoiDungOpt.isPresent()) {
                NguoiDung nguoiDung = nguoiDungOpt.get();
                String ten = nguoiDung.getTenGianHang() != null ? nguoiDung.getTenGianHang() : "";
                return ten;
            }
            return "Unknown User";
        } catch (Exception e) {
            return "Unknown User";
        }
    }
    
    private void updateConversationLastMessage(String conversationId, String lastMessage, LocalDateTime timestamp) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        
        // Encrypt last message before storing
        String encryptedLastMessage = encryptionUtil.encrypt(lastMessage);
        
        Update update = new Update().set("lastMessage", encryptedLastMessage)
                                   .set("lastMessageTime", timestamp)
                                   .set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, "conversations");
    }
    
    private void incrementUnreadCount(String conversationId, Integer receiverId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                                      .and("participants.userId").is(receiverId));
        Update update = new Update().inc("participants.$.unreadCount", 1);
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
    
    private ConversationModel mapToConversationModel(ConversationDocument doc, Integer currentUserId) {
        ConversationModel model = new ConversationModel();
        model.setConversationId(doc.getConversationId());
        model.setUserId(doc.getUserId());
        model.setSellerId(doc.getSellerId());
        model.setUserName(doc.getUserName());
        model.setSellerName(doc.getSellerName());
        
        // Decrypt last message before returning
        String decryptedLastMessage = null;
        if (doc.getLastMessage() != null && !doc.getLastMessage().isEmpty()) {
            try {
                decryptedLastMessage = encryptionUtil.decrypt(doc.getLastMessage());
            } catch (Exception e) {
                // If decryption fails, it might be an old unencrypted message
                decryptedLastMessage = doc.getLastMessage();
            }
        }
        model.setLastMessage(decryptedLastMessage);
        
        model.setLastMessageTime(doc.getLastMessageTime());
        model.setCreatedAt(doc.getCreatedAt());
        
        // Set unread count for current user
        if (doc.getParticipants() != null) {
            doc.getParticipants().stream()
                .filter(p -> p.getUserId().equals(currentUserId))
                .findFirst()
                .ifPresent(p -> model.setUnreadCount(p.getUnreadCount()));
        }
        
        return model;
    }
    
    private RealtimeChatMessage mapToRealtimeChatMessage(MessageDocument doc) {
        RealtimeChatMessage message = new RealtimeChatMessage();
        message.setMessageId(doc.getMessageId());
        message.setConversationId(doc.getConversationId());
        message.setSenderId(doc.getSenderId());
        message.setReceiverId(doc.getReceiverId());
        message.setSenderName(doc.getSenderName());
        
        // Decrypt message content before returning
        String decryptedContent = null;
        if (doc.getContent() != null && !doc.getContent().isEmpty()) {
            try {
                decryptedContent = encryptionUtil.decrypt(doc.getContent());
            } catch (Exception e) {
                // If decryption fails, it might be an old unencrypted message
                decryptedContent = doc.getContent();
            }
        }
        message.setContent(decryptedContent);
        
        message.setType(doc.getMessageType());
        message.setStatus(doc.getStatus());
        message.setTimestamp(doc.getCreatedAt());
        return message;
    }
}