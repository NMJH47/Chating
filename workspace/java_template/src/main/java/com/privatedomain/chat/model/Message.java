package com.privatedomain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Document representing a message in the chat system.
 * Stored in MongoDB for better scalability with high volume message data.
 */
@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    private String id;
    
    @Indexed
    private Long groupId;
    
    @Indexed
    private Long senderId;
    
    @Indexed
    private MessageType type;
    
    private String content;
    
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();
    
    @Indexed
    private LocalDateTime sentAt;
    
    private Boolean isDeleted;
    
    private Boolean isEdited;
    
    @Builder.Default
    private List<Long> readBy = new ArrayList<>();
    
    @Builder.Default
    private List<Long> deliveredTo = new ArrayList<>();
}