package com.apricorn.analytics.dto;

import java.util.List;

public record UserDashboardResponse(
        String username,
        DashboardSummaryResponse summary,
        DifficultyProgressResponse difficultyProgress,
        List<VerdictStatResponse> verdictDistribution,
        List<DailyActivityResponse> activity,
        List<LanguageStatResponse> languageDistribution,
        List<RecentSubmissionResponse> recentSubmissions,
        List<ContinueSolvingResponse> continueSolving,
        // Legacy direct fields for backward compatibility with profile.html and tests
        long totalSubmissions,
        long acceptedSubmissions,
        double acceptanceRate,
        long problemsAttempted,
        long problemsSolved,
        long easySolved,
        long mediumSolved,
        long hardSolved,
        int currentStreak,
        int longestStreak
) {
    public UserDashboardResponse(
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
    ) {
        this(
                username,
                new DashboardSummaryResponse(problemsSolved, 0, totalSubmissions, totalSubmissions, acceptedSubmissions, acceptanceRate),
                new DifficultyProgressResponse(easySolved, 0, mediumSolved, 0, hardSolved, 0),
                List.of(),
                List.of(),
                List.of(),
                recentSubmissions != null ? recentSubmissions : List.of(),
                List.of(),
                totalSubmissions,
                acceptedSubmissions,
                acceptanceRate,
                problemsAttempted,
                problemsSolved,
                easySolved,
                mediumSolved,
                hardSolved,
                currentStreak,
                longestStreak
        );
    }
}
