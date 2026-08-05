package com.apricorn.web;

import com.apricorn.analytics.dto.UserDashboardResponse;
import com.apricorn.analytics.service.AnalyticsService;
import com.apricorn.auth.service.AuthService;
import com.apricorn.problem.service.ProblemService;
import com.apricorn.problem.service.TagService;
import com.apricorn.security.jwt.JwtService;
import com.apricorn.submission.service.SubmissionService;
import com.apricorn.user.dto.UserResponse;
import com.apricorn.user.entity.Role;
import com.apricorn.user.service.UserService;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(WebController.class)
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private UserService userService;

    @MockBean
    private ProblemService problemService;

    @MockBean
    private TagService tagService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@example.com")
    void profile_AuthenticatedUser_ReturnsProfileView() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "username", "user@example.com", Role.USER, Instant.now());
        when(userService.getCurrentUser("user@example.com")).thenReturn(userResponse);

        UserDashboardResponse dashboardResponse = new UserDashboardResponse(
                "username", 10L, 5L, 50.0, 8L, 5L, 2L, 2L, 1L, 3, 5, Collections.emptyList()
        );
        when(analyticsService.getUserDashboard("user@example.com")).thenReturn(dashboardResponse);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().attributeExists("dashboard"));
    }
}
