package com.apricorn.insights.dto;

public record InsightSummaryResponse(
        long problemsAttempted,
        long problemsSolved,
        long firstTrySolves,
        Double averageAttemptsToSolve,
        long currentUnsolvedProblems,
        Double acceptedSubmissionRate
) {}
