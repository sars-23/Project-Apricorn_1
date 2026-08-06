package com.apricorn.analytics.dto;

import com.apricorn.submission.entity.Verdict;

public record VerdictStatResponse(
        Verdict verdict,
        String label,
        long count,
        double percentage
) {}
