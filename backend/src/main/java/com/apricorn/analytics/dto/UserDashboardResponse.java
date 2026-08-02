package com.apricorn.analytics.dto;

import java.util.List;

public record UserDashboardResponse(
        String username,
        long totalSubmissions,
        long acceptedSubmissions,
        double acceptanceRate,
        long problemsAttempted,
        long problemsSolved,
        long easySolved,
        long mediumSolved,
        long hardSolved,
        int currentStreak,
        int longestStreak,
        List<RecentSubmissionResponse> recentSubmissions
) {}
