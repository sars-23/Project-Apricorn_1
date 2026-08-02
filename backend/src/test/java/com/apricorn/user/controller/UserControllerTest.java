package com.apricorn.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import com.apricorn.user.dto.UserResponse;
import com.apricorn.user.entity.Role;
import com.apricorn.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtServiceImpl.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=0123456789012345678901234567890123456789",
        "jwt.expiration-ms=3600000"
})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void meReturnsCurrentUserProfileWhenAuthenticated() throws Exception {
        UserResponse response = new UserResponse(1L, "apricorn", "user@example.com", Role.USER, Instant.now());
        when(userService.getCurrentUser("user@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("apricorn"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
