package com.privatedomain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity representing a chat group.
 */
@Entity
@Table(name = "groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(min = 3, max = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String avatar;
    
    @Column(name = "owner_id")
    private Long ownerId;
    
    @Column(name = "is_private")
    private Boolean isPrivate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "max_members")
    private Integer maxMembers;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.maxMembers == null) {
            this.maxMembers = 200; // Default max members
        }
        if (this.isPrivate == null) {
            this.isPrivate = true; // Default to private
        }
    }
}