package com.apricorn.problem.dto;

import com.apricorn.problem.entity.Difficulty;
import java.time.Instant;
import java.util.List;

public record ProblemDetailResponse(
        Long id,
        String title,
        String slug,
        String description,
        Difficulty difficulty,
        String inputFormat,
        String outputFormat,
        String constraints,
        String sampleInput,
        String sampleOutput,
        String explanation,
        int timeLimitMs,
        int memoryLimitMb,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String starterCode
) {}
