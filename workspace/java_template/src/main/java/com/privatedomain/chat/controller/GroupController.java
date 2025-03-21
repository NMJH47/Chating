package com.privatedomain.chat.controller;

import com.privatedomain.chat.model.Group;
import com.privatedomain.chat.model.GroupMember;
import com.privatedomain.chat.model.GroupRole;
import com.privatedomain.chat.security.UserDetailsImpl;
import com.privatedomain.chat.service.GroupService;
import com.privatedomain.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing chat groups.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    /**
     * DTO for creating a group.
     */
    public static class CreateGroupRequest {
        @NotBlank
        @Size(min = 3, max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private Boolean isPrivate;

        private Integer maxMembers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getIsPrivate() {
            return isPrivate;
        }

        public void setIsPrivate(Boolean isPrivate) {
            this.isPrivate = isPrivate;
        }

        public Integer getMaxMembers() {
            return maxMembers;
        }

        public void setMaxMembers(Integer maxMembers) {
            this.maxMembers = maxMembers;
        }
    }

    /**
     * DTO for updating a group.
     */
    public static class UpdateGroupRequest {
        @Size(min = 3, max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private Boolean isPrivate;

        private Integer maxMembers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getIsPrivate() {
            return isPrivate;
        }

        public void setIsPrivate(Boolean isPrivate) {
            this.isPrivate = isPrivate;
        }

        public Integer getMaxMembers() {
            return maxMembers;
        }

        public void setMaxMembers(Integer maxMembers) {
            this.maxMembers = maxMembers;
        }
    }

    /**
     * DTO for adding a member to a group.
     */
    public static class AddMemberRequest {
        @NotBlank
        private Long userId;

        private GroupRole role;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public GroupRole getRole() {
            return role;
        }

        public void setRole(GroupRole role) {
            this.role = role;
        }
    }

    /**
     * DTO for updating a member's role.
     */
    public static class UpdateMemberRoleRequest {
        @NotBlank
        private GroupRole role;

        public GroupRole getRole() {
            return role;
        }

        public void setRole(GroupRole role) {
            this.role = role;
        }
    }

    /**
     * Create a new group.
     *
     * @param userDetails Authenticated user details
     * @param request Group creation request
     * @return Created group
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createGroup(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateGroupRequest request) {
        try {
            Group group = groupService.createGroup(
                    request.getName(),
                    request.getDescription(),
                    null, // No avatar yet
                    request.getIsPrivate(),
                    request.getMaxMembers(),
                    userDetails.getId()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        } catch (Exception e) {
            log.error("Error creating group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating group: " + e.getMessage()));
        }
    }

    /**
     * Get a group by ID.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @return Group information
     */
    @GetMapping("/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getGroupById(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Group group = groupService.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            // Check if user is allowed to view this group
            if (group.getIsPrivate() && !groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You don't have access to this group"));
            }

            return ResponseEntity.ok(group);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving group: " + e.getMessage()));
        }
    }

    /**
     * Update a group.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @param request Update request
     * @return Updated group
     */
    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateGroupRequest request) {
        try {
            Group updatedGroup = groupService.updateGroup(
                    groupId,
                    request.getName(),
                    request.getDescription(),
                    null, // Avatar updated separately
                    request.getIsPrivate(),
                    request.getMaxMembers(),
                    userDetails.getId()
            );

            return ResponseEntity.ok(updatedGroup);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating group: " + e.getMessage()));
        }
    }

    /**
     * Upload a group avatar.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @param file Avatar image file
     * @return Success response
     */
    @PostMapping("/{groupId}/avatar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateGroupAvatar(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Please select a file to upload"));
            }

            // Check if user has permission
            GroupRole role = groupService.getGroupRole(groupId, userDetails.getId());
            if (role != GroupRole.OWNER && role != GroupRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You don't have permission to update group avatar"));
            }

            // Save the file
            String uploadDir = "./uploads/groups/";
            Files.createDirectories(Paths.get(uploadDir));

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "group_" + groupId + "_" + UUID.randomUUID() + fileExtension;
            Path targetPath = Paths.get(uploadDir + newFilename);

            Files.copy(file.getInputStream(), targetPath);

            // Update group avatar in database
            Group updatedGroup = groupService.updateGroup(
                    groupId,
                    null,
                    null,
                    "/uploads/groups/" + newFilename,
                    null,
                    null,
                    userDetails.getId()
            );

            return ResponseEntity.ok(Map.of(
                    "avatar", updatedGroup.getAvatar(),
                    "message", "Group avatar updated successfully"
            ));
        } catch (IOException e) {
            log.error("Error uploading group avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error uploading avatar: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating group avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating avatar: " + e.getMessage()));
        }
    }

    /**
     * Delete a group.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            boolean deleted = groupService.deleteGroup(groupId, userDetails.getId());
            
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to delete group"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting group: " + e.getMessage()));
        }
    }

    /**
     * Get all members in a group.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @return List of group members
     */
    @GetMapping("/{groupId}/members")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getGroupMembers(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if user is a member of the group
            if (!groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You must be a member of the group to view members"));
            }

            List<GroupMember> members = groupService.getGroupMembers(groupId);
            List<Map<String, Object>> memberInfoList = members.stream().map(member -> {
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("userId", member.getUserId());
                memberInfo.put("role", member.getRole());
                memberInfo.put("joinedAt", member.getJoinedAt());
                
                // Add basic user info
                userService.findById(member.getUserId()).ifPresent(user -> {
                    memberInfo.put("username", user.getUsername());
                    memberInfo.put("avatar", user.getAvatar());
                });
                
                return memberInfo;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(memberInfoList);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving group members", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving group members: " + e.getMessage()));
        }
    }

    /**
     * Add a member to a group.
     *
     * @param groupId Group ID
     * @param request Add member request
     * @param userDetails Authenticated user details
     * @return Added member information
     */
    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Default role is MEMBER if not specified
            GroupRole role = (request.getRole() != null) ? request.getRole() : GroupRole.MEMBER;
            
            GroupMember member = groupService.addMember(groupId, request.getUserId(), role, userDetails.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupId", member.getGroupId());
            response.put("userId", member.getUserId());
            response.put("role", member.getRole());
            response.put("joinedAt", member.getJoinedAt());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding member to group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error adding member: " + e.getMessage()));
        }
    }

    /**
     * Remove a member from a group.
     *
     * @param groupId Group ID
     * @param userId User ID to remove
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            boolean removed = groupService.removeMember(groupId, userId, userDetails.getId());
            
            if (removed) {
                return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to remove member"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error removing member from group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error removing member: " + e.getMessage()));
        }
    }

    /**
     * Update a member's role in a group.
     *
     * @param groupId Group ID
     * @param userId User ID to update
     * @param request Role update request
     * @param userDetails Authenticated user details
     * @return Updated member information
     */
    @PutMapping("/{groupId}/members/{userId}/role")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            GroupMember updatedMember = groupService.updateMemberRole(
                    groupId,
                    userId,
                    request.getRole(),
                    userDetails.getId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupId", updatedMember.getGroupId());
            response.put("userId", updatedMember.getUserId());
            response.put("role", updatedMember.getRole());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating member role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating role: " + e.getMessage()));
        }
    }

    /**
     * Get public groups with pagination.
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of public groups
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Group> publicGroups = groupService.findPublicGroups(pageable);
            return ResponseEntity.ok(publicGroups);
        } catch (Exception e) {
            log.error("Error retrieving public groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving public groups: " + e.getMessage()));
        }
    }

    /**
     * Leave a group.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @PostMapping("/{groupId}/leave")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> leaveGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            boolean left = groupService.removeMember(groupId, userDetails.getId(), userDetails.getId());
            
            if (left) {
                return ResponseEntity.ok(Map.of("message", "Left group successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to leave group"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error leaving group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error leaving group: " + e.getMessage()));
        }
    }

    /**
     * Join a public group.
     *
     * @param groupId Group ID
     * @param userDetails Authenticated user details
     * @return Success response
     */
    @PostMapping("/{groupId}/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> joinGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Check if group is public
            Group group = groupService.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            
            if (group.getIsPrivate()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Cannot directly join a private group"));
            }
            
            // Check if already a member
            if (groupService.isGroupMember(groupId, userDetails.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "You are already a member of this group"));
            }
            
            // Add as a member
            GroupMember member = groupService.addMember(
                    groupId,
                    userDetails.getId(),
                    GroupRole.MEMBER,
                    userDetails.getId() // Self-joining
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupId", member.getGroupId());
            response.put("userId", member.getUserId());
            response.put("role", member.getRole());
            response.put("joinedAt", member.getJoinedAt());
            response.put("message", "Joined group successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error joining group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error joining group: " + e.getMessage()));
        }
    }
}