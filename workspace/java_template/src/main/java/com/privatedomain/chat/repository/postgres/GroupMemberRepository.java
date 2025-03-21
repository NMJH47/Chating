package com.privatedomain.chat.repository.postgres;

import com.privatedomain.chat.model.GroupMember;
import com.privatedomain.chat.model.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    List<GroupMember> findByGroupId(Long groupId);
    
    List<GroupMember> findByUserId(Long userId);
    
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.role IN :roles")
    List<GroupMember> findByGroupIdAndRoles(@Param("groupId") Long groupId, @Param("roles") List<GroupRole> roles);
    
    @Query("SELECT gm.role FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.userId = :userId")
    Optional<GroupRole> findRoleByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE GroupMember gm SET gm.lastRead = :lastRead WHERE gm.groupId = :groupId AND gm.userId = :userId")
    int updateLastRead(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("lastRead") LocalDateTime lastRead);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.userId = :userId")
    int removeUserFromGroup(@Param("groupId") Long groupId, @Param("userId") Long userId);
}