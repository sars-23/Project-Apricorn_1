package com.apricorn.problem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.problem.dto.AdminTestCaseResponse;
import com.apricorn.problem.dto.CreateTestCaseRequest;
import com.apricorn.problem.service.TestCaseService;
import com.apricorn.security.RestAccessDeniedHandler;
import com.apricorn.security.RestAuthenticationEntryPoint;
import com.apricorn.security.SecurityConfig;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtServiceImpl;
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

@WebMvcTest(controllers = AdminTestCaseController.class)
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
class AdminTestCaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestCaseService testCaseService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToAddTestCase() throws Exception {
        AdminTestCaseResponse response = new AdminTestCaseResponse(1L, 100L, "1 2", "3", true, 0, Instant.now());
        when(testCaseService.addTestCase(eq(100L), any(CreateTestCaseRequest.class))).thenReturn(response);

        CreateTestCaseRequest request = new CreateTestCaseRequest("1 2", "3", true, 0);

        mockMvc.perform(post("/api/v1/admin/problems/100/test-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.problemId").value(100))
                .andExpect(jsonPath("$.input").value("1 2"))
                .andExpect(jsonPath("$.expectedOutput").value("3"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToGetTestCasesForProblem() throws Exception {
        AdminTestCaseResponse response = new AdminTestCaseResponse(1L, 100L, "secret input", "secret output", false, 0, Instant.now());
        when(testCaseService.getTestCasesForProblem(100L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/problems/100/test-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].input").value("secret input"))
                .andExpect(jsonPath("$[0].isSample").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsNormalUserFromAccessingAdminTestCases() throws Exception {
        mockMvc.perform(get("/api/v1/admin/problems/100/test-cases"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToDeleteTestCase() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/test-cases/1"))
                .andExpect(status().isNoContent());
    }
}
