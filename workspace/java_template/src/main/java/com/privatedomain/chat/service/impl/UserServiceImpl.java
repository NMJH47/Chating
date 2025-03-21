package com.privatedomain.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.privatedomain.chat.mapper.UserMapper;
import com.privatedomain.chat.model.User;
import com.privatedomain.chat.security.JwtUtils;
import com.privatedomain.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserService that uses MyBatis Plus for database operations
 * Extends ServiceImpl which provides base CRUD operations for entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    
    @Override
    public User registerUser(User user) {
        if (userMapper.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (userMapper.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        
        // Use MyBatis Plus save method
        save(user);
        
        return user;
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        User user = userMapper.findByUsername(username);
        return Optional.ofNullable(user);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        User user = userMapper.findByEmail(email);
        return Optional.ofNullable(user);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }
    
    @Override
    public List<User> findRecentlyActiveUsers(LocalDateTime since) {
        return userMapper.findRecentlyActiveUsers(since);
    }
    
    @Override
    public List<User> findUsersByGroupId(Long groupId) {
        return userMapper.findUsersByGroupId(groupId);
    }
    
    @Override
    public boolean isUserInGroup(Long userId, Long groupId) {
        return userMapper.isUserInGroup(userId, groupId);
    }
    
    @Override
    public User updateUserActiveStatus(Long userId, boolean isActive) {
        User user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        user.setIsActive(isActive);
        updateById(user);
        return user;
    }
    
    @Override
    public User updateLastLogin(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        user.setLastLogin(LocalDateTime.now());
        updateById(user);
        return user;
    }
    
    @Override
    public Page<User> getUsersWithPagination(int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        return page(page);
    }
    
    @Override
    public List<User> searchUsers(String keyword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getEmail, keyword);
        return list(queryWrapper);
    }
    
    @Override
    public String authenticateUser(String username, String password) {
        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        
        // Update security context with authentication
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        // Update user's last login timestamp
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            updateLastLogin(userOpt.get().getId());
        }
        
        return jwt;
    }
}