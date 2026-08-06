package com.apricorn.insights.dto;

public record PerformanceInsightResponse(
        Long problemId,
        String title,
        String slug,
        int firstAcceptedRuntimeMs,
        int bestRuntimeMs,
        int improvementMs,
        double improvementPercent
) {}
