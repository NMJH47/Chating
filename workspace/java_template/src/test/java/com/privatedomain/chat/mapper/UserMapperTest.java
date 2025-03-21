package com.privatedomain.chat.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.privatedomain.chat.model.Role;
import com.privatedomain.chat.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisPlusTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RoleMapper roleMapper;
    
    private User testUser1;
    private User testUser2;
    private Role userRole;
    private Role adminRole;
    
    @BeforeEach
    void setUp() {
        // Clean up existing test data
        userMapper.delete(new LambdaQueryWrapper<User>()
                .like(User::getUsername, "testuser"));
        
        // Create roles if they don't exist
        userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getName, Role.ERole.ROLE_USER.toString()));
        
        if (userRole == null) {
            userRole = new Role();
            userRole.setName(Role.ERole.ROLE_USER.toString());
            roleMapper.insert(userRole);
        }
        
        adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getName, Role.ERole.ROLE_ADMIN.toString()));
        
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName(Role.ERole.ROLE_ADMIN.toString());
            roleMapper.insert(adminRole);
        }
        
        // Create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@example.com");
        testUser1.setPassword("encodedPassword1");
        testUser1.setFirstName("Test");
        testUser1.setLastName("User1");
        testUser1.setCreatedAt(LocalDateTime.now());
        testUser1.setUpdatedAt(LocalDateTime.now());
        testUser1.setLastLoginAt(null);
        testUser1.setActive(true);
        testUser1.setLocked(false);
        
        Set<Role> roles1 = new HashSet<>();
        roles1.add(userRole);
        testUser1.setRoles(roles1);
        
        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@example.com");
        testUser2.setPassword("encodedPassword2");
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        testUser2.setCreatedAt(LocalDateTime.now());
        testUser2.setUpdatedAt(LocalDateTime.now());
        testUser2.setLastLoginAt(LocalDateTime.now());
        testUser2.setActive(true);
        testUser2.setLocked(false);
        
        Set<Role> roles2 = new HashSet<>();
        roles2.add(userRole);
        roles2.add(adminRole);
        testUser2.setRoles(roles2);
        
        // Insert test users
        userMapper.insert(testUser1);
        userMapper.insert(testUser2);
    }
    
    @Test
    @DisplayName("Select by ID should return correct user")
    void testSelectById() {
        // Act
        User foundUser = userMapper.selectById(testUser1.getId());
        
        // Assert
        assertNotNull(foundUser);
        assertEquals(testUser1.getUsername(), foundUser.getUsername());
        assertEquals(testUser1.getEmail(), foundUser.getEmail());
    }
    
    @Test
    @DisplayName("Select by username should return correct user")
    void testSelectByUsername() {
        // Act
        User foundUser = userMapper.selectByUsername(testUser1.getUsername());
        
        // Assert
        assertNotNull(foundUser);
        assertEquals(testUser1.getId(), foundUser.getId());
        assertEquals(testUser1.getEmail(), foundUser.getEmail());
    }
    
    @Test
    @DisplayName("Select by username should return null for non-existent user")
    void testSelectByUsernameNonExistent() {
        // Act
        User foundUser = userMapper.selectByUsername("nonexistentuser");
        
        // Assert
        assertNull(foundUser);
    }
    
    @Test
    @DisplayName("Select page of users should return correct users")
    void testSelectPage() {
        // Arrange
        IPage<User> page = new Page<>(1, 10);
        
        // Act
        IPage<User> result = userMapper.selectPage(page, null);
        
        // Assert
        assertNotNull(result);
        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getTotal()).isGreaterThanOrEqualTo(2);
        
        // Verify our test users are in the results
        List<String> usernames = result.getRecords().stream()
                .map(User::getUsername)
                .toList();
        assertThat(usernames).contains(testUser1.getUsername(), testUser2.getUsername());
    }
    
    @Test
    @DisplayName("Update user should modify user data")
    void testUpdateUser() {
        // Arrange
        final String newFirstName = "UpdatedFirst";
        final String newLastName = "UpdatedLast";
        final String newEmail = "updated@example.com";
        
        User userToUpdate = userMapper.selectById(testUser1.getId());
        userToUpdate.setFirstName(newFirstName);
        userToUpdate.setLastName(newLastName);
        userToUpdate.setEmail(newEmail);
        userToUpdate.setUpdatedAt(LocalDateTime.now());
        
        // Act
        int result = userMapper.updateById(userToUpdate);
        
        // Assert
        assertEquals(1, result);
        
        // Verify the update by retrieving the user again
        User updatedUser = userMapper.selectById(testUser1.getId());
        assertEquals(newFirstName, updatedUser.getFirstName());
        assertEquals(newLastName, updatedUser.getLastName());
        assertEquals(newEmail, updatedUser.getEmail());
    }
    
    @Test
    @DisplayName("Delete user should remove user from database")
    void testDeleteUser() {
        // Act
        int result = userMapper.deleteById(testUser1.getId());
        
        // Assert
        assertEquals(1, result);
        
        // Verify user is deleted
        User deletedUser = userMapper.selectById(testUser1.getId());
        assertNull(deletedUser);
    }
    
    @Test
    @DisplayName("Search users by keyword should return matching users")
    void testSearchUsers() {
        // Arrange
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, "testuser")
                .or()
                .like(User::getEmail, "example.com");
        
        // Act
        List<User> users = userMapper.selectList(queryWrapper);
        
        // Assert
        assertThat(users).isNotEmpty();
        assertThat(users.size()).isGreaterThanOrEqualTo(2);
        
        // Verify our test users are in the results
        List<String> usernames = users.stream()
                .map(User::getUsername)
                .toList();
        assertThat(usernames).contains(testUser1.getUsername(), testUser2.getUsername());
    }
    
    @Test
    @DisplayName("Find users by role should return users with specified role")
    void testFindUsersByRole() {
        // Arrange - Admin role should only be assigned to testUser2
        
        // Act - This would normally be a custom method in the mapper
        // For demonstration, we'll use a more complex query approach
        List<User> adminUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .inSql(User::getId, 
                        "SELECT user_id FROM user_roles WHERE role_id = " + adminRole.getId()));
        
        // Assert
        assertThat(adminUsers).isNotEmpty();
        assertThat(adminUsers.size()).isEqualTo(1);
        assertThat(adminUsers.get(0).getUsername()).isEqualTo(testUser2.getUsername());
        
        // Additional test for users with basic role
        List<User> basicUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .inSql(User::getId, 
                        "SELECT user_id FROM user_roles WHERE role_id = " + userRole.getId()));
        assertThat(basicUsers.size()).isGreaterThanOrEqualTo(2);
        
        List<String> usernames = basicUsers.stream()
                .map(User::getUsername)
                .toList();
        assertThat(usernames).contains(testUser1.getUsername(), testUser2.getUsername());
    }
    
    @Test
    @DisplayName("Count users should return correct count")
    void testCountUsers() {
        // Act
        Long count = userMapper.selectCount(null);
        
        // Assert
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}