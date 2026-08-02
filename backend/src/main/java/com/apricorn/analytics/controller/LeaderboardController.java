package com.apricorn.analytics.controller;

import com.apricorn.analytics.dto.LeaderboardEntryResponse;
import com.apricorn.analytics.service.LeaderboardService;
import com.apricorn.problem.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboard")
@Tag(name = "Leaderboard", description = "Global leaderboard APIs")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Get global leaderboard", description = "Retrieves paginated global leaderboard ranked by solved count, hard solved, medium solved, acceptance rate, and username.")
    public PageResponse<LeaderboardEntryResponse> getGlobalLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return leaderboardService.getGlobalLeaderboard(page, size);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my leaderboard position", description = "Retrieves current user's global ranking and statistics.")
    public LeaderboardEntryResponse getMyLeaderboardPosition(@AuthenticationPrincipal UserDetails userDetails) {
        return leaderboardService.getUserLeaderboardPosition(userDetails.getUsername());
    }
}
