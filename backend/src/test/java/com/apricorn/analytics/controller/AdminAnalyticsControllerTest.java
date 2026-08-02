package com.apricorn.analytics.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.analytics.dto.PlatformAnalyticsResponse;
import com.apricorn.analytics.service.AnalyticsService;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminAnalyticsController.class)
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
class AdminAnalyticsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void returns403ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsAnalyticsForAdminUser() throws Exception {
        PlatformAnalyticsResponse analytics = new PlatformAnalyticsResponse(
                50, 20, 500, 200, 40.0, 15, 80, Map.of(), Map.of(), Map.of()
        );

        when(analyticsService.getAdminPlatformAnalytics()).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(50))
                .andExpect(jsonPath("$.totalProblems").value(20))
                .andExpect(jsonPath("$.overallAcceptanceRate").value(40.0));
    }
}
