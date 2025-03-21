package com.privatedomain.chat.model;

/**
 * Enum representing the type of message in the chat system.
 */
public enum MessageType {
    TEXT,       // Regular text message
    IMAGE,      // Image message
    VIDEO,      // Video message
    AUDIO,      // Audio message
    FILE,       // File/document message
    LOCATION,   // Location sharing message
    SYSTEM      // System notification or event
}