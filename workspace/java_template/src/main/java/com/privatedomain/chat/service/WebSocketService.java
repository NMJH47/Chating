package com.privatedomain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatedomain.chat.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for WebSocket messaging and real-time communication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Send a message to a specific topic.
     *
     * @param destination The destination topic
     * @param payload The payload to send
     */
    public void sendToTopic(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Message sent to topic: {}", destination);
        } catch (Exception e) {
            log.error("Failed to send message to topic {}: {}", destination, e.getMessage());
        }
    }
    
    /**
     * Send a message to all users in a group.
     *
     * @param groupId Group ID
     * @param payload The payload to send
     */
    public void sendToGroup(Long groupId, Object payload) {
        String destination = "/topic/group." + groupId;
        sendToTopic(destination, payload);
    }
    
    /**
     * Send a chat message to all users in a group.
     *
     * @param groupId Group ID
     * @param message The message to send
     */
    public void sendMessageToGroup(Long groupId, Message message) {
        Map<String, Object> payload = Map.of(
            "type", "NEW_MESSAGE",
            "groupId", groupId,
            "message", message
        );
        sendToGroup(groupId, payload);
    }
    
    /**
     * Send a message to a specific user.
     *
     * @param userId User ID
     * @param payload The payload to send
     */
    public void sendToUser(Long userId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                payload
            );
            log.debug("Message sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send message to user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Send a message to multiple users.
     *
     * @param userIds List of user IDs
     * @param payload The payload to send
     */
    public void sendToUsers(Iterable<Long> userIds, Object payload) {
        for (Long userId : userIds) {
            sendToUser(userId, payload);
        }
    }
    
    /**
     * Send a notification to a specific user.
     *
     * @param userId User ID
     * @param type Notification type
     * @param title Notification title
     * @param content Notification content
     * @param metadata Additional metadata
     */
    public void sendNotification(Long userId, String type, String title, String content, Map<String, Object> metadata) {
        Map<String, Object> notification = Map.of(
            "type", "NOTIFICATION",
            "notificationType", type,
            "title", title,
            "content", content,
            "timestamp", System.currentTimeMillis(),
            "metadata", metadata != null ? metadata : Map.of()
        );
        
        sendToUser(userId, notification);
    }
    
    /**
     * Send a system announcement to all connected users.
     *
     * @param title Announcement title
     * @param content Announcement content
     * @param metadata Additional metadata
     */
    public void sendSystemAnnouncement(String title, String content, Map<String, Object> metadata) {
        Map<String, Object> announcement = Map.of(
            "type", "SYSTEM_ANNOUNCEMENT",
            "title", title,
            "content", content,
            "timestamp", System.currentTimeMillis(),
            "metadata", metadata != null ? metadata : Map.of()
        );
        
        sendToTopic("/topic/announcements", announcement);
    }
    
    /**
     * Send presence update when a user comes online/offline.
     *
     * @param userId User ID
     * @param online Whether the user is online
     */
    public void sendPresenceUpdate(Long userId, boolean online) {
        Map<String, Object> presenceUpdate = Map.of(
            "type", "PRESENCE_UPDATE",
            "userId", userId,
            "status", online ? "ONLINE" : "OFFLINE",
            "timestamp", System.currentTimeMillis()
        );
        
        sendToTopic("/topic/presence", presenceUpdate);
    }
    
    /**
     * Send typing indicator to a group.
     *
     * @param groupId Group ID
     * @param userId User ID of the user typing
     * @param isTyping Whether the user is typing or stopped typing
     */
    public void sendTypingIndicator(Long groupId, Long userId, boolean isTyping) {
        Map<String, Object> typingUpdate = Map.of(
            "type", "TYPING_INDICATOR",
            "groupId", groupId,
            "userId", userId,
            "isTyping", isTyping,
            "timestamp", System.currentTimeMillis()
        );
        
        sendToGroup(groupId, typingUpdate);
    }
}