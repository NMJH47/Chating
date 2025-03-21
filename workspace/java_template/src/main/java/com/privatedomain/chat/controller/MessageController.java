package com.privatedomain.chat.controller;

import com.privatedomain.chat.model.Message;
import com.privatedomain.chat.model.MessageType;
import com.privatedomain.chat.security.UserDetailsImpl;
import com.privatedomain.chat.service.GroupService;
import com.privatedomain.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing chat messages.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final GroupService groupService;

    /**
     * DTO for text message.
     */
    public static class TextMessageRequest {
        @NotBlank
        @Size(max = 10000)
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * DTO for message edit.
     */
    public static class EditMessageRequest {
        @NotBlank
        @Size(max = 10000)
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * Send a text message to a group.
     *
     * @param groupId     Group ID
     * @param request     Message content
     * @param userDetails Authenticated user details
     * @return Created message
     */
    @PostMapping("/groups/{groupId}/text")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> sendTextMessage(
            @PathVariable Long groupId,
            @Valid @RequestBody TextMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to send messages"));
            }

            Message message = messageService.sendTextMessage(groupId, userDetails.getId(), request.getContent());
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (Exception e) {
            log.error("Error sending text message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error sending message: " + e.getMessage()));
        }
    }

    /**
     * Send a message with attachments to a group.
     *
     * @param groupId     Group ID
     * @param content     Optional text content
     * @param type        Message type (IMAGE, VIDEO, AUDIO, FILE)
     * @param files       Attachment files
     * @param userDetails Authenticated user details
     * @return Created message
     */
    @PostMapping("/groups/{groupId}/attachments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> sendMessageWithAttachments(
            @PathVariable Long groupId,
            @RequestParam(required = false) String content,
            @RequestParam MessageType type,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to send messages"));
            }

            // Validate message type
            if (type != MessageType.IMAGE && type != MessageType.VIDEO && 
                type != MessageType.AUDIO && type != MessageType.FILE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid message type for attachment"));
            }

            // Check if files are provided
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "No files provided"));
            }

            Message message = messageService.sendMessageWithAttachments(
                    groupId, userDetails.getId(), content, type, files);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (Exception e) {
            log.error("Error sending message with attachments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error sending message: " + e.getMessage()));
        }
    }

    /**
     * Get messages for a group with pagination.
     *
     * @param groupId     Group ID
     * @param page        Page number (0-based)
     * @param size        Page size
     * @param userDetails Authenticated user details
     * @return Page of messages
     */
    @GetMapping("/groups/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to view messages"));
            }

            Page<Message> messages = messageService.getGroupMessages(groupId, page, size);
            
            // Update last read time
            groupService.updateLastRead(groupId, userDetails.getId());
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving messages: " + e.getMessage()));
        }
    }

    /**
     * Get messages for a group since a specific time.
     *
     * @param groupId     Group ID
     * @param since       Timestamp (ISO format)
     * @param userDetails Authenticated user details
     * @return List of messages since the specified time
     */
    @GetMapping("/groups/{groupId}/since")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMessagesSince(
            @PathVariable Long groupId,
            @RequestParam String since,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to view messages"));
            }

            // Parse timestamp
            LocalDateTime sinceTimestamp;
            try {
                sinceTimestamp = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid timestamp format. Use ISO format (e.g., 2023-01-01T00:00:00)"));
            }

            List<Message> messages = messageService.getMessagesSince(groupId, sinceTimestamp);
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving messages: " + e.getMessage()));
        }
    }

    /**
     * Search messages in a group by content.
     *
     * @param groupId     Group ID
     * @param query       Search keyword
     * @param page        Page number (0-based)
     * @param size        Page size
     * @param userDetails Authenticated user details
     * @return Page of matching messages
     */
    @GetMapping("/groups/{groupId}/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> searchMessages(
            @PathVariable Long groupId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to search messages"));
            }

            Page<Message> messages = messageService.searchMessages(groupId, query, page, size);
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error searching messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error searching messages: " + e.getMessage()));
        }
    }

    /**
     * Edit a message.
     *
     * @param messageId   Message ID
     * @param request     Edit request with new content
     * @param userDetails Authenticated user details
     * @return Updated message
     */
    @PutMapping("/{messageId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Message updatedMessage = messageService.editMessage(messageId, request.getContent(), userDetails.getId());
            return ResponseEntity.ok(updatedMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error editing message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error editing message: " + e.getMessage()));
        }
    }

    /**
     * Delete a message.
     *
     * @param messageId   Message ID
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            boolean deleted = messageService.deleteMessage(messageId, userDetails.getId());
            
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to delete message"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting message: " + e.getMessage()));
        }
    }

    /**
     * Mark a message as read.
     *
     * @param messageId   Message ID
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @PostMapping("/{messageId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markMessageAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            messageService.markMessageAsRead(messageId, userDetails.getId());
            return ResponseEntity.ok(Map.of("message", "Message marked as read"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error marking message as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error marking message as read: " + e.getMessage()));
        }
    }

    /**
     * Send typing indicator to a group.
     *
     * @param groupId     Group ID
     * @param isTyping    Whether the user is typing
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @PostMapping("/groups/{groupId}/typing")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> sendTypingIndicator(
            @PathVariable Long groupId,
            @RequestParam boolean isTyping,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to send typing indicators"));
            }

            // Send typing indicator via WebSocket
            messageService.sendTypingIndicator(groupId, userDetails.getId(), isTyping);
            
            return ResponseEntity.ok(Map.of("message", "Typing indicator sent"));
        } catch (Exception e) {
            log.error("Error sending typing indicator", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error sending typing indicator: " + e.getMessage()));
        }
    }
}