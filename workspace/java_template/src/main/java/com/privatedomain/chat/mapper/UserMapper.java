package com.privatedomain.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.privatedomain.chat.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis Plus mapper interface for User entity
 * Provides CRUD operations and custom queries
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * Find a user by username
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    /**
     * Select a user by username with Optional return type
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    java.util.Optional<User> selectByUsername(String username);
    
    /**
     * Find a user by email
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);
    
    /**
     * Check if a username exists
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    boolean existsByUsername(String username);
    
    /**
     * Check if an email exists
     */
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    boolean existsByEmail(String email);
    
    /**
     * Find recently active users
     */
    @Select("SELECT * FROM users WHERE last_login > #{since}")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
    
    /**
     * Find users by group ID
     */
    @Select("SELECT u.* FROM users u JOIN group_members gm ON u.id = gm.user_id WHERE gm.group_id = #{groupId}")
    List<User> findUsersByGroupId(@Param("groupId") Long groupId);
    
    /**
     * Check if a user is in a group
     */
    @Select("SELECT COUNT(*) > 0 FROM users u JOIN group_members gm ON u.id = gm.user_id WHERE u.id = #{userId} AND gm.group_id = #{groupId}")
    boolean isUserInGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);
}