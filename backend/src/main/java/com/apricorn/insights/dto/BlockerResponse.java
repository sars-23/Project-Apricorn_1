package com.apricorn.insights.dto;

import com.apricorn.problem.entity.Difficulty;
import com.apricorn.submission.entity.Verdict;
import java.time.Instant;

public record BlockerResponse(
        Long problemId,
        String title,
        String slug,
        Difficulty difficulty,
        long attemptCount,
        Verdict latestVerdict,
        Instant latestAttemptAt
) {}
