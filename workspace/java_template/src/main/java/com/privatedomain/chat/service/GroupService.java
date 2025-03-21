package com.privatedomain.chat.service;

import com.privatedomain.chat.model.Group;
import com.privatedomain.chat.model.GroupMember;
import com.privatedomain.chat.model.GroupRole;
import com.privatedomain.chat.model.User;
import com.privatedomain.chat.repository.postgres.GroupMemberRepository;
import com.privatedomain.chat.repository.postgres.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing chat groups and group members.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;
    private final WebSocketService webSocketService;

    /**
     * Create a new chat group.
     *
     * @param name Group name
     * @param description Group description
     * @param avatar Group avatar URL
     * @param isPrivate Whether the group is private
     * @param maxMembers Maximum number of members
     * @param ownerId ID of the user creating the group
     * @return Created group
     */
    @Transactional
    public Group createGroup(String name, String description, String avatar, 
                            Boolean isPrivate, Integer maxMembers, Long ownerId) {
        Group group = Group.builder()
                .name(name)
                .description(description)
                .avatar(avatar)
                .isPrivate(isPrivate)
                .maxMembers(maxMembers != null ? maxMembers : 200)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();

        group = groupRepository.save(group);

        // Add owner as a member with OWNER role
        GroupMember ownerMember = GroupMember.builder()
                .groupId(group.getId())
                .userId(ownerId)
                .role(GroupRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .lastRead(LocalDateTime.now())
                .build();

        groupMemberRepository.save(ownerMember);

        return group;
    }

    /**
     * Get a group by ID.
     *
     * @param id Group ID
     * @return Optional containing the group if found
     */
    @Cacheable(value = "groups", key = "#id")
    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    /**
     * Update a group's information.
     *
     * @param id Group ID
     * @param name New name (optional)
     * @param description New description (optional)
     * @param avatar New avatar URL (optional)
     * @param isPrivate New privacy setting (optional)
     * @param maxMembers New maximum members (optional)
     * @param currentUserId ID of the user making the update
     * @return Updated group
     */
    @Transactional
    @CacheEvict(value = "groups", key = "#id")
    public Group updateGroup(Long id, String name, String description, String avatar,
                            Boolean isPrivate, Integer maxMembers, Long currentUserId) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user has permission to update group
        GroupRole role = getGroupRole(id, currentUserId);
        if (role != GroupRole.OWNER && role != GroupRole.ADMIN) {
            throw new RuntimeException("You don't have permission to update this group");
        }

        if (name != null) {
            group.setName(name);
        }
        
        if (description != null) {
            group.setDescription(description);
        }
        
        if (avatar != null) {
            group.setAvatar(avatar);
        }
        
        if (isPrivate != null) {
            group.setIsPrivate(isPrivate);
        }
        
        if (maxMembers != null) {
            int currentMembers = groupRepository.countMembersByGroupId(id);
            if (maxMembers < currentMembers) {
                throw new RuntimeException("Cannot reduce max members below current member count");
            }
            group.setMaxMembers(maxMembers);
        }

        Group updatedGroup = groupRepository.save(group);

        // Notify group members about the update
        webSocketService.sendToGroup(id, Map.of(
            "type", "GROUP_UPDATED",
            "groupId", id,
            "updatedBy", currentUserId
        ));

        return updatedGroup;
    }

    /**
     * Delete a group.
     *
     * @param id Group ID
     * @param currentUserId ID of the user requesting deletion
     * @return true if group was deleted
     */
    @Transactional
    @CacheEvict(value = "groups", key = "#id")
    public boolean deleteGroup(Long id, Long currentUserId) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Only owner can delete a group
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new RuntimeException("Only the group owner can delete a group");
        }

        // Notify members before deletion
        List<GroupMember> members = groupMemberRepository.findByGroupId(id);
        List<Long> memberIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());

        // Delete group
        groupRepository.delete(group);
        
        // Notify members about group deletion
        for (Long memberId : memberIds) {
            webSocketService.sendToUser(memberId, Map.of(
                "type", "GROUP_DELETED",
                "groupId", id,
                "deletedBy", currentUserId
            ));
        }

        return true;
    }

    /**
     * Add a member to a group.
     *
     * @param groupId Group ID
     * @param userId User ID to add
     * @param role Role to assign to the new member
     * @param addedByUserId ID of the user adding the new member
     * @return Created group member
     */
    @Transactional
    public GroupMember addMember(Long groupId, Long userId, GroupRole role, Long addedByUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check permission
        GroupRole currentUserRole = getGroupRole(groupId, addedByUserId);
        if (currentUserRole != GroupRole.OWNER && currentUserRole != GroupRole.ADMIN) {
            throw new RuntimeException("You don't have permission to add members");
        }

        // Check if user is already a member
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("User is already a member of this group");
        }

        // Check member limit
        int currentMemberCount = groupRepository.countMembersByGroupId(groupId);
        if (currentMemberCount >= group.getMaxMembers()) {
            throw new RuntimeException("Group has reached maximum member limit");
        }

        // Create new member
        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .lastRead(LocalDateTime.now())
                .build();

        GroupMember savedMember = groupMemberRepository.save(member);

        // Notify group about new member
        webSocketService.sendToGroup(groupId, Map.of(
            "type", "MEMBER_JOINED",
            "groupId", groupId,
            "userId", userId,
            "role", role.name(),
            "addedBy", addedByUserId
        ));

        return savedMember;
    }

    /**
     * Remove a member from a group.
     *
     * @param groupId Group ID
     * @param userId User ID to remove
     * @param removedByUserId ID of the user requesting the removal
     * @return true if member was removed
     */
    @Transactional
    public boolean removeMember(Long groupId, Long userId, Long removedByUserId) {
        // Cannot remove group owner
        Optional<GroupRole> targetUserRole = groupMemberRepository.findRoleByGroupIdAndUserId(groupId, userId);
        if (targetUserRole.isPresent() && targetUserRole.get() == GroupRole.OWNER) {
            throw new RuntimeException("Cannot remove the group owner");
        }

        // Check if removing yourself
        boolean removingSelf = userId.equals(removedByUserId);
        
        // If not removing self, check permissions
        if (!removingSelf) {
            GroupRole currentUserRole = getGroupRole(groupId, removedByUserId);
            
            // Admins can only remove regular members
            if (currentUserRole == GroupRole.ADMIN && targetUserRole.isPresent() && 
                (targetUserRole.get() == GroupRole.ADMIN || targetUserRole.get() == GroupRole.MODERATOR)) {
                throw new RuntimeException("Admins can only remove regular members or moderators");
            }
            
            // Moderators can only remove regular members
            if (currentUserRole == GroupRole.MODERATOR && targetUserRole.isPresent() &&
                targetUserRole.get() != GroupRole.MEMBER) {
                throw new RuntimeException("Moderators can only remove regular members");
            }
            
            // Regular members cannot remove other members
            if (currentUserRole == GroupRole.MEMBER) {
                throw new RuntimeException("You don't have permission to remove members");
            }
        }

        // Remove member
        int result = groupMemberRepository.removeUserFromGroup(groupId, userId);

        if (result > 0) {
            // Notify group about member removal
            webSocketService.sendToGroup(groupId, Map.of(
                "type", removingSelf ? "MEMBER_LEFT" : "MEMBER_REMOVED",
                "groupId", groupId,
                "userId", userId,
                "removedBy", removedByUserId
            ));
            
            // Also notify the removed user
            webSocketService.sendToUser(userId, Map.of(
                "type", removingSelf ? "LEFT_GROUP" : "REMOVED_FROM_GROUP",
                "groupId", groupId,
                "removedBy", removedByUserId
            ));
            
            return true;
        }
        
        return false;
    }

    /**
     * Update a member's role in a group.
     *
     * @param groupId Group ID
     * @param userId User ID 
     * @param newRole New role to assign
     * @param updatedByUserId ID of the user updating the role
     * @return Updated group member
     */
    @Transactional
    public GroupMember updateMemberRole(Long groupId, Long userId, GroupRole newRole, Long updatedByUserId) {
        // Validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Cannot change owner's role
        if (group.getOwnerId().equals(userId)) {
            throw new RuntimeException("Cannot change the group owner's role");
        }

        // Check if member exists
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        // Check permissions
        GroupRole currentUserRole = getGroupRole(groupId, updatedByUserId);
        
        // Only owner can promote to admin
        if (newRole == GroupRole.ADMIN && currentUserRole != GroupRole.OWNER) {
            throw new RuntimeException("Only the group owner can promote members to admin");
        }
        
        // Only owner and admins can change roles
        if (currentUserRole != GroupRole.OWNER && currentUserRole != GroupRole.ADMIN) {
            throw new RuntimeException("You don't have permission to change member roles");
        }

        // Update role
        member.setRole(newRole);
        GroupMember updatedMember = groupMemberRepository.save(member);

        // Notify group about role change
        webSocketService.sendToGroup(groupId, Map.of(
            "type", "ROLE_CHANGED",
            "groupId", groupId,
            "userId", userId,
            "newRole", newRole.name(),
            "updatedBy", updatedByUserId
        ));
        
        // Also notify the user whose role changed
        webSocketService.sendToUser(userId, Map.of(
            "type", "YOUR_ROLE_CHANGED",
            "groupId", groupId,
            "newRole", newRole.name(),
            "updatedBy", updatedByUserId
        ));

        return updatedMember;
    }

    /**
     * Get all members of a group.
     *
     * @param groupId Group ID
     * @return List of group members
     */
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    /**
     * Get a user's role in a group.
     *
     * @param groupId Group ID
     * @param userId User ID
     * @return User's role in the group
     */
    public GroupRole getGroupRole(Long groupId, Long userId) {
        return groupMemberRepository.findRoleByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
    }

    /**
     * Check if a user is a member of a group.
     *
     * @param groupId Group ID
     * @param userId User ID
     * @return true if user is a member
     */
    public boolean isGroupMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    /**
     * Get all groups a user is a member of.
     *
     * @param userId User ID
     * @return List of groups
     */
    public List<Group> getGroupsByUserId(Long userId) {
        return groupRepository.findGroupsByUserId(userId);
    }

    /**
     * Get recent active groups for a user.
     *
     * @param userId User ID
     * @param limit Maximum number of groups to return
     * @return List of recently active groups
     */
    public List<Group> getRecentActiveGroups(Long userId, int limit) {
        return groupRepository.findRecentActiveGroupsByUserId(userId, limit);
    }

    /**
     * Update last read timestamp for a user in a group.
     *
     * @param groupId Group ID
     * @param userId User ID
     * @return true if timestamp was updated
     */
    @Transactional
    public boolean updateLastRead(Long groupId, Long userId) {
        int result = groupMemberRepository.updateLastRead(groupId, userId, LocalDateTime.now());
        return result > 0;
    }
}