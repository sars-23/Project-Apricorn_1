package com.apricorn.analytics.dto;

public record DifficultyProgressResponse(
        long easySolved,
        long totalEasy,
        long mediumSolved,
        long totalMedium,
        long hardSolved,
        long totalHard
) {}
