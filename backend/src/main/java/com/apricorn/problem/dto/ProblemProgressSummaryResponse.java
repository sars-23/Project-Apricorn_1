package com.apricorn.problem.dto;

public record ProblemProgressSummaryResponse(
        long solvedTotal,
        long totalProblems,
        long solvedEasy,
        long totalEasy,
        long solvedMedium,
        long totalMedium,
        long solvedHard,
        long totalHard
) {}
