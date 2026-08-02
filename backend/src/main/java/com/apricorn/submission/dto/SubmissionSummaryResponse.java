package com.apricorn.submission.dto;

import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import java.time.Instant;

public record SubmissionSummaryResponse(
        Long id,
        ProblemReferenceResponse problem,
        Language language,
        SubmissionStatus status,
        Verdict verdict,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        int passedTestCases,
        int totalTestCases,
        Instant submittedAt,
        Instant completedAt
) {}
