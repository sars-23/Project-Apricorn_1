package com.apricorn.analytics.dto;

import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import java.time.Instant;

public record RecentSubmissionResponse(
        Long submissionId,
        String problemTitle,
        String problemSlug,
        Language language,
        SubmissionStatus status,
        Verdict verdict,
        Integer runtimeMs,
        Integer memoryKb,
        Instant submittedAt
) {}
