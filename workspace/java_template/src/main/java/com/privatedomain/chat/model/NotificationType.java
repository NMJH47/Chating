package com.privatedomain.chat.model;

/**
 * Enum representing different types of notifications in the chat system.
 */
public enum NotificationType {
    NEW_MESSAGE,        // New message received
    GROUP_INVITATION,   // Invitation to join a group
    GROUP_UPDATE,       // Group details updated
    MEMBER_JOINED,      // New member joined a group
    MEMBER_LEFT,        // Member left a group
    ROLE_CHANGED,       // User role changed in a group
    SYSTEM_ALERT,       // System alert or announcement
    MENTION             // User mentioned in a message
}