package com.apricorn.insights.dto;

import com.apricorn.problem.entity.Difficulty;

public record DifficultyInsightResponse(
        Difficulty difficulty,
        String label,
        long attemptedCount,
        long solvedCount,
        double successPercentage
) {}
