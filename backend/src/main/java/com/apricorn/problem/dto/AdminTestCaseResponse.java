package com.apricorn.problem.dto;

import java.time.Instant;

public record AdminTestCaseResponse(
        Long id,
        Long problemId,
        String input,
        String expectedOutput,
        boolean isSample,
        int orderIndex,
        Instant createdAt
) {}
