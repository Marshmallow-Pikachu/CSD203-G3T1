package com.ratewise.services;

import com.ratewise.security.dto.LoginRequest;
import com.ratewise.security.dto.LoginResponse;
import com.ratewise.security.dto.RegisterRequest;
import com.ratewise.security.UserRepository;
import com.ratewise.security.entities.RoleRepository;
import com.ratewise.security.util.JWTUtil;
import com.ratewise.security.User;
import com.ratewise.security.entities.Role;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final RoleRepository roleRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTUtil jwtUtil, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.roleRepository = roleRepository;
    }

    public LoginResponse login(LoginRequest request) {
        // Check if user exists
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // Check password, throw error for wrong password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Load user with roles for token generation
        User userWithRoles = userRepository.findByIdWithRoles(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate token with roles
        String token = jwtUtil.generateToken(userWithRoles);

        return LoginResponse.builder()
                .accessToken(token)
                .build();
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Manual password validation
        String password = request.getPassword();
        
        // Check if password is null or blank
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password must be between 8 and 50 characters");
        }
        
        // Check password length
        if (password.length() < 8 || password.length() > 50) {
            throw new RuntimeException("Password must be between 8 and 50 characters");
        }
        
        // Check password complexity (at least one lowercase, uppercase, and number)
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasNumber = password.chars().anyMatch(Character::isDigit);
        
        if (!hasLowercase || !hasUppercase || !hasNumber) {
            throw new RuntimeException("Password must contain at least one lowercase letter, one uppercase letter, and one number");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        roleRepository.assignRoleToUser(savedUser.getId(), Role.ROLE_USER);
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    public void logout(Long userId) {
        jwtUtil.invalidateUserToken(userId);
    }
}