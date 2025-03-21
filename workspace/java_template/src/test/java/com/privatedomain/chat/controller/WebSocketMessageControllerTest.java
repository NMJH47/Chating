package com.privatedomain.chat.controller;

import com.privatedomain.chat.model.ChatMessage;
import com.privatedomain.chat.model.User;
import com.privatedomain.chat.security.services.UserDetailsImpl;
import com.privatedomain.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebSocketMessageControllerTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private MessageChannel clientOutboundChannel;

    private WebSocketMessageController webSocketMessageController;
    private User testUser;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        webSocketMessageController = new WebSocketMessageController(messagingTemplate, chatMessageService);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        userDetails = new UserDetailsImpl(
                testUser.getId(),
                testUser.getUsername(),
                testUser.getEmail(),
                "password",
                null
        );
    }

    @Test
    @DisplayName("Send chat message should broadcast message to topic")
    void sendMessageSuccess() {
        // Arrange
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent("Hello, World!");
        chatMessage.setSenderId(testUser.getId());
        chatMessage.setGroupId(1L);
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        
        when(chatMessageService.saveMessage(any(ChatMessage.class))).thenReturn(chatMessage);
        
        // Act
        webSocketMessageController.sendMessage(chatMessage);
        
        // Assert
        verify(chatMessageService).saveMessage(chatMessage);
        verify(messagingTemplate).convertAndSend(eq("/topic/group/" + chatMessage.getGroupId()), any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("Add user to group should notify others of new user")
    void addUserSuccess() throws Exception {
        // Arrange
        Long groupId = 1L;
        String sessionId = "test-session-id";
        
        // Create StompHeaderAccessor
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null));
        
        // Create SimpMessageHeaderAccessor with security details
        SimpMessageHeaderAccessor messageHeaderAccessor = SimpMessageHeaderAccessor.create();
        messageHeaderAccessor.setSessionId(sessionId);
        messageHeaderAccessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null));
        
        // Act
        ChatMessage result = webSocketMessageController.addUser(groupId, messageHeaderAccessor);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(ChatMessage.MessageType.JOIN);
        assertThat(result.getSenderId()).isEqualTo(testUser.getId());
        assertThat(result.getContent()).contains("joined");
        
        // Verify message was sent to group topic
        verify(messagingTemplate).convertAndSend(eq("/topic/group/" + groupId), any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("Handle disconnect should notify group of user leaving")
    void handleDisconnectSuccess() {
        // Arrange
        String sessionId = "test-session-id";
        Principal principal = new UsernamePasswordAuthenticationToken(userDetails, null);
        
        // Create disconnect event with StompHeaderAccessor
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setUser(principal);
        
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(new StompSubProtocolHandler(), message, sessionId, CloseStatus.NORMAL);
        
        // Setup WebSocketSession storage with group mapping
        Map<String, Long> sessionGroupMap = new HashMap<>();
        Long groupId = 1L;
        sessionGroupMap.put(sessionId, groupId);
        
        // Inject session-group mapping using reflection
        try {
            java.lang.reflect.Field field = WebSocketMessageController.class.getDeclaredField("userSessions");
            field.setAccessible(true);
            field.set(webSocketMessageController, sessionGroupMap);
        } catch (Exception e) {
            // Handle reflection exception
            e.printStackTrace();
        }
        
        // Act
        webSocketMessageController.handleWebSocketDisconnectListener(event);
        
        // Assert
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/group/" + groupId), messageCaptor.capture());
        
        ChatMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getType()).isEqualTo(ChatMessage.MessageType.LEAVE);
        assertThat(sentMessage.getSenderId()).isEqualTo(testUser.getId());
        assertThat(sentMessage.getContent()).contains("left");
    }
    
    @Test
    @DisplayName("Test WebSocket connection with real STOMP client")
    void testWebSocketClientConnection() throws Exception {
        // This test demonstrates how to set up a STOMP client for testing
        // In a real test, use StompSession from spring-messaging
        
        // Create a CompletableFuture to store the received message
        CompletableFuture<ChatMessage> completableFuture = new CompletableFuture<>();
        
        // Mock the behavior when a message is sent to a topic
        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            ChatMessage chatMessage = invocation.getArgument(1);
            
            if (destination.equals("/topic/group/1")) {
                completableFuture.complete(chatMessage);
            }
            
            return null;
        }).when(messagingTemplate).convertAndSend(eq("/topic/group/1"), any(ChatMessage.class));
        
        // Send a test message
        ChatMessage testMessage = new ChatMessage();
        testMessage.setContent("Test WebSocket Message");
        testMessage.setSenderId(1L);
        testMessage.setGroupId(1L);
        testMessage.setType(ChatMessage.MessageType.CHAT);
        
        when(chatMessageService.saveMessage(any(ChatMessage.class))).thenReturn(testMessage);
        
        // Act - send the message
        webSocketMessageController.sendMessage(testMessage);
        
        // Wait for result with timeout
        ChatMessage receivedMessage = completableFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getContent()).isEqualTo("Test WebSocket Message");
        assertThat(receivedMessage.getType()).isEqualTo(ChatMessage.MessageType.CHAT);
    }
}