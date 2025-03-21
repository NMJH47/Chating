package com.privatedomain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatedomain.chat.model.Role;
import com.privatedomain.chat.model.User;
import com.privatedomain.chat.security.jwt.JwtUtils;
import com.privatedomain.chat.service.RoleService;
import com.privatedomain.chat.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setActive(true);
        testUser.setLocked(false);
        
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(Role.ERole.ROLE_USER.toString());
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Test
    @DisplayName("Register user with valid data should return 200 OK")
    void registerUserSuccess() throws Exception {
        // Arrange
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "password123";
        
        when(userService.existsByUsername(username)).thenReturn(false);
        when(userService.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(roleService.findByName(Role.ERole.ROLE_USER.toString())).thenReturn(Optional.of(new Role()));
        when(userService.registerUser(any(User.class))).thenReturn(testUser);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"" + username + "\","
                        + "\"email\":\"" + email + "\","
                        + "\"password\":\"" + password + "\""
                        + "}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
        
        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Register user with existing username should return 400 Bad Request")
    void registerUserExistingUsername() throws Exception {
        // Arrange
        String username = "existinguser";
        String email = "newuser@example.com";
        String password = "password123";
        
        when(userService.existsByUsername(username)).thenReturn(true);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"" + username + "\","
                        + "\"email\":\"" + email + "\","
                        + "\"password\":\"" + password + "\""
                        + "}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
        
        verify(userService, never()).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Register user with existing email should return 400 Bad Request")
    void registerUserExistingEmail() throws Exception {
        // Arrange
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password123";
        
        when(userService.existsByUsername(username)).thenReturn(false);
        when(userService.existsByEmail(email)).thenReturn(true);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"" + username + "\","
                        + "\"email\":\"" + email + "\","
                        + "\"password\":\"" + password + "\""
                        + "}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));
        
        verify(userService, never()).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Login with valid credentials should return JWT token")
    void loginUserSuccess() throws Exception {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String jwtToken = "mock.jwt.token";
        
        when(userService.authenticateUser(username, password)).thenReturn(jwtToken);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"" + username + "\","
                        + "\"password\":\"" + password + "\""
                        + "}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(jwtToken))
                .andExpect(jsonPath("$.username").value(username));
        
        verify(userService).authenticateUser(username, password);
        verify(userService).findByUsername(username);
        verify(userService).updateLastLogin(testUser.getId());
    }

    @Test
    @DisplayName("Login with non-existing user should return 401 Unauthorized")
    void loginUserNotFound() throws Exception {
        // Arrange
        String username = "nonexistentuser";
        String password = "password123";
        
        when(userService.authenticateUser(username, password))
                .thenThrow(new RuntimeException("Bad credentials"));
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"" + username + "\","
                        + "\"password\":\"" + password + "\""
                        + "}")
                )
                .andExpect(status().isUnauthorized());
        
        verify(userService).authenticateUser(username, password);
    }
    
    @Test
    @WithMockUser
    @DisplayName("Refresh token with valid token should return new JWT token")
    void refreshTokenSuccess() throws Exception {
        // Arrange
        String username = "testuser";
        String newToken = "new.jwt.token";
        
        when(jwtUtils.getUsernameFromJwtToken(anyString())).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateJwtToken(any())).thenReturn(newToken);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer valid.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(newToken));
    }
    
    @Test
    @WithMockUser
    @DisplayName("Get current user profile should return user details")
    void getCurrentUserProfile() throws Exception {
        // Arrange
        String username = "testuser";

        when(userService.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }
}