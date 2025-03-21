package com.privatedomain.chat.controller;

import com.privatedomain.chat.model.User;
import com.privatedomain.chat.security.UserDetailsImpl;
import com.privatedomain.chat.service.GroupService;
import com.privatedomain.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for user management operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final GroupService groupService;

    /**
     * DTO for profile update.
     */
    public static class UpdateProfileRequest {
        @Email
        private String email;
        
        @Size(max = 20)
        private String phone;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    /**
     * DTO for password reset.
     */
    public static class PasswordResetRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 6, max = 40)
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * Get current user profile.
     *
     * @param userDetails Authenticated user details
     * @return User profile information
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userService.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create a map with only the fields we want to expose
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("id", user.getId());
            userProfile.put("username", user.getUsername());
            userProfile.put("email", user.getEmail());
            userProfile.put("phone", user.getPhone());
            userProfile.put("avatar", user.getAvatar());
            userProfile.put("isActive", user.getIsActive());
            userProfile.put("createdAt", user.getCreatedAt());
            userProfile.put("lastLogin", user.getLastLogin());
            userProfile.put("roles", user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            log.error("Error retrieving user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving user profile: " + e.getMessage()));
        }
    }

    /**
     * Update user profile.
     *
     * @param userDetails Authenticated user details
     * @param updateRequest Update request data
     * @return Updated user information
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        try {
            User updatedUser = userService.updateUser(
                    userDetails.getId(),
                    updateRequest.getEmail(),
                    updateRequest.getPhone(),
                    null
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());
            response.put("phone", updatedUser.getPhone());
            response.put("avatar", updatedUser.getAvatar());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating profile: " + e.getMessage()));
        }
    }

    /**
     * Update user avatar.
     *
     * @param userDetails Authenticated user details
     * @param file Avatar image file
     * @return Updated avatar URL
     */
    @PostMapping("/me/avatar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateAvatar(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Please select a file to upload"));
            }

            // Save the file
            String uploadDir = "./uploads/avatars/";
            Files.createDirectories(Paths.get(uploadDir));

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID() + fileExtension;
            Path targetPath = Paths.get(uploadDir + newFilename);

            Files.copy(file.getInputStream(), targetPath);

            // Update user avatar in database
            User updatedUser = userService.updateUser(
                    userDetails.getId(),
                    null,
                    null,
                    "/uploads/avatars/" + newFilename
            );

            return ResponseEntity.ok(Map.of(
                    "avatar", updatedUser.getAvatar(),
                    "message", "Avatar updated successfully"
            ));
        } catch (IOException e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error uploading avatar: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating avatar: " + e.getMessage()));
        }
    }

    /**
     * Reset user password.
     *
     * @param userDetails Authenticated user details
     * @param passwordRequest Password reset request
     * @return Success or failure response
     */
    @PostMapping("/me/password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> resetPassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PasswordResetRequest passwordRequest) {
        try {
            boolean success = userService.resetPassword(
                    userDetails.getId(),
                    passwordRequest.getCurrentPassword(),
                    passwordRequest.getNewPassword()
            );

            if (success) {
                return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to update password"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error resetting password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error resetting password: " + e.getMessage()));
        }
    }

    /**
     * Get user groups.
     *
     * @param userDetails Authenticated user details
     * @return List of groups the user belongs to
     */
    @GetMapping("/me/groups")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserGroups(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            return ResponseEntity.ok(groupService.getGroupsByUserId(userDetails.getId()));
        } catch (Exception e) {
            log.error("Error retrieving user groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving user groups: " + e.getMessage()));
        }
    }

    /**
     * Get recently active groups for a user.
     *
     * @param userDetails Authenticated user details
     * @param limit Maximum number of groups to return
     * @return List of recently active groups
     */
    @GetMapping("/me/groups/recent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getRecentGroups(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(groupService.getRecentActiveGroups(userDetails.getId(), limit));
        } catch (Exception e) {
            log.error("Error retrieving recent groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving recent groups: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID.
     *
     * @param id User ID
     * @return User information
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create a map with limited fields for security
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("isActive", user.getIsActive());

            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving user: " + e.getMessage()));
        }
    }

    /**
     * Get recently active users.
     *
     * @param hours Number of hours to look back
     * @return List of recently active users
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getRecentlyActiveUsers(@RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<User> recentUsers = userService.findRecentlyActiveUsers(since);

            // Map to only expose necessary information
            List<Map<String, Object>> userInfoList = recentUsers.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("avatar", user.getAvatar());
                userInfo.put("lastLogin", user.getLastLogin());
                return userInfo;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(userInfoList);
        } catch (Exception e) {
            log.error("Error retrieving recent users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving recent users: " + e.getMessage()));
        }
    }
}