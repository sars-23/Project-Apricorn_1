package com.apricorn.submission.model;

import com.apricorn.submission.entity.Verdict;

public record JudgeResult(
        Verdict verdict,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        int passedTestCases,
        int totalTestCases,
        String errorMessage
) {}
