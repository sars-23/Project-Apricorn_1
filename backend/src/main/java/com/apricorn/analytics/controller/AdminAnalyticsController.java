package com.apricorn.analytics.controller;

import com.apricorn.analytics.dto.PlatformAnalyticsResponse;
import com.apricorn.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Analytics", description = "Admin platform analytics APIs")
public class AdminAnalyticsController {
    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get platform analytics", description = "Retrieves global platform metrics, user counts, and distributions.")
    public PlatformAnalyticsResponse getPlatformAnalytics() {
        return analyticsService.getAdminPlatformAnalytics();
    }
}
