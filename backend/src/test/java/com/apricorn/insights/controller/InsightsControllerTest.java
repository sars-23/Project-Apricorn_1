package com.apricorn.insights.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.apricorn.insights.dto.InsightSummaryResponse;
import com.apricorn.insights.dto.UserInsightsResponse;
import com.apricorn.insights.service.InsightsService;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import com.apricorn.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightsController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtServiceImpl.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=supersecretkey_for_testing_purposes_only_which_is_long_enough",
        "jwt.expiration-ms=86400000"
})
class InsightsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InsightsService insightsService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@apricorn.com")
    void returnsInsightsPageForAuthenticatedUser() throws Exception {
        InsightSummaryResponse summary = new InsightSummaryResponse(2, 1, 1, 1.0, 1, 50.0);
        UserInsightsResponse response = new UserInsightsResponse(summary, List.of(), List.of(), List.of(), List.of(), List.of(), 0, 0, 0);

        when(insightsService.getUserInsights("user@apricorn.com")).thenReturn(response);

        mockMvc.perform(get("/insights"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/insights"))
                .andExpect(model().attributeExists("insights"));
    }
}
