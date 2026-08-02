package com.apricorn.analytics.controller;

import com.apricorn.analytics.dto.ProblemStatsResponse;
import com.apricorn.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems/{slug}/stats")
@Tag(name = "Problem Statistics", description = "Problem analytics APIs")
public class ProblemStatsController {
    private final AnalyticsService analyticsService;

    public ProblemStatsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get problem stats", description = "Retrieves safe aggregate metrics for a problem.")
    public ProblemStatsResponse getProblemStats(@PathVariable String slug) {
        return analyticsService.getProblemStats(slug);
    }
}
