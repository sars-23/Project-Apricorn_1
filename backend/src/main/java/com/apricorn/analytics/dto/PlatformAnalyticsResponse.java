package com.apricorn.analytics.dto;

import com.apricorn.problem.entity.Difficulty;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Verdict;
import java.util.Map;

public record PlatformAnalyticsResponse(
        long totalUsers,
        long totalProblems,
        long totalSubmissions,
        long acceptedSubmissions,
        double overallAcceptanceRate,
        long activeUsersToday,
        long submissionsToday,
        Map<Difficulty, Long> difficultyDistribution,
        Map<Verdict, Long> verdictDistribution,
        Map<Language, Long> languageDistribution
) {}
