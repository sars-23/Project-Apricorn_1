package com.apricorn.insights.dto;

public record InsightCardResponse(
        String category,
        String headline,
        String evidence,
        String actionText,
        String actionUrl,
        String badgeColor
) {}
