package com.privatedomain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Document representing a file attachment in the chat system.
 */
@Document(collection = "attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    
    @Id
    private String id;
    
    private String messageId;
    
    private String fileName;
    
    private String fileType;
    
    private Long fileSize;
    
    private String url;
    
    private LocalDateTime uploadedAt;
    
    @Builder.Default
    private Boolean isTemp = false;
}