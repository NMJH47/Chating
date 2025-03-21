package com.privatedomain.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.privatedomain.chat.mapper.UserMapper;
import com.privatedomain.chat.model.Role;
import com.privatedomain.chat.model.User;
import com.privatedomain.chat.security.jwt.JwtUtils;
import com.privatedomain.chat.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setLastLoginAt(LocalDateTime.now());
        testUser.setActive(true);
        testUser.setLocked(false);

        // Setup user role
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(Role.ERole.ROLE_USER.toString());

        // Setup roles for test user
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Test
    @DisplayName("Authenticate user with valid credentials should return JWT token")
    void authenticateUserValidCredentials() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String jwtToken = "mock.jwt.token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn(jwtToken);

        // Act
        String result = userService.authenticateUser(username, password);

        // Assert
        assertThat(result).isEqualTo(jwtToken);
        verify(authenticationManager).authenticate(
                argThat(auth -> 
                    auth.getPrincipal().equals(username) && 
                    auth.getCredentials().equals(password)
                )
        );
    }

    @Test
    @DisplayName("Register new user should create user and return user object")
    void registerUserSuccess() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("password123");
        
        String encodedPassword = "encodedPassword";
        
        when(passwordEncoder.encode(newUser.getPassword())).thenReturn(encodedPassword);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        // Act
        User result = userService.registerUser(newUser);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.isActive()).isTrue();
        assertThat(result.isLocked()).isFalse();
        
        // Verify timestamps are set
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("Find by username should return user when user exists")
    void findByUsernameExistingUser() {
        // Arrange
        String username = "testuser";
        
        when(userMapper.selectByUsername(username)).thenReturn(testUser);
        
        // Act
        Optional<User> result = userService.findByUsername(username);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userMapper).selectByUsername(username);
    }

    @Test
    @DisplayName("Find by username should return empty optional when user doesn't exist")
    void findByUsernameNonExistingUser() {
        // Arrange
        String username = "nonexistentuser";
        
        when(userMapper.selectByUsername(username)).thenReturn(null);
        
        // Act
        Optional<User> result = userService.findByUsername(username);
        
        // Assert
        assertFalse(result.isPresent());
        verify(userMapper).selectByUsername(username);
    }

    @Test
    @DisplayName("Find by email should return user when email exists")
    void findByEmailExistingEmail() {
        // Arrange
        String email = "test@example.com";
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        when(userMapper.selectOne(any())).thenReturn(testUser);
        
        // Act
        Optional<User> result = userService.findByEmail(email);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userMapper).selectOne(any());
    }

    @Test
    @DisplayName("Check if username exists should return true when username exists")
    void existsByUsernameTrue() {
        // Arrange
        String username = "testuser";
        
        when(userMapper.selectByUsername(username)).thenReturn(testUser);
        
        // Act
        boolean result = userService.existsByUsername(username);
        
        // Assert
        assertTrue(result);
        verify(userMapper).selectByUsername(username);
    }

    @Test
    @DisplayName("Check if email exists should return true when email exists")
    void existsByEmailTrue() {
        // Arrange
        String email = "test@example.com";
        
        when(userMapper.selectCount(any())).thenReturn(1L);
        
        // Act
        boolean result = userService.existsByEmail(email);
        
        // Assert
        assertTrue(result);
        verify(userMapper).selectCount(any());
    }

    @Test
    @DisplayName("Update user active status should update user's active field")
    void updateUserActiveStatus() {
        // Arrange
        Long userId = 1L;
        boolean newActiveStatus = false;
        
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        
        // Act
        User result = userService.updateUserActiveStatus(userId, newActiveStatus);
        
        // Assert
        assertFalse(result.isActive());
        assertEquals(userId, result.getId());
        
        // Capture the user that was passed to updateById
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("Update user active status should throw exception when user not found")
    void updateUserActiveStatusUserNotFound() {
        // Arrange
        Long userId = 999L;
        boolean newActiveStatus = false;
        
        when(userMapper.selectById(userId)).thenReturn(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            userService.updateUserActiveStatus(userId, newActiveStatus)
        );
        
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("Update last login should update user's last login timestamp")
    void updateLastLogin() {
        // Arrange
        Long userId = 1L;
        LocalDateTime beforeUpdate = testUser.getLastLoginAt();
        
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        
        // Act
        User result = userService.updateLastLogin(userId);
        
        // Assert
        assertNotNull(result.getLastLoginAt());
        assertNotEquals(beforeUpdate, result.getLastLoginAt());
        
        // Capture the user that was passed to updateById
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getLastLoginAt()).isNotEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Get users with pagination should return page of users")
    void getUsersWithPagination() {
        // Arrange
        int pageNum = 1;
        int pageSize = 10;
        List<User> userList = Collections.singletonList(testUser);
        Page<User> userPage = new Page<>(pageNum, pageSize);
        userPage.setRecords(userList);
        userPage.setTotal(1);
        
        when(userMapper.selectPage(any(), any())).thenReturn(userPage);
        
        // Act
        Page<User> result = userService.getUsersWithPagination(pageNum, pageSize);
        
        // Assert
        assertEquals(1, result.getRecords().size());
        assertEquals(testUser, result.getRecords().get(0));
        assertEquals(1, result.getTotal());
        verify(userMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("Search users should return matching users")
    void searchUsers() {
        // Arrange
        String keyword = "test";
        List<User> userList = Collections.singletonList(testUser);
        
        when(userMapper.selectList(any())).thenReturn(userList);
        
        // Act
        List<User> result = userService.searchUsers(keyword);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userMapper).selectList(any());
    }
}