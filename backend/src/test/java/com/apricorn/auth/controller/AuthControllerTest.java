package com.apricorn.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.auth.dto.AuthResponse;
import com.apricorn.auth.dto.LoginRequest;
import com.apricorn.auth.dto.RegisterRequest;
import com.apricorn.auth.service.AuthService;
import com.apricorn.exception.ConflictException;
import com.apricorn.exception.GlobalExceptionHandler;
import com.apricorn.user.dto.UserResponse;
import com.apricorn.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
    private AuthService authService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturns201CreatedOnSuccess() throws Exception {
        UserResponse user = new UserResponse(1L, "apricorn", "user@example.com", Role.USER, Instant.now());
        AuthResponse response = new AuthResponse("mock.jwt.token", "Bearer", 3600, user);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        RegisterRequest request = new RegisterRequest("apricorn", "user@example.com", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("apricorn"));
    }

    @Test
    void registerReturns400OnInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("apricorn", "invalid-email", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerReturns409OnDuplicateEmail() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Email is already registered"));

        RegisterRequest request = new RegisterRequest("apricorn", "duplicate@example.com", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void loginReturns200OkOnSuccess() throws Exception {
        UserResponse user = new UserResponse(1L, "apricorn", "user@example.com", Role.USER, Instant.now());
        AuthResponse response = new AuthResponse("mock.jwt.token", "Bearer", 3600, user);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest("user@example.com", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }
}
