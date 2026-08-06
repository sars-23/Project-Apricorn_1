package com.apricorn.analytics.dto;

public record DashboardSummaryResponse(
        long totalProblemsSolved,
        long totalCatalogProblems,
        long totalSubmissions,
        long judgedSubmissions,
        long acceptedSubmissions,
        Double acceptanceRate
) {}
