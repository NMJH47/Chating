package com.privatedomain.chat.repository.mongodb;

import com.privatedomain.chat.model.Notification;
import com.privatedomain.chat.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("{'userId': ?0, 'isRead': false}")
    List<Notification> findUnreadNotifications(Long userId);
    
    @Query("{'userId': ?0, 'isRead': false, 'type': ?1}")
    List<Notification> findUnreadNotificationsByType(Long userId, NotificationType type);
    
    @Query(value = "{'userId': ?0, 'isRead': false}", count = true)
    long countUnreadNotifications(Long userId);
    
    @Query("{'userId': ?0, 'createdAt': {'$gt': ?1}}")
    List<Notification> findNotificationsSince(Long userId, LocalDateTime since);
    
    @Query("{'userId': ?0, 'type': {'$in': ?1}}")
    Page<Notification> findByUserIdAndTypes(Long userId, List<NotificationType> types, Pageable pageable);
}