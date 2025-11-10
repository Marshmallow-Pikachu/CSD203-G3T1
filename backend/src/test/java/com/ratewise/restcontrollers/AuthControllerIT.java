package com.ratewise.restcontrollers;

import com.ratewise.security.dto.LoginRequest;
import com.ratewise.security.dto.LoginResponse;
import com.ratewise.security.dto.RegisterRequest;
import com.ratewise.security.entities.User;
import com.ratewise.security.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    // Track test users to delete after each test
//    private final List<String> testEmails = new ArrayList<>();

//     @AfterEach
//     void cleanup() {
//         // Only delete test users created during this test
//         for (String email : testEmails) {
//             userRepository.findByEmail(email).ifPresent(user -> {
//                 userRepository.delete(user);
//                 System.out.println("Cleaned up test user: " + email);
//             });
//         }
//         testEmails.clear();
//     }

//     private void // tracktestUser(String email) {
//         testEmails.add(email);
//     }

    @Test
    void testRegisterAndLoginFlow() {
        String email = "testuser_" + System.currentTimeMillis() + "@example.com";
        // tracktestUser(email);

        // 1. Register a new user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser_" + System.currentTimeMillis())
                .email(email)
                .password("Password123")
                .build();

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/registration",
                registerRequest,
                String.class
        );

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertEquals("User registered successfully", registerResponse.getBody());

        // Verify user exists in database
        User user = userRepository.findByEmail(email).orElse(null);
        assertNotNull(user);
        assertEquals(registerRequest.getUsername(), user.getUsername());

        // 2. Login with the registered user
        LoginRequest loginRequest = LoginRequest.builder()
                .username(registerRequest.getUsername())
                .password("Password123")
                .build();

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/session",
                loginRequest,
                LoginResponse.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getAccessToken());
        assertTrue(loginResponse.getBody().getAccessToken().length() > 0);
    }

    @Test
    void testRegisterDuplicateUser() {
        String email = "duplicate_" + System.currentTimeMillis() + "@example.com";
        // // tracktestUser(email);

        // Register first user
        RegisterRequest request = RegisterRequest.builder()
                .username("duplicate_" + System.currentTimeMillis())
                .email(email)
                .password("Password123")
                .build();

        restTemplate.postForEntity("/api/v1/auth/registration", request, String.class);

        // Try to register same user again
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/registration",
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode()); // duplicate user returns error 409 Conflict 
    }

    @Test
    void testLoginWithInvalidCredentials() {
        String email = "validuser_" + System.currentTimeMillis() + "@example.com";
        String username = "validuser_" + System.currentTimeMillis();
        // tracktestUser(email);

        // Register user first
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password("Password123")
                .build();

        restTemplate.postForEntity("/api/v1/auth/registration", registerRequest, String.class);

        // Try to login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password("WrongPassword123")
                .build();

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/session",
                loginRequest,
                LoginResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetProfileWithValidToken() {
        String email = "profileuser_" + System.currentTimeMillis() + "@example.com";
        String username = "profileuser_" + System.currentTimeMillis();
        // tracktestUser(email);

        // 1. Register and login to get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password("Password123")
                .build();

        restTemplate.postForEntity("/api/v1/auth/registration", registerRequest, String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password("Password123")
                .build();

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/session",
                loginRequest,
                LoginResponse.class
        );

        String token = loginResponse.getBody().getAccessToken();

        // 2. Get profile with token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> profileResponse = restTemplate.exchange(
                "/api/v1/auth/profile",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, profileResponse.getStatusCode());
        assertTrue(profileResponse.getBody().contains(username));
        assertTrue(profileResponse.getBody().contains(email));
    }

    @Test
    void testLogout() {
        String email = "logoutuser_" + System.currentTimeMillis() + "@example.com";
        String username = "logoutuser_" + System.currentTimeMillis();
        // tracktestUser(email);

        // 1. Register and login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password("Password123")
                .build();

        restTemplate.postForEntity("/api/v1/auth/registration", registerRequest, String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password("Password123")
                .build();

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/session",
                loginRequest,
                LoginResponse.class
        );

        String token = loginResponse.getBody().getAccessToken();

        // 2. Logout
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> logoutResponse = restTemplate.exchange(
                "/api/v1/auth/session",
                HttpMethod.DELETE,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertTrue(logoutResponse.getBody().contains("logged out successfully"));
    }

    @Test
    void testRegisterWithInvalidPassword() {
        String email = "invalidpass_" + System.currentTimeMillis() + "@example.com";
        // tracktestUser(email);  // Track even if registration fails (for safety)

        // Password too short
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email(email)
                .password("Pass1")  // Only 5 chars, needs 8+
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/registration",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testRegisterWithInvalidEmail() {
        // No need to track - registration will fail
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("invalid-email")  // Not a valid email
                .password("Password123")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/registration",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
