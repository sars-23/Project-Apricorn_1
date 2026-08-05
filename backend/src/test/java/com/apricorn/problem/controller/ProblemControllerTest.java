package com.apricorn.problem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.problem.dto.CreateProblemRequest;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.dto.ProblemSummaryResponse;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.service.ProblemService;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ProblemController.class, AdminProblemController.class})
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
class ProblemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProblemService problemService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void allowsUnauthenticatedAccessToGetProblems() throws Exception {
        ProblemSummaryResponse summary = new ProblemSummaryResponse(1L, "Two Sum", "two-sum", Difficulty.EASY, List.of(), Instant.now(), 2000, 256, null, com.apricorn.problem.entity.ProblemStatus.UNSOLVED);
        PageResponse<ProblemSummaryResponse> page = new PageResponse<>(List.of(summary), 0, 20, 1, 1, true, true);

        when(problemService.getProblems(eq(Difficulty.EASY), eq("array"), eq("two"), any(), any(), eq(0), eq(20), eq("title,asc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/problems")
                        .param("difficulty", "EASY")
                        .param("tag", "array")
                        .param("search", "two")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Two Sum"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void allowsUnauthenticatedAccessToGetProblemBySlug() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(
                1L, "Two Sum", "two-sum", "Description", Difficulty.EASY,
                "Format", "Format", "Constraints", "[2,7], 9", "[0,1]", "Explanation",
                2000, 256, List.of(), Instant.now(), Instant.now(), "admin"
        );

        when(problemService.getProblemBySlug("two-sum")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/problems/two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.slug").value("two-sum"))
                .andExpect(jsonPath("$.sampleInput").value("[2,7], 9"))
                .andExpect(jsonPath("$.testCases").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void allowsAdminToCreateProblem() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(
                10L, "Two Sum", "two-sum", "Description", Difficulty.EASY,
                null, null, null, null, null, null,
                2000, 256, List.of(), Instant.now(), Instant.now(), "admin@example.com"
        );

        when(problemService.createProblem(any(CreateProblemRequest.class), eq("admin@example.com"))).thenReturn(detail);

        CreateProblemRequest request = new CreateProblemRequest(
                "Two Sum", "Description", Difficulty.EASY, null, null, null, null, null, null, 2000, 256, Set.of()
        );

        mockMvc.perform(post("/api/v1/admin/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.slug").value("two-sum"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsNormalUserFromCreatingProblem() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest(
                "Two Sum", "Description", Difficulty.EASY, null, null, null, null, null, null, 2000, 256, Set.of()
        );

        mockMvc.perform(post("/api/v1/admin/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToDeleteProblem() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/problems/10"))
                .andExpect(status().isNoContent());
    }
}
