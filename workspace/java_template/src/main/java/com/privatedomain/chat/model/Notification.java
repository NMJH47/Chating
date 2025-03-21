package com.privatedomain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Document representing a notification in the chat system.
 */
@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    private String id;
    
    @Indexed
    private Long userId;
    
    private NotificationType type;
    
    private String title;
    
    private String content;
    
    @Builder.Default
    private Boolean isRead = false;
    
    @Indexed
    private LocalDateTime createdAt;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}