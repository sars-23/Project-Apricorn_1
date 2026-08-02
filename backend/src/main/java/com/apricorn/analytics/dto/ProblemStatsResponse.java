package com.apricorn.analytics.dto;

public record ProblemStatsResponse(
        long totalSubmissions,
        long acceptedSubmissions,
        double acceptanceRate,
        long uniqueUsersAttempted,
        long uniqueUsersSolved
) {}
