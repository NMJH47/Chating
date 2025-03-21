package com.privatedomain.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Private Domain Chat System.
 * This application manages real-time communication for private domain traffic.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.privatedomain.chat.repository.postgres")
@EnableMongoRepositories(basePackages = "com.privatedomain.chat.repository.mongodb")
@EntityScan(basePackages = "com.privatedomain.chat.model")
@EnableCaching
@EnableAsync
@EnableScheduling
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}