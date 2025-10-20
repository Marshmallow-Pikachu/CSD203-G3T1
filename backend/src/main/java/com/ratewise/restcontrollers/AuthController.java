package com.ratewise.restcontrollers;

import com.ratewise.services.AuthService;
import com.ratewise.security.util.JWTUtil;
import com.ratewise.security.entities.User;
import com.ratewise.security.dto.LoginRequest;
import com.ratewise.security.dto.LoginResponse;
import com.ratewise.security.dto.RegisterRequest;
import com.ratewise.security.exception.JwtTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;
    private final JWTUtil jwtUtil;

    public AuthController(AuthService authService, JWTUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * User Registration
     * POST /api/v1/auth/registration
     */
    @Operation(summary = "Register new user", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or user already exists")
    })
    @PostMapping("/registration")
    public ResponseEntity<String> register(@RequestBody @Validated RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).body("User registered successfully");
    }

    /**
     * User Login
     * POST /api/v1/auth/session
     */
    @Operation(summary = "User login", description = "Authenticate user and receive JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/session")
    public ResponseEntity<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate JWT Token
     * GET /api/v1/auth/validation
     */
    @Operation(summary = "Validate JWT token", description = "Validate a JWT token (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @GetMapping("/validation")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtTokenException("Invalid token format");
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.getEmail(token);
        return ResponseEntity.ok("Token valid for: " + email);
    }

    /**
     * Get Current User Info
     * GET /api/v1/auth/profile
     */
    @Operation(summary = "Get current user profile", description = "Retrieve current authenticated user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtTokenException("Invalid token format");
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.getEmail(token);
        Long userId = jwtUtil.getUserId(token);

        // Get user details from service
        User user = authService.getCurrentUser(email);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", user.getUsername());
        userInfo.put("email", email);

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Logout User
     * DELETE /api/v1/auth/session
     */
    @Operation(summary = "Logout user", description = "Invalidate user's JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @DeleteMapping("/session")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtTokenException("Invalid token format");
        }

        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserId(token);

        // Invalidate the user's token
        authService.logout(userId);
        return ResponseEntity.ok("Logged out successfully");
    }
}