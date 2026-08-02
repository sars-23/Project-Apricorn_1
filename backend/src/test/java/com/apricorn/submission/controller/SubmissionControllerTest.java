package com.apricorn.submission.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.exception.UnauthorizedSubmissionAccessException;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
import com.apricorn.submission.dto.CreateSubmissionRequest;
import com.apricorn.submission.dto.ProblemReferenceResponse;
import com.apricorn.submission.dto.SubmissionCreatedResponse;
import com.apricorn.submission.dto.SubmissionDetailResponse;
import com.apricorn.submission.dto.SubmissionSummaryResponse;
import com.apricorn.submission.dto.UserSubmissionStatsResponse;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = SubmissionController.class)
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
class SubmissionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void allowsAuthenticatedUserToSubmitCode() throws Exception {
        SubmissionCreatedResponse response = new SubmissionCreatedResponse(123L, SubmissionStatus.PENDING, Instant.now());
        when(submissionService.createSubmission(eq("two-sum"), any(CreateSubmissionRequest.class), eq("user@example.com")))
                .thenReturn(response);

        CreateSubmissionRequest request = new CreateSubmissionRequest(Language.JAVA, "public class Main {}");

        mockMvc.perform(post("/api/v1/problems/two-sum/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.submissionId").value(123))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsUnauthenticatedSubmission() throws Exception {
        CreateSubmissionRequest request = new CreateSubmissionRequest(Language.JAVA, "public class Main {}");

        mockMvc.perform(post("/api/v1/problems/two-sum/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void allowsOwnerToGetSubmissionDetail() throws Exception {
        ProblemReferenceResponse problemRef = new ProblemReferenceResponse(1L, "Two Sum", "two-sum");
        SubmissionDetailResponse detail = new SubmissionDetailResponse(
                123L, problemRef, Language.JAVA, "public class Main {}", SubmissionStatus.COMPLETED,
                Verdict.ACCEPTED, 50, 16000, 10, 10, null, Instant.now(), Instant.now()
        );

        when(submissionService.getSubmissionById(123L, "user@example.com", false)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/submissions/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.sourceCode").value("public class Main {}"))
                .andExpect(jsonPath("$.verdict").value("ACCEPTED"));
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "USER")
    void rejectsOtherUserFromGettingSubmissionDetail() throws Exception {
        when(submissionService.getSubmissionById(123L, "other@example.com", false))
                .thenThrow(new UnauthorizedSubmissionAccessException("You are not authorized to view this submission."));

        mockMvc.perform(get("/api/v1/submissions/123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void returnsUserSubmissionsHistory() throws Exception {
        ProblemReferenceResponse problemRef = new ProblemReferenceResponse(1L, "Two Sum", "two-sum");
        SubmissionSummaryResponse summary = new SubmissionSummaryResponse(
                123L, problemRef, Language.JAVA, SubmissionStatus.COMPLETED,
                Verdict.ACCEPTED, 50, 16000, 10, 10, Instant.now(), Instant.now()
        );
        PageResponse<SubmissionSummaryResponse> page = new PageResponse<>(List.of(summary), 0, 20, 1, 1, true, true);

        when(submissionService.getUserSubmissions(eq("user@example.com"), eq("two-sum"), eq(Language.JAVA), eq(null), eq(Verdict.ACCEPTED), eq(0), eq(20), eq("submittedAt,desc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/submissions")
                        .param("problem", "two-sum")
                        .param("language", "JAVA")
                        .param("verdict", "ACCEPTED")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "submittedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(123))
                .andExpect(jsonPath("$.content[0].verdict").value("ACCEPTED"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void returnsUserSubmissionStats() throws Exception {
        UserSubmissionStatsResponse stats = new UserSubmissionStatsResponse(10, 5, 50.0, 4, 3);
        when(submissionService.getUserSubmissionStats("user@example.com")).thenReturn(stats);

        mockMvc.perform(get("/api/v1/users/me/submission-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmissions").value(10))
                .andExpect(jsonPath("$.acceptedSubmissions").value(5))
                .andExpect(jsonPath("$.acceptanceRate").value(50.0))
                .andExpect(jsonPath("$.problemsAttempted").value(4))
                .andExpect(jsonPath("$.problemsSolved").value(3));
    }
}
