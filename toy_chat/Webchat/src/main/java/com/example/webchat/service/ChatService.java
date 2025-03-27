package com.example.webchat.service;

import com.example.webchat.entity.ChatChannel;
import com.example.webchat.entity.ChatMessage;
import com.example.webchat.entity.UserEntity;
import com.example.webchat.repository.ChatChannelRepository;
import com.example.webchat.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatChannelRepository channelRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UserService userService;

    public ChatChannel createChannel(String name, String description, Long creatorId) {
        if (channelRepository.existsByName(name)) {
            throw new RuntimeException("Channel name already exists");
        }

        UserEntity creator = userService.getUserById(creatorId);
        ChatChannel channel = new ChatChannel();
        channel.setName(name);
        channel.setDescription(description);
        channel.setCreator(creator);
        channel.getMembers().add(creator);

        return channelRepository.save(channel);
    }

    public ChatChannel joinChannel(Long channelId, Long userId) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        
        UserEntity user = userService.getUserById(userId);
        channel.getMembers().add(user);
        
        return channelRepository.save(channel);
    }

    public ChatChannel leaveChannel(Long channelId, Long userId) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        
        UserEntity user = userService.getUserById(userId);
        channel.getMembers().remove(user);
        
        return channelRepository.save(channel);
    }

    public ChatMessage sendMessage(Long channelId, Long userId, String content) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        
        UserEntity user = userService.getUserById(userId);
        
        ChatMessage message = new ChatMessage();
        message.setChannelName(channel.getName());
        message.setUser(user);
        message.setContent(content);
        
        return messageRepository.save(message);
    }

    public Page<ChatMessage> getChannelMessages(String channelName, Pageable pageable) {
        return messageRepository.findByChannelNameOrderByCreatedAtDesc(channelName, pageable);
    }

    public List<ChatChannel> getUserChannels(Long userId) {
        return channelRepository.findByMembersId(userId);
    }
} 