package com.ratewise.restcontrollers;

import com.ratewise.security.dto.*;
import com.ratewise.security.entities.Role;
import com.ratewise.security.entities.User;
import com.ratewise.security.exception.RoleNotFoundException;
import com.ratewise.security.exception.UserNotFoundException;
import com.ratewise.security.repositories.UserRepository;
import com.ratewise.security.repositories.RoleRepository;
import com.ratewise.security.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin user management endpoints")
public class AdminController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JWTUtil jwtUtil;

    public AdminController(UserRepository userRepository, RoleRepository roleRepository, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
    }


    /**
     * Get all users with their role
     * GET /api/v1/admin/users
     */
    @Operation(summary = "Get all users", description = "Retrieve all users with their role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        List<UserSummaryResponse> users = new ArrayList<>();

        List<User> allUsers = userRepository.findAll();

        allUsers.forEach(user -> {
            Optional<User> userWithRole = userRepository.findByIdWithRole(user.getId());
            userWithRole.ifPresent(u -> {
                String roleName = u.getRole() != null ? u.getRole().getRoleName() : null;
                UserSummaryResponse userResponse = UserSummaryResponse.builder()
                        .userId(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .isActive(u.isEnabled())
                        .createdAt(u.getCreatedAt())
                        .role(roleName)
                        .build();
                users.add(userResponse);
            });
        });

        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID with role
     * GET /api/v1/admin/users/{id}
     */
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user with their role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<SpecificUserDetailResponse> getUserById(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findByIdWithRole(id);
        if(userOpt.isEmpty()) {
            throw new UserNotFoundException(id);
        }

        User user = userOpt.get();
        SpecificUserDetailResponse response = SpecificUserDetailResponse.builder()
                .message("User found")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .role(user.getRole().getRoleName())
                .build();

        return ResponseEntity.ok(response);
    }


    /**
     * Update user's role (replaces existing role with new one)
     * PUT /api/v1/admin/users/{userId}/role/{roleId}
     */
    @Operation(summary = "Update user role", description = "Update a user's role - replaces existing role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PutMapping("/users/{userId}/role/{roleId}")
    public ResponseEntity<RoleUpdateResponse> updateUserRole(@PathVariable String userId, @PathVariable Long roleId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if(roleOpt.isEmpty()) {
            throw new RoleNotFoundException(roleId);
        }

        roleRepository.assignRoleToUser(userId, roleId);

        // Invalidate user's token so they need to re-login with new role
        jwtUtil.invalidateUserToken(userId);

        RoleUpdateResponse response = RoleUpdateResponse.builder()
                        .message("Role updated successfully for " + userOpt.get().getEmail())
                        .userId(userId)
                        .username(userOpt.get().getUsername())
                        .email(userOpt.get().getEmail())
                        .newRole(roleOpt.get().getRoleName())
                        .note("User must re-login for changes to take effect")
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Enable/Disable user account
     * PATCH /api/v1/admin/users/{id}/status
     */
    @Operation(summary = "Update user status", description = "Enable or disable a user account (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserStatusResponse> updateUserStatus(@PathVariable String id, @RequestBody @Valid UserStatusRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(id);
        }

        if (request.getIsActive() == null) {
            throw new IllegalArgumentException("Invalid request body: 'isActive' field is required");
        }

        // Invalidate token if disabling user
        if (Boolean.FALSE.equals(request.getIsActive())) {
            jwtUtil.invalidateUserToken(id);
        }

        User user = userOpt.get();
        user.setEnabled(request.getIsActive());
        userRepository.save(user);

        UserStatusResponse response = UserStatusResponse.builder()
                        .message("User status updated successfully")
                        .userId(id)
                        .isActive(user.isEnabled())
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available roles
     * GET /api/v1/admin/roles
     */
    @Operation(summary = "Get all roles", description = "Retrieve all available roles (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleResponse> response = new ArrayList<>();

        roles.forEach(role -> {
            RoleResponse roleResponse = RoleResponse.builder()
                            .roleId(role.getId())
                            .roleName(role.getRoleName())
                            .build();

            response.add(roleResponse);
        });

        return ResponseEntity.ok(response);
    }

}