package com.apricorn.analytics.controller;

import com.apricorn.analytics.dto.UserDashboardResponse;
import com.apricorn.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/dashboard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Dashboard", description = "User coding profile & analytics APIs")
public class DashboardController {
    private final AnalyticsService analyticsService;

    public DashboardController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get my coding dashboard", description = "Returns user profile, solved breakdown, streak calculation, and recent activity.")
    public UserDashboardResponse getMyDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return analyticsService.getUserDashboard(userDetails.getUsername());
    }
}
