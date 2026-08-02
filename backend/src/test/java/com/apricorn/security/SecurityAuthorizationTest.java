package com.apricorn.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.controller.HealthController;
import com.apricorn.security.jwt.JwtAuthenticationFilter;
import com.apricorn.security.jwt.JwtService;
import com.apricorn.service.HealthService;
import com.apricorn.user.controller.AdminController;
import com.apricorn.user.controller.UserController;
import com.apricorn.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AdminController.class, UserController.class, HealthController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=0123456789012345678901234567890123456789",
        "jwt.expiration-ms=3600000"
})
class SecurityAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private HealthService healthService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void allowsUnauthenticatedAccessToHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnauthenticatedAccessToProtectedUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsNormalUserAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_ACCESS_GRANTED"));
    }
}
