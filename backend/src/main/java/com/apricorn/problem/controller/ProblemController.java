package com.apricorn.problem.controller;

import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.dto.ProblemSummaryResponse;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems")
@Tag(name = "Problems", description = "Public problem APIs")
public class ProblemController {
    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    @Operation(summary = "List problems", description = "Browse, search, filter, and paginate coding problems.")
    public PageResponse<ProblemSummaryResponse> getProblems(
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(name = "tag", required = false) String tagSlug,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return problemService.getProblems(difficulty, tagSlug, search, page, size, sort);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get problem by slug", description = "Retrieves problem details without exposing hidden judge test cases.")
    public ProblemDetailResponse getProblemBySlug(@PathVariable String slug) {
        return problemService.getProblemBySlug(slug);
    }
}
