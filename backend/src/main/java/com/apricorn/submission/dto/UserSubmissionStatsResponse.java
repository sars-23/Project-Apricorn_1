package com.apricorn.submission.dto;

public record UserSubmissionStatsResponse(
        long totalSubmissions,
        long acceptedSubmissions,
        double acceptanceRate,
        long problemsAttempted,
        long problemsSolved
) {}
