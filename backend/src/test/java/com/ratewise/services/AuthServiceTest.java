package com.ratewise.services;

import com.ratewise.security.dto.LoginRequest;
import com.ratewise.security.dto.LoginResponse;
import com.ratewise.security.dto.RegisterRequest;
import com.ratewise.security.entities.Role;
import com.ratewise.security.entities.User;
import com.ratewise.security.exception.*;
import com.ratewise.security.repositories.RoleRepository;
import com.ratewise.security.repositories.UserRepository;
import com.ratewise.security.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private User testUserWithRole;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        Role userRole = Role.builder()
                .id(2L)
                .roleName("USER")
                .build();

        testUserWithRole = User.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .role(userRole)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("Password123")
                .build();
    }

    // login 

    @Test
    void testLoginSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "hashedPassword123")).thenReturn(true);
        when(userRepository.findByIdWithRole("user-123")).thenReturn(Optional.of(testUserWithRole));
        when(jwtUtil.generateToken(testUserWithRole)).thenReturn("jwt.token.here");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.getAccessToken());
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("Password123", "hashedPassword123");
        verify(jwtUtil).generateToken(testUserWithRole);
    }

    @Test
    void testLoginInvalidUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLoginInvalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "hashedPassword123")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("Password123", "hashedPassword123");
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void testLoginAccountDisabled() {
        User disabledUser = User.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .enabled(false)  // Account disabled
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(disabledUser));

        assertThrows(AccountDisabledException.class, () -> {
            authService.login(loginRequest);
        });

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLoginUserNotFoundAfterPasswordCheck() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "hashedPassword123")).thenReturn(true);
        when(userRepository.findByIdWithRole("user-123")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.login(loginRequest);
        });

        verify(userRepository).findByIdWithRole("user-123");
    }

    // Register Tests 

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword123");
        
        User savedUser = User.builder()
                .id("new-user-id")
                .username("newuser")
                .email("newuser@example.com")
                .password("hashedPassword123")
                .enabled(true)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(roleRepository).assignRoleToUser("new-user-id", Role.ROLE_USER);

        authService.register(registerRequest);

        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).existsByUsername("newuser");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
        verify(roleRepository).assignRoleToUser("new-user-id", Role.ROLE_USER);
    }

    @Test
    void testRegisterEmailAlreadyExists() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterUsernameAlreadyExists() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository, never()).save(any());
    }

    // Get Current User

    @Test
    void testGetCurrentUserSuccess() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User user = authService.getCurrentUser("test@example.com");

        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testGetCurrentUserNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.getCurrentUser("notfound@example.com");
        });

        verify(userRepository).findByEmail("notfound@example.com");
    }

    // logout  

    @Test
    void testLogout() {
        doNothing().when(jwtUtil).invalidateUserToken("user-123");

        authService.logout("user-123");

        verify(jwtUtil).invalidateUserToken("user-123");
    }
}
