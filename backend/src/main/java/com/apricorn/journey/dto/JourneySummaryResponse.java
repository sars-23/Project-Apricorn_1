package com.apricorn.journey.dto;

import java.time.Instant;

public record JourneySummaryResponse(
        long totalAttempts,
        Instant firstAttemptAt,
        Integer firstAcceptedAttemptNumber,
        Integer attemptsUntilFirstAccepted,
        long acceptedSubmissionsCount,
        Integer bestRuntimeMs,
        Integer bestMemoryKb,
        Instant latestAttemptAt,
        Integer firstAcceptedRuntimeMs,
        Integer runtimeImprovementMs,
        Double runtimeImprovementPercent
) {}
