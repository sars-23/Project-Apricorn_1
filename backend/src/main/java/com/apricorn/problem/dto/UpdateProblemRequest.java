package com.apricorn.problem.dto;

import com.apricorn.problem.entity.Difficulty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateProblemRequest(
        @NotBlank @Size(min = 3, max = 255) String title,
        @NotBlank String description,
        @NotNull Difficulty difficulty,
        String inputFormat,
        String outputFormat,
        String constraints,
        String sampleInput,
        String sampleOutput,
        String explanation,
        @Min(1) Integer timeLimitMs,
        @Min(1) Integer memoryLimitMb,
        Set<Long> tagIds
) {}
