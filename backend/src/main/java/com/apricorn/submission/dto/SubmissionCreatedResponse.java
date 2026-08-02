package com.apricorn.submission.dto;

import com.apricorn.submission.entity.SubmissionStatus;
import java.time.Instant;

public record SubmissionCreatedResponse(
        Long submissionId,
        SubmissionStatus status,
        Instant submittedAt
) {}
