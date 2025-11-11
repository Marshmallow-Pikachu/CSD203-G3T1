package com.ratewise.restcontrollers;

import com.ratewise.security.dto.*;
import com.ratewise.security.oauth2.CustomOAuth2UserService;
import com.ratewise.security.oauth2.OAuth2LoginSuccessHandler;
import com.ratewise.services.AuthService;
import com.ratewise.security.util.JWTUtil;
import com.ratewise.security.entities.User;
import com.ratewise.security.exception.JwtTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
        @ApiResponse(responseCode = "400", description = "Invalid request or user already exists",
                        content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/registration")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request) {
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
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/session")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
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
        @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/validation")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtTokenException("Invalid token format");
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.getEmail(token);
        String oauthProvider = jwtUtil.getOAuthProvider(token);
        String oauthProviderId = jwtUtil.getOAuthProviderId(token);

        TokenValidationResponse response = TokenValidationResponse.builder()
                .message("Token valid for : " + email)
                .email(email)
                .valid(true)
                .oauthProvider(oauthProvider)
                .oauthProviderId(oauthProviderId)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get Current User Info
     * GET /api/v1/auth/profile
     */
    @Operation(summary = "Get current user profile", description = "Retrieve current authenticated user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtTokenException("Invalid token format");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsername(token);
        String email = jwtUtil.getEmail(token);
        String userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);

        // Get user details from service
        User user = authService.getCurrentUser(email);
        ProfileResponse response = ProfileResponse.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .role(role)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Logout User
     * DELETE /api/v1/auth/session
     */
    @Operation(summary = "Logout user", description = "Invalidate user's JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/session")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtTokenException("Invalid token format");
        }

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserId(token);

        // Invalidate the user's token
        authService.logout(userId);

        LogoutResponse response = LogoutResponse.builder()
                .message("You have logged out successfully!")
                .userId(userId)
                .build();
        return ResponseEntity.ok(response);
    }
}