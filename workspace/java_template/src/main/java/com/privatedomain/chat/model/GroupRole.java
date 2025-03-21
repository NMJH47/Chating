package com.privatedomain.chat.model;

/**
 * Enum representing the role of a user in a group.
 */
public enum GroupRole {
    OWNER,      // Group creator with all permissions
    ADMIN,      // Administrator with management permissions
    MODERATOR,  // Can moderate content but cannot change group settings
    MEMBER      // Regular member with basic permissions
}