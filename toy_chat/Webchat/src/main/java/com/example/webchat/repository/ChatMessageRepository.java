package com.example.webchat.repository;

import com.example.webchat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByChannelNameOrderByCreatedAtDesc(String channelName, Pageable pageable);
} 