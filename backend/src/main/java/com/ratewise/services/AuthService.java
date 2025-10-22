package com.ratewise.services;

import com.ratewise.security.dto.LoginRequest;
import com.ratewise.security.dto.LoginResponse;
import com.ratewise.security.dto.RegisterRequest;
import com.ratewise.security.repositories.UserRepository;
import com.ratewise.security.repositories.RoleRepository;
import com.ratewise.security.util.JWTUtil;
import com.ratewise.security.entities.User;
import com.ratewise.security.entities.Role;
import com.ratewise.security.exception.EmailAlreadyExistsException;
import com.ratewise.security.exception.UsernameAlreadyExistsException;
import com.ratewise.security.exception.InvalidCredentialsException;
import com.ratewise.security.exception.AccountDisabledException;
import com.ratewise.security.exception.UserNotFoundException;

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
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }

        // Check password, throw error for wrong password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Load user with role for token generation
        User userWithRole = userRepository.findByIdWithRole(user.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Generate token with role
        String token = jwtUtil.generateToken(userWithRole);

        return LoginResponse.builder()
                .accessToken(token)
                .build();
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        roleRepository.assignRoleToUser(savedUser.getId(), Role.ROLE_USER);
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void logout(String userId) {
        jwtUtil.invalidateUserToken(userId);
    }
}