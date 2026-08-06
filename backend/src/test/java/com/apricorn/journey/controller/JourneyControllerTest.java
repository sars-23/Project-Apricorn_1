package com.apricorn.journey.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.journey.dto.*;
import com.apricorn.journey.service.JourneyService;
import com.apricorn.problem.entity.ProblemStatus;
import com.apricorn.problem.entity.Difficulty;
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

@WebMvcTest(JourneyController.class)
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
class JourneyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JourneyService journeyService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@apricorn.com")
    void returnsJourneyPageForAuthenticatedUser() throws Exception {
        JourneyProblemResponse problemResp = new JourneyProblemResponse(1L, "Two Sum", "two-sum", Difficulty.EASY, ProblemStatus.SOLVED);
        JourneySummaryResponse summaryResp = new JourneySummaryResponse(1L, null, 1, 1, 1L, 40, 15000, null, 40, 0, 0.0);
        SolutionJourneyResponse journeyResp = new SolutionJourneyResponse(problemResp, ProblemStatus.SOLVED, summaryResp, List.of(), "Solved on your first attempt.");

        when(journeyService.getSolutionJourney("user@apricorn.com", "two-sum")).thenReturn(journeyResp);

        mockMvc.perform(get("/problems/two-sum/journey"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/journey"))
                .andExpect(model().attributeExists("journey"));
    }

    @Test
    @WithMockUser(username = "user@apricorn.com")
    void returns404ForNonExistentProblem() throws Exception {
        when(journeyService.getSolutionJourney("user@apricorn.com", "unknown")).thenThrow(new ProblemNotFoundException("Problem not found with slug: unknown"));

        mockMvc.perform(get("/problems/unknown/journey"))
                .andExpect(status().isNotFound());
    }
}
