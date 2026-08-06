package com.apricorn.insights.dto;

import java.util.List;

public record UserInsightsResponse(
        InsightSummaryResponse summary,
        List<InsightCardResponse> cards,
        List<FailurePatternResponse> failurePatterns,
        List<DifficultyInsightResponse> difficultyBreakdown,
        List<BlockerResponse> blockers,
        List<PerformanceInsightResponse> topImprovements,
        long recentSubmissions7Days,
        long recentAccepted7Days,
        long recentSolved7Days
) {}
