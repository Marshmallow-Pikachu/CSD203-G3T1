package com.ratewise.restcontrollers;

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
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();

        List<User> allUsers = userRepository.findAll();

        allUsers.forEach(user -> {
            Optional<User> userWithRole = userRepository.findByIdWithRole(user.getId());
            userWithRole.ifPresent(u -> {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("id", u.getId());
                userMap.put("username", u.getUsername());
                userMap.put("email", u.getEmail());
                userMap.put("isActive", u.isEnabled());
                userMap.put("createdAt", u.getCreatedAt());
                String roleName = u.getRole() != null ? u.getRole().getRoleName() : null;
                userMap.put("role", roleName);
                users.add(userMap);
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
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable long id) {
        Optional<User> userOpt = userRepository.findByIdWithRole(id);
        if(userOpt.isEmpty()) {
            throw new UserNotFoundException(id);
        }

        User user = userOpt.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "User found");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("isActive", user.isEnabled());
        response.put("createdAt", user.getCreatedAt());
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        response.put("role", roleName);

        return ResponseEntity.ok(response);
    }


    /**
     * Update user's role (replaces existing role with new one)
     * PUT /api/v1/admin/users/{userId}/role/{roleId}
     */
    @Operation(summary = "Update user role", description = "Update a user's role - replaces existing role (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    @PutMapping("/users/{userId}/role/{roleId}")
    public ResponseEntity<Map<String, Object>> updateUserRole(@PathVariable Long userId, @PathVariable Long roleId) {
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

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Role updated successfully");
        response.put("userId", userId);
        response.put("username", userOpt.get().getUsername());
        response.put("email", userOpt.get().getEmail());
        response.put("newRole", roleOpt.get().getRoleName());
        response.put("note", "User must re-login for changes to take effect");
        return ResponseEntity.ok(response);
    }

    /**
     * Enable/Disable user account
     * PATCH /api/v1/admin/users/{id}/status
     */
    @Operation(summary = "Update user status", description = "Enable or disable a user account (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(id);
        }

        if (!request.containsKey("isActive")) {
            throw new IllegalArgumentException("Invalid request body: 'isActive' field is required");
        }

        // Invalidate token if disabling user
        if (Boolean.FALSE.equals(request.get("isActive"))) {
            jwtUtil.invalidateUserToken(id);
        }

        User user = userOpt.get();
        user.setEnabled(request.get("isActive"));
        userRepository.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "User status updated successfully");
        response.put("userId", id);
        response.put("isActive", user.isEnabled());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available roles
     * GET /api/v1/admin/roles
     */
    @Operation(summary = "Get all roles", description = "Retrieve all available roles (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        roles.forEach(role -> {
            Map<String, Object> roleMap = new LinkedHashMap<>();
            roleMap.put("id", role.getId());
            roleMap.put("roleName", role.getRoleName());
            response.add(roleMap);
        });

        return ResponseEntity.ok(response);
    }

}