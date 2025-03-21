package com.privatedomain.chat.repository.postgres;

import com.privatedomain.chat.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    List<Group> findByOwnerId(Long ownerId);
    
    @Query("SELECT g FROM Group g WHERE g.isPrivate = false")
    Page<Group> findPublicGroups(Pageable pageable);
    
    @Query(value = "SELECT g.* FROM groups g JOIN group_members gm ON g.id = gm.group_id " +
                   "WHERE gm.user_id = :userId ORDER BY g.name", 
           nativeQuery = true)
    List<Group> findGroupsByUserId(@Param("userId") Long userId);
    
    @Query(value = "SELECT g.* FROM groups g JOIN group_members gm ON g.id = gm.group_id " +
                   "WHERE gm.user_id = :userId " +
                   "ORDER BY (SELECT MAX(m.sent_at) FROM messages m WHERE m.group_id = g.id) DESC " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<Group> findRecentActiveGroupsByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId")
    int countMembersByGroupId(@Param("groupId") Long groupId);
}