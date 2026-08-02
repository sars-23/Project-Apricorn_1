package com.apricorn.problem.controller;

import com.apricorn.problem.dto.TagResponse;
import com.apricorn.problem.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags", description = "Public tag APIs")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Get all tags", description = "Retrieves all available problem tags.")
    public List<TagResponse> getAllTags() {
        return tagService.getAllTags();
    }
}
