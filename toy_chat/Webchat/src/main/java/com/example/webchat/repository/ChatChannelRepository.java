package com.example.webchat.repository;

import com.example.webchat.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {
    List<ChatChannel> findByMembersId(Long userId);
    boolean existsByName(String name);
} 