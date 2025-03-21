package com.privatedomain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing additional profile information for a user.
 */
@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private String nickname;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Type(type = "json")
    @Column(columnDefinition = "json")
    private Map<String, Object> preferences = new HashMap<>();
    
    private String location;
    
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;
}