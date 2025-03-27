package com.example.webchat.controller;

import com.example.webchat.entity.ChatChannel;
import com.example.webchat.entity.ChatMessage;
import com.example.webchat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping("/channels")
    public ResponseEntity<?> createChannel(@RequestBody Map<String, String> request) {
        try {
            ChatChannel channel = chatService.createChannel(
                request.get("name"),
                request.get("description"),
                Long.parseLong(request.get("creatorId"))
            );
            return ResponseEntity.ok(channel);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/channels/{channelId}/join")
    public ResponseEntity<?> joinChannel(
            @PathVariable Long channelId,
            @RequestBody Map<String, String> request) {
        try {
            ChatChannel channel = chatService.joinChannel(
                channelId,
                Long.parseLong(request.get("userId"))
            );
            return ResponseEntity.ok(channel);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/channels/{channelId}/leave")
    public ResponseEntity<?> leaveChannel(
            @PathVariable Long channelId,
            @RequestBody Map<String, String> request) {
        try {
            ChatChannel channel = chatService.leaveChannel(
                channelId,
                Long.parseLong(request.get("userId"))
            );
            return ResponseEntity.ok(channel);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long channelId,
            @RequestBody Map<String, String> request) {
        try {
            ChatMessage message = chatService.sendMessage(
                channelId,
                Long.parseLong(request.get("userId")),
                request.get("content")
            );
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/channels/{channelName}/messages")
    public ResponseEntity<?> getChannelMessages(
            @PathVariable String channelName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ChatMessage> messages = chatService.getChannelMessages(
                channelName,
                PageRequest.of(page, size)
            );
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users/{userId}/channels")
    public ResponseEntity<?> getUserChannels(@PathVariable Long userId) {
        try {
            List<ChatChannel> channels = chatService.getUserChannels(userId);
            return ResponseEntity.ok(channels);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 