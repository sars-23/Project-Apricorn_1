package com.apricorn.journey.dto;

import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import java.time.Instant;

public record JourneyAttemptResponse(
        int attemptNumber,
        Long submissionId,
        Language language,
        SubmissionStatus status,
        Verdict verdict,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        int passedTestCases,
        int totalTestCases,
        Instant submittedAt,
        String sourceCode,
        String errorMessage,
        boolean isFirstAccepted,
        boolean isBestRuntime,
        boolean isBestMemory
) {}
