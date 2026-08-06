package com.apricorn.insights.dto;

import com.apricorn.submission.entity.Verdict;

public record FailurePatternResponse(
        Verdict verdict,
        String label,
        long count,
        double percentage
) {}
