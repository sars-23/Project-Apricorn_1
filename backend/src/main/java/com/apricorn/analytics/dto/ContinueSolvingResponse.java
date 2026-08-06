package com.apricorn.analytics.dto;

import com.apricorn.problem.entity.Difficulty;
import java.time.Instant;

public record ContinueSolvingResponse(
        Long problemId,
        String title,
        String slug,
        Difficulty difficulty,
        Instant lastAttemptedAt
) {}
