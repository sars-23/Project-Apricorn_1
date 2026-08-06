package com.apricorn.analytics.dto;

import com.apricorn.submission.entity.Language;

public record LanguageStatResponse(
        Language language,
        String label,
        long count,
        double percentage
) {}
