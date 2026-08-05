package com.apricorn.problem.dto;

import com.apricorn.problem.entity.Difficulty;
import java.time.Instant;
import java.util.List;

public record ProblemSummaryResponse(
        Long id,
        String title,
        String slug,
        Difficulty difficulty,
        List<TagResponse> tags,
        Instant createdAt,
        int timeLimitMs,
        int memoryLimitMb
) {}
