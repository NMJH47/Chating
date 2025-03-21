package com.privatedomain.chat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatedomain.chat.ChatApplication;
import com.privatedomain.chat.model.Role;
import com.privatedomain.chat.model.User;
import com.privatedomain.chat.service.RoleService;
import com.privatedomain.chat.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = ChatApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleService roleService;
    
    private static final String TEST_USERNAME = "integrationtestuser";
    private static final String TEST_EMAIL = "integration@example.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static String jwtToken;
    
    @BeforeEach
    void setUp() {
        // Create roles if they don't exist
        if (roleService.findByName(Role.ERole.ROLE_USER.toString()).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(Role.ERole.ROLE_USER.toString());
            roleService.save(userRole);
        }
        
        if (roleService.findByName(Role.ERole.ROLE_ADMIN.toString()).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(Role.ERole.ROLE_ADMIN.toString());
            roleService.save(adminRole);
        }
        
        // Clean up test user if exists
        Optional<User> existingUser = userService.findByUsername(TEST_USERNAME);
        if (existingUser.isPresent()) {
            // In a real application, we would delete the user here
            // For this test, we'll just use the existing user
        }
    }
    
    @Test
    @DisplayName("Full authentication flow - register, login, and access protected resources")
    void testFullAuthenticationFlow() throws Exception {
        // Step 1: Register a new user
        if (!userService.existsByUsername(TEST_USERNAME)) {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{"
                            + "\"username\":\"" + TEST_USERNAME + "\","
                            + "\"email\":\"" + TEST_EMAIL + "\","
                            + "\"password\":\"" + TEST_PASSWORD + "\""
                            + "}")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("User registered successfully")))
                    .andDo(MockMvcResultHandlers.print());
            
            // Verify user is created in the database
            Optional<User> newUser = userService.findByUsername(TEST_USERNAME);
            assertTrue(newUser.isPresent(), "User should be created in the database");
        }
        
        // Step 2: Login with the newly created user
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"" + TEST_USERNAME + "\","
                        + "\"password\":\"" + TEST_PASSWORD + "\""
                        + "}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        
        // Extract the JWT token from the response
        String responseContent = loginResult.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(responseContent).get("token").asText();
        assertNotNull(jwtToken, "JWT token should not be null");
        
        // Step 3: Access protected user profile endpoint with JWT token
        mockMvc.perform(get("/api/auth/profile")
                .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.email", is(TEST_EMAIL)))
                .andDo(MockMvcResultHandlers.print());
        
        // Step 4: Test token refresh
        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andDo(MockMvcResultHandlers.print());
        
        // Step 5: Try accessing protected endpoint without token (should fail)
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andDo(MockMvcResultHandlers.print());
        
        // Step 6: Try accessing protected endpoint with invalid token (should fail)
        mockMvc.perform(get("/api/auth/profile")
                .header("Authorization", "Bearer invalidtoken")
                )
                .andExpect(status().isUnauthorized())
                .andDo(MockMvcResultHandlers.print());
    }
    
    @Test
    @DisplayName("Register validation - empty fields")
    void testRegisterValidationEmptyFields() throws Exception {
        // Test registration with empty username
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"\","
                        + "\"email\":\"valid@example.com\","
                        + "\"password\":\"Password123!\""
                        + "}")
                )
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
        
        // Test registration with empty email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"validuser\","
                        + "\"email\":\"\","
                        + "\"password\":\"Password123!\""
                        + "}")
                )
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
        
        // Test registration with empty password
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"validuser\","
                        + "\"email\":\"valid@example.com\","
                        + "\"password\":\"\""
                        + "}")
                )
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }
    
    @Test
    @DisplayName("Register validation - invalid email format")
    void testRegisterValidationInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"validuser\","
                        + "\"email\":\"invalid-email-format\","
                        + "\"password\":\"Password123!\""
                        + "}")
                )
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }
    
    @Test
    @DisplayName("Login validation - invalid credentials")
    void testLoginValidationInvalidCredentials() throws Exception {
        // Test login with non-existent user
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"username\":\"nonexistentuser\","
                        + "\"password\":\"Password123!\""
                        + "}")
                )
                .andExpect(status().isUnauthorized())
                .andDo(MockMvcResultHandlers.print());
        
        // Test login with wrong password (assuming user exists)
        if (userService.existsByUsername(TEST_USERNAME)) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{"
                            + "\"username\":\"" + TEST_USERNAME + "\","
                            + "\"password\":\"wrongpassword\""
                            + "}")
                    )
                    .andExpect(status().isUnauthorized())
                    .andDo(MockMvcResultHandlers.print());
        }
    }
}