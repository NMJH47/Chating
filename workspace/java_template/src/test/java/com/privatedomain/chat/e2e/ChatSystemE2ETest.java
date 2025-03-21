package com.privatedomain.chat.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatedomain.chat.model.ChatMessage;
import com.privatedomain.chat.model.Group;
import com.privatedomain.chat.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public class ChatSystemE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private WebDriver driver;
    private WebDriverWait wait;
    private WebSocketStompClient stompClient;
    private String baseUrl;
    private String wsUrl;

    // Test data
    private static final String TEST_USERNAME_1 = "e2euser1";
    private static final String TEST_USERNAME_2 = "e2euser2";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_EMAIL = "e2e@example.com";
    private static final String TEST_GROUP_NAME = "E2E Test Group";
    
    // WebSocket session and message handlers
    private StompSession stompSession;
    private CompletableFuture<ChatMessage> completableFuture;

    @BeforeEach
    void setUp() {
        // Setup WebDriver
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Setup URLs
        baseUrl = "http://localhost:" + port;
        wsUrl = "ws://localhost:" + port + "/ws";
        
        // Setup WebSocket client
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        // Initialize message handler
        completableFuture = new CompletableFuture<>();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    @Test
    @DisplayName("End-to-end test: Register, login, create group, send message")
    void testCompleteUserJourney() throws Exception {
        // Step 1: Register two users
        registerUser(TEST_USERNAME_1, TEST_EMAIL, TEST_PASSWORD);
        registerUser(TEST_USERNAME_2, TEST_EMAIL + ".2", TEST_PASSWORD);
        
        // Step 2: Login with first user
        String userToken = loginUser(TEST_USERNAME_1, TEST_PASSWORD);
        assertThat(userToken).isNotEmpty();
        
        // Step 3: Create a chat group
        Group group = createChatGroup(userToken, TEST_GROUP_NAME, "Test group for E2E testing");
        assertThat(group.getId()).isNotNull();
        
        // Step 4: Connect to WebSocket
        connectToWebSocket();
        
        // Step 5: Subscribe to group topic
        subscribeToGroup(group.getId());
        
        // Step 6: Send a message to the group
        String messageContent = "Hello, this is an E2E test message!";
        sendMessageToGroup(userToken, group.getId(), messageContent);
        
        // Step 7: Verify message was received via WebSocket
        ChatMessage receivedMessage = completableFuture.get(5, TimeUnit.SECONDS);
        assertThat(receivedMessage.getContent()).isEqualTo(messageContent);
        
        // Step 8: Verify UI updates (simulate UI check in headless mode)
        // Note: In a real test, you would have a running frontend
        // This is a simplified simulation of checking the UI
        driver.get(baseUrl + "/chat/" + group.getId());
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".message-list")));
        
        // Step 9: Add the second user to the group
        addUserToGroup(userToken, group.getId(), TEST_USERNAME_2);
        
        // Step 10: Check group members
        List<User> groupMembers = getGroupMembers(userToken, group.getId());
        assertThat(groupMembers.size()).isEqualTo(2);
        assertThat(groupMembers).extracting("username")
                .contains(TEST_USERNAME_1, TEST_USERNAME_2);
    }

    // Helper methods for the test steps
    
    private void registerUser(String username, String email, String password) {
        driver.get(baseUrl + "/register");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).sendKeys(password);
        driver.findElement(By.id("registerButton")).click();
        
        wait.until(ExpectedConditions.urlToBe(baseUrl + "/login"));
    }
    
    private String loginUser(String username, String password) {
        // In a headless test, we can simulate API calls directly
        try {
            // Create REST client
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
            
            // Create login request
            String loginJson = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
            java.net.http.HttpRequest loginRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();
            
            // Send request and get response
            java.net.http.HttpResponse<String> loginResponse = client.send(loginRequest, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            // Parse token from response
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(loginResponse.body());
            return jsonNode.get("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to login user", e);
        }
    }
    
    private Group createChatGroup(String token, String groupName, String description) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
            
            String groupJson = String.format("{\"name\":\"%s\",\"description\":\"%s\",\"isPrivate\":false}", 
                    groupName, description);
            java.net.http.HttpRequest createGroupRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/api/groups"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(groupJson))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(createGroupRequest, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            return objectMapper.readValue(response.body(), Group.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create chat group", e);
        }
    }
    
    private void connectToWebSocket() throws ExecutionException, InterruptedException, TimeoutException {
        stompSession = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }
    
    private void subscribeToGroup(Long groupId) {
        stompSession.subscribe("/topic/group/" + groupId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completableFuture.complete((ChatMessage) payload);
            }
        });
    }
    
    private void sendMessageToGroup(String token, Long groupId, String content) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
            
            String messageJson = String.format(
                    "{\"content\":\"%s\",\"groupId\":%d,\"type\":\"CHAT\"}", 
                    content, groupId);
            
            java.net.http.HttpRequest sendMessageRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/api/messages"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(messageJson))
                    .build();
            
            client.send(sendMessageRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }
    
    private void addUserToGroup(String token, Long groupId, String username) {
        try {
            // First get the user ID from username
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
            
            java.net.http.HttpRequest getUserRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/api/users/search?keyword=" + username))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<String> userResponse = client.send(getUserRequest, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            com.fasterxml.jackson.core.type.TypeReference<List<User>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
            List<User> users = objectMapper.readValue(userResponse.body(), typeRef);
            
            Long userId = users.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"))
                    .getId();
            
            // Add user to group
            String addUserJson = String.format("{\"userId\":%d}", userId);
            java.net.http.HttpRequest addUserRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/api/groups/" + groupId + "/users"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(addUserJson))
                    .build();
            
            client.send(addUserRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to add user to group", e);
        }
    }
    
    private List<User> getGroupMembers(String token, Long groupId) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
            
            java.net.http.HttpRequest getMembersRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/api/groups/" + groupId + "/users"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<String> membersResponse = client.send(getMembersRequest, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            com.fasterxml.jackson.core.type.TypeReference<List<User>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
            return objectMapper.readValue(membersResponse.body(), typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get group members", e);
        }
    }
}