package com.privatedomain.chat.repository.postgres;

import com.privatedomain.chat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.lastLogin > :since")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
    
    @Query(value = "SELECT u.* FROM users u JOIN group_members gm ON u.id = gm.user_id WHERE gm.group_id = :groupId", 
           nativeQuery = true)
    List<User> findUsersByGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "JOIN GroupMember gm ON u.id = gm.userId " +
           "WHERE u.id = :userId AND gm.groupId = :groupId")
    boolean isUserInGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);
}