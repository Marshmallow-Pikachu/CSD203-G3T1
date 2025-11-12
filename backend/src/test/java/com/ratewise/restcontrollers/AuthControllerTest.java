package com.ratewise.restcontrollers;

import com.ratewise.security.dto.*;
import com.ratewise.security.entities.User;
import com.ratewise.security.exception.JwtTokenException;
import com.ratewise.services.AuthService;
import com.ratewise.security.util.JWTUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JWTUtil jwtUtil;

    // -------- POST /registration --------
    @Test
    void testRegisterSuccess() throws Exception {
        // Password must: min 8 chars, uppercase, lowercase, digit
        String requestBody = "{"
            + "\"username\":\"alice\","
            + "\"email\":\"alice@example.com\","
            + "\"password\":\"Password123\""  // ← Valid password
            + "}";

        Mockito.doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void testRegisterInvalidPassword() throws Exception {
        // Password too short (less than 8 chars)
        String requestBody = "{"
            + "\"username\":\"alice\","
            + "\"email\":\"alice@example.com\","
            + "\"password\":\"Pass1\""
            + "}";

        mockMvc.perform(post("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterMissingUppercase() throws Exception {
        // Password missing uppercase letter
        String requestBody = "{"
            + "\"username\":\"alice\","
            + "\"email\":\"alice@example.com\","
            + "\"password\":\"password123\""
            + "}";

        mockMvc.perform(post("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------- POST /session (login) --------
    @Test
    void testLoginSuccess() throws Exception {
        // LoginRequest expects "username", not "email"
        String requestBody = "{"
            + "\"username\":\"alice\","  // ← Changed from "email"
            + "\"password\":\"Password123\""
            + "}";
        
        LoginResponse mockResponse = LoginResponse.builder()
                .accessToken("mock.jwt.token")
                .build();

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        String requestBody = "{"
            + "\"username\":\"alice\","
            + "\"password\":\"wrongpass\""
            + "}";

        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());  // Adjust based on your exception handler
    }

    @Test
    void testLoginShortUsername() throws Exception {
        // Username too short (less than 3 chars)
        String requestBody = "{"
            + "\"username\":\"ab\","
            + "\"password\":\"Password123\""
            + "}";

        mockMvc.perform(post("/api/v1/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------- GET /validation --------
    @Test
    void testValidateTokenSuccess() throws Exception {
        String token = "valid.jwt.token";
        
        Mockito.when(jwtUtil.getEmail(token)).thenReturn("alice@example.com");
        Mockito.when(jwtUtil.getOAuthProvider(token)).thenReturn(null);
        Mockito.when(jwtUtil.getOAuthProviderId(token)).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/validation")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testValidateTokenInvalidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validation")
                .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());  // Adjust based on exception handler
    }

    @Test
    void testValidateTokenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validation"))
                .andExpect(status().isInternalServerError());  // Adjust based on exception handler
    }

    // -------- GET /profile --------
    @Test
    void testGetCurrentUserSuccess() throws Exception {
        String token = "valid.jwt.token";
        User mockUser = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .build();

        Mockito.when(jwtUtil.getUsername(token)).thenReturn("alice");
        Mockito.when(jwtUtil.getEmail(token)).thenReturn("alice@example.com");
        Mockito.when(jwtUtil.getUserId(token)).thenReturn("u1");
        Mockito.when(authService.getCurrentUser("alice@example.com")).thenReturn(mockUser);

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testGetCurrentUserInvalidToken() throws Exception {
        String invalidToken = "invalid.token";
        
        Mockito.when(jwtUtil.getUsername(invalidToken))
                .thenThrow(new JwtTokenException("Invalid token"));

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());  // Adjust if exception handler returns different status
    }

    // -------- DELETE /session (logout) --------
    @Test
    void testLogoutSuccess() throws Exception {
        String token = "valid.jwt.token";

        Mockito.when(jwtUtil.getUserId(token)).thenReturn("u1");
        Mockito.doNothing().when(authService).logout("u1");

        mockMvc.perform(delete("/api/v1/auth/session")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You have logged out successfully!"))
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    void testLogoutMissingToken() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/session"))
                .andExpect(status().isInternalServerError());  // Adjust based on exception handler
    }
}
