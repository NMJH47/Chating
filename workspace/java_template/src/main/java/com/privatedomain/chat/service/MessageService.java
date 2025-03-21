package com.privatedomain.chat.service;

import com.privatedomain.chat.model.Attachment;
import com.privatedomain.chat.model.Message;
import com.privatedomain.chat.model.MessageType;
import com.privatedomain.chat.repository.mongodb.MessageRepository;
import com.privatedomain.chat.repository.postgres.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing chat messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final WebSocketService webSocketService;
    
    /**
     * Send a text message in a group.
     *
     * @param groupId Group ID
     * @param senderId User ID of the sender
     * @param content Message content
     * @return The saved message
     */
    @Transactional
    public Message sendTextMessage(Long groupId, Long senderId, String content) {
        // Check if user is a member of the group
        if (!groupService.isGroupMember(groupId, senderId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        // Create and save the message
        Message message = Message.builder()
                .groupId(groupId)
                .senderId(senderId)
                .type(MessageType.TEXT)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isDeleted(false)
                .isEdited(false)
                .deliveredTo(new ArrayList<>())
                .readBy(new ArrayList<>())
                .build();

        Message savedMessage = messageRepository.save(message);
        
        // Deliver message via WebSocket
        webSocketService.sendMessageToGroup(groupId, savedMessage);
        
        return savedMessage;
    }

    /**
     * Send a message with attachments.
     *
     * @param groupId Group ID
     * @param senderId User ID of the sender
     * @param content Message content (optional)
     * @param type Message type (IMAGE, VIDEO, AUDIO, FILE)
     * @param files Uploaded files
     * @return The saved message
     */
    @Transactional
    public Message sendMessageWithAttachments(Long groupId, Long senderId, String content, 
                                           MessageType type, List<MultipartFile> files) {
        // Check if user is a member of the group
        if (!groupService.isGroupMember(groupId, senderId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        // Create the message
        Message message = Message.builder()
                .groupId(groupId)
                .senderId(senderId)
                .type(type)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isDeleted(false)
                .isEdited(false)
                .attachments(new ArrayList<>())
                .deliveredTo(new ArrayList<>())
                .readBy(new ArrayList<>())
                .build();
        
        // Process attachments
        if (files != null && !files.isEmpty()) {
            String uploadDir = "./uploads/" + groupId + "/" + LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            try {
                Files.createDirectories(Paths.get(uploadDir));
                
                for (MultipartFile file : files) {
                    String originalFilename = file.getOriginalFilename();
                    String uniqueFilename = UUID.randomUUID().toString() + "-" + originalFilename;
                    Path targetPath = Paths.get(uploadDir, uniqueFilename);
                    Files.copy(file.getInputStream(), targetPath);
                    
                    Attachment attachment = Attachment.builder()
                            .fileName(originalFilename)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .url(targetPath.toString())
                            .uploadedAt(LocalDateTime.now())
                            .build();
                    
                    message.getAttachments().add(attachment);
                }
            } catch (IOException e) {
                log.error("Failed to save attachment", e);
                throw new RuntimeException("Failed to save attachment: " + e.getMessage());
            }
        }
        
        Message savedMessage = messageRepository.save(message);
        
        // Deliver message via WebSocket
        webSocketService.sendMessageToGroup(groupId, savedMessage);
        
        return savedMessage;
    }

    /**
     * Edit a message.
     *
     * @param messageId Message ID
     * @param content New content
     * @param userId User ID of the requester
     * @return The updated message
     */
    public Message editMessage(String messageId, String content, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        // Only the sender can edit the message
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("You can only edit your own messages");
        }
        
        // Cannot edit deleted messages
        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit deleted messages");
        }

        // Update message content
        message.setContent(content);
        message.setIsEdited(true);
        
        Message updatedMessage = messageRepository.save(message);
        
        // Notify group about edited message
        webSocketService.sendToGroup(message.getGroupId(), Map.of(
            "type", "MESSAGE_EDITED",
            "messageId", messageId,
            "groupId", message.getGroupId(),
            "senderId", userId,
            "message", updatedMessage
        ));
        
        return updatedMessage;
    }

    /**
     * Delete a message.
     *
     * @param messageId Message ID
     * @param userId User ID of the requester
     * @return true if message was deleted
     */
    public boolean deleteMessage(String messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        // Check if user is authorized to delete this message
        boolean isMessageSender = message.getSenderId().equals(userId);
        boolean isGroupAdmin = false;
        
        try {
            if (!isMessageSender) {
                // Check if user is admin/moderator/owner of the group
                GroupService.GroupRole role = groupService.getGroupRole(message.getGroupId(), userId);
                isGroupAdmin = role == GroupService.GroupRole.OWNER || 
                               role == GroupService.GroupRole.ADMIN || 
                               role == GroupService.GroupRole.MODERATOR;
            }
        } catch (RuntimeException e) {
            // User is not a member of the group
            if (!isMessageSender) {
                throw new RuntimeException("You don't have permission to delete this message");
            }
        }
        
        if (!isMessageSender && !isGroupAdmin) {
            throw new RuntimeException("You don't have permission to delete this message");
        }

        // Soft delete the message
        message.setIsDeleted(true);
        message.setContent("[This message has been deleted]");
        messageRepository.save(message);
        
        // Notify group about deleted message
        webSocketService.sendToGroup(message.getGroupId(), Map.of(
            "type", "MESSAGE_DELETED",
            "messageId", messageId,
            "groupId", message.getGroupId(),
            "deletedBy", userId
        ));
        
        return true;
    }
    
    /**
     * Mark a message as delivered to a user.
     *
     * @param messageId Message ID
     * @param userId User ID
     */
    public void markMessageAsDelivered(String messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!message.getDeliveredTo().contains(userId)) {
            message.getDeliveredTo().add(userId);
            messageRepository.save(message);
        }
    }
    
    /**
     * Mark a message as read by a user.
     *
     * @param messageId Message ID
     * @param userId User ID
     */
    public void markMessageAsRead(String messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!message.getReadBy().contains(userId)) {
            message.getReadBy().add(userId);
            messageRepository.save(message);
            
            // Also mark as delivered if not already
            if (!message.getDeliveredTo().contains(userId)) {
                message.getDeliveredTo().add(userId);
            }
            
            messageRepository.save(message);
        }
        
        // Update the last read time for the group member
        groupMemberRepository.updateLastRead(message.getGroupId(), userId, LocalDateTime.now());
    }

    /**
     * Get messages in a group with pagination.
     *
     * @param groupId Group ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of messages
     */
    public Page<Message> getGroupMessages(Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        return messageRepository.findByGroupIdOrderBySentAtDesc(groupId, pageable);
    }
    
    /**
     * Get messages in a group since a specific time.
     *
     * @param groupId Group ID
     * @param since Timestamp
     * @return List of messages since the specified time
     */
    public List<Message> getMessagesSince(Long groupId, LocalDateTime since) {
        return messageRepository.findMessagesSince(groupId, since);
    }
    
    /**
     * Search messages in a group by content.
     *
     * @param groupId Group ID
     * @param keyword Search keyword
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of matching messages
     */
    public Page<Message> searchMessages(Long groupId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.searchMessagesByContent(keyword, groupId, pageable);
    }
    
    /**
     * Get a count of messages in a group.
     *
     * @param groupId Group ID
     * @return Number of messages in the group
     */
    public long countMessagesByGroup(Long groupId) {
        return messageRepository.countByGroupId(groupId);
    }
    
    /**
     * Get the most recent message in a group.
     *
     * @param groupId Group ID
     * @return The most recent message, if any
     */
    public Message getLastMessage(Long groupId) {
        return messageRepository.findTopByGroupIdOrderBySentAtDesc(groupId).orElse(null);
    }
    
    /**
     * Send typing indicator to a group.
     *
     * @param groupId Group ID
     * @param userId User ID of the user typing
     * @param isTyping Whether the user is typing or stopped typing
     */
    public void sendTypingIndicator(Long groupId, Long userId, boolean isTyping) {
        webSocketService.sendTypingIndicator(groupId, userId, isTyping);
    }
}