package com.privatedomain.chat.repository.mongodb;

import com.privatedomain.chat.model.Message;
import com.privatedomain.chat.model.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    Page<Message> findByGroupIdOrderBySentAtDesc(Long groupId, Pageable pageable);
    
    @Query("{'groupId': ?0, 'sentAt': {'$gt': ?1}}")
    List<Message> findMessagesSince(Long groupId, LocalDateTime since);
    
    @Query("{'groupId': ?0, 'sentAt': {'$lt': ?1}}")
    Page<Message> findMessagesBefore(Long groupId, LocalDateTime before, Pageable pageable);
    
    @Query("{'groupId': ?0, 'senderId': ?1}")
    Page<Message> findBySenderAndGroup(Long groupId, Long senderId, Pageable pageable);
    
    @Query("{'groupId': ?0, 'type': ?1}")
    List<Message> findByGroupIdAndType(Long groupId, MessageType type);
    
    @Query("{'content': {'$regex': ?0, '$options': 'i'}, 'groupId': ?1}")
    Page<Message> searchMessagesByContent(String keyword, Long groupId, Pageable pageable);
    
    @Query(value = "{'groupId': ?0}", count = true)
    long countByGroupId(Long groupId);
    
    Optional<Message> findTopByGroupIdOrderBySentAtDesc(Long groupId);
    
    @Query("{'isDeleted': false, 'groupId': {'$in': ?0}}")
    List<Message> findRecentMessagesByGroups(List<Long> groupIds, Pageable pageable);
}