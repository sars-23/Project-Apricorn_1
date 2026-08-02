package com.apricorn.analytics.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.analytics.dto.LeaderboardEntryResponse;
import com.apricorn.analytics.service.LeaderboardService;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LeaderboardController.class)
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
class LeaderboardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void permitsPublicAccessToGlobalLeaderboard() throws Exception {
        LeaderboardEntryResponse entry = new LeaderboardEntryResponse(1, "alice", 10, 4, 4, 2, 80.0);
        PageResponse<LeaderboardEntryResponse> page = PageResponse.from(
                new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1)
        );

        when(leaderboardService.getGlobalLeaderboard(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.content[0].rank").value(1));
    }

    @Test
    void returns401ForMeWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void returnsLeaderboardPositionForAuthenticatedUser() throws Exception {
        LeaderboardEntryResponse entry = new LeaderboardEntryResponse(1, "alice", 10, 4, 4, 2, 80.0);

        when(leaderboardService.getUserLeaderboardPosition(anyString())).thenReturn(entry);

        mockMvc.perform(get("/api/v1/leaderboard/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.rank").value(1));
    }
}
