package com.apricorn.analytics.dto;

import java.time.LocalDate;

public record DailyActivityResponse(
        LocalDate date,
        String displayDate,
        long submissionCount,
        long acceptedCount
) {}
