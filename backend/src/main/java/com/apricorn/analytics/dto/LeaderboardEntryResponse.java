package com.apricorn.analytics.dto;

public record LeaderboardEntryResponse(
        int rank,
        String username,
        long problemsSolved,
        long easySolved,
        long mediumSolved,
        long hardSolved,
        double acceptanceRate
) {}
