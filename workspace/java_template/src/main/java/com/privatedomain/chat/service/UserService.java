package com.privatedomain.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.privatedomain.chat.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User service interface that defines operations for managing user entities
 */
public interface UserService {
    
    /**
     * Authenticate a user with username and password, returning a JWT token
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @return JWT token string if authentication is successful
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    String authenticateUser(String username, String password);
    
    /**
     * Register a new user
     *
     * @param user the user to register
     * @return the registered user
     * @throws IllegalArgumentException if username or email already exists
     */
    User registerUser(User user);
    
    /**
     * Find a user by username
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by email
     *
     * @param email the email to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a username exists
     *
     * @param username the username to check
     * @return true if the username exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if an email exists
     *
     * @param email the email to check
     * @return true if the email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users active since a specific time
     *
     * @param since the time threshold for activity
     * @return list of recently active users
     */
    List<User> findRecentlyActiveUsers(LocalDateTime since);
    
    /**
     * Find users by group ID
     *
     * @param groupId the ID of the group
     * @return list of users in the specified group
     */
    List<User> findUsersByGroupId(Long groupId);
    
    /**
     * Check if a user is in a specific group
     *
     * @param userId  the user ID
     * @param groupId the group ID
     * @return true if the user is in the group, false otherwise
     */
    boolean isUserInGroup(Long userId, Long groupId);
    
    /**
     * Update a user's active status
     *
     * @param userId   the ID of the user to update
     * @param isActive the new active status
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    User updateUserActiveStatus(Long userId, boolean isActive);
    
    /**
     * Update a user's last login timestamp to the current time
     *
     * @param userId the ID of the user to update
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    User updateLastLogin(Long userId);
    
    /**
     * Get paginated list of users
     *
     * @param pageNum  the page number (1-based)
     * @param pageSize the size of each page
     * @return a page of users
     */
    Page<User> getUsersWithPagination(int pageNum, int pageSize);
    
    /**
     * Search users by keyword in username or email
     *
     * @param keyword the search keyword
     * @return list of matching users
     */
    List<User> searchUsers(String keyword);
}