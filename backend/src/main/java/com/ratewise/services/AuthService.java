package com.ratewise.services;

import com.ratewise.security.dto.LoginRequest;
import com.ratewise.security.dto.LoginResponse;
import com.ratewise.security.dto.RegisterRequest;
import com.ratewise.security.UserRepository;
import com.ratewise.security.util.JWTUtil;
import com.ratewise.security.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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

        // Generate token if all checks pass
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
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
                .build();

        userRepository.save(user);
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }
}