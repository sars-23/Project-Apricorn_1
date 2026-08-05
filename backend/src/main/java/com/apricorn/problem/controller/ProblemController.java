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

import com.apricorn.problem.entity.ProblemStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

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
            @RequestParam(name = "difficulty", required = false) String difficultyStr,
            @RequestParam(name = "tag", required = false) String tagSlug,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String statusStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails) {

        String searchQuery = (q != null && !q.isBlank()) ? q : search;

        Difficulty difficulty = null;
        if (difficultyStr != null && !difficultyStr.isBlank() && !"ALL".equalsIgnoreCase(difficultyStr)) {
            try {
                difficulty = Difficulty.valueOf(difficultyStr.toUpperCase().trim());
            } catch (Exception ignored) {}
        }

        ProblemStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = ProblemStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (Exception ignored) {}
        }

        if ("ALL".equalsIgnoreCase(tagSlug)) {
            tagSlug = null;
        }

        String currentUserEmail = userDetails != null ? userDetails.getUsername() : null;
        return problemService.getProblems(difficulty, tagSlug, searchQuery, status, currentUserEmail, page, size, sort);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get problem by slug", description = "Retrieves problem details without exposing hidden judge test cases.")
    public ProblemDetailResponse getProblemBySlug(@PathVariable String slug) {
        return problemService.getProblemBySlug(slug);
    }
}
