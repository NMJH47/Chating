package com.privatedomain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the membership relation between a user and a group.
 */
@Entity
@Table(name = "group_members", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"group_id", "user_id"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "group_id")
    private Long groupId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    private GroupRole role;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Column(name = "last_read")
    private LocalDateTime lastRead;
    
    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        this.lastRead = this.joinedAt;
        if (this.role == null) {
            this.role = GroupRole.MEMBER;
        }
    }
}