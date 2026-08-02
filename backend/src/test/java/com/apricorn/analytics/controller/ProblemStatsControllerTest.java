package com.apricorn.analytics.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.analytics.dto.ProblemStatsResponse;
import com.apricorn.analytics.service.AnalyticsService;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProblemStatsController.class)
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
class ProblemStatsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void permitsPublicAccessToProblemStats() throws Exception {
        ProblemStatsResponse stats = new ProblemStatsResponse(100, 45, 45.0, 30, 20);

        when(analyticsService.getProblemStats(anyString())).thenReturn(stats);

        mockMvc.perform(get("/api/v1/problems/two-sum/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmissions").value(100))
                .andExpect(jsonPath("$.acceptedSubmissions").value(45))
                .andExpect(jsonPath("$.acceptanceRate").value(45.0))
                .andExpect(jsonPath("$.uniqueUsersAttempted").value(30))
                .andExpect(jsonPath("$.uniqueUsersSolved").value(20));
    }
}
