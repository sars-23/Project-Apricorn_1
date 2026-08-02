package com.apricorn.problem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apricorn.problem.dto.CreateTagRequest;
import com.apricorn.problem.dto.TagResponse;
import com.apricorn.problem.service.TagService;
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

@WebMvcTest(controllers = {TagController.class, AdminTagController.class})
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
class TagControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void allowsUnauthenticatedAccessToGetAllTags() throws Exception {
        TagResponse tag = new TagResponse(1L, "Array", "array", Instant.now());
        when(tagService.getAllTags()).thenReturn(List.of(tag));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Array"))
                .andExpect(jsonPath("$[0].slug").value("array"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToCreateTag() throws Exception {
        TagResponse tag = new TagResponse(1L, "Dynamic Programming", "dynamic-programming", Instant.now());
        when(tagService.createTag(any(CreateTagRequest.class))).thenReturn(tag);

        CreateTagRequest request = new CreateTagRequest("Dynamic Programming");

        mockMvc.perform(post("/api/v1/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dynamic Programming"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsNormalUserFromCreatingTag() throws Exception {
        CreateTagRequest request = new CreateTagRequest("Dynamic Programming");

        mockMvc.perform(post("/api/v1/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToDeleteTag() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/tags/1"))
                .andExpect(status().isNoContent());
    }
}
