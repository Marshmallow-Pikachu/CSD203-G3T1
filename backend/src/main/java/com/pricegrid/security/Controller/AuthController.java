package com.pricegrid.security.Controller;

import com.pricegrid.security.Service.AuthService;
import com.pricegrid.security.util.JWTUtil;
import com.pricegrid.security.User;
import com.pricegrid.security.dto.LoginRequest;
import com.pricegrid.security.dto.LoginResponse;
import com.pricegrid.security.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JWTUtil jwtUtil;

    public AuthController(AuthService authService, JWTUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * User Registration
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Validated RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(201).body("User registered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    /**
     * User Login
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Validated LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.status(200).body(response);
        } catch (RuntimeException e) {
            // Return the specific error message instead of empty token
            Map<String, String> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * Validate JWT Token
     * GET /api/v1/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body("Invalid token format");
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.getEmail(token);
            return ResponseEntity.status(200).body("Token valid for: " + email);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }

    /**
     * Get Current User Info
     * GET /api/v1/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body(Map.of("error", "Invalid token format"));
            }

            String token = authHeader.substring(7);
            
            String email = jwtUtil.getEmail(token);
            Long userId = jwtUtil.getUserId(token);
            User user = authService.getCurrentUser(email);
            
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            
            return ResponseEntity.status(200).body(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    /**
     * Logout User
     * DELETE /api/v1/auth/logout
     */
    @DeleteMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(200).body("Logged out successfully"); 
    }
}