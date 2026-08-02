package com.apricorn.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {
    private StreakCalculator streakCalculator;

    @BeforeEach
    void setUp() {
        streakCalculator = new StreakCalculator();
    }

    @Test
    void handlesEmptyList() {
        StreakCalculator.StreakResult result = streakCalculator.calculateStreak(List.of());
        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.longestStreak()).isEqualTo(0);
    }

    @Test
    void calculatesLongestStreakAcrossGaps() {
        LocalDate d1 = LocalDate.of(2026, 8, 1);
        LocalDate d2 = LocalDate.of(2026, 8, 2);
        LocalDate d3 = LocalDate.of(2026, 8, 3);
        LocalDate d5 = LocalDate.of(2026, 8, 5);

        List<Instant> timestamps = List.of(
                d1.atStartOfDay(ZoneOffset.UTC).toInstant(),
                d2.atStartOfDay(ZoneOffset.UTC).toInstant(),
                d3.atStartOfDay(ZoneOffset.UTC).toInstant(),
                d5.atStartOfDay(ZoneOffset.UTC).toInstant()
        );

        StreakCalculator.StreakResult result = streakCalculator.calculateStreak(timestamps);
        assertThat(result.longestStreak()).isEqualTo(3);
    }

    @Test
    void calculatesCurrentStreakForToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        List<Instant> timestamps = List.of(
                dayBefore.atStartOfDay(ZoneOffset.UTC).toInstant(),
                yesterday.atStartOfDay(ZoneOffset.UTC).toInstant(),
                today.atStartOfDay(ZoneOffset.UTC).toInstant()
        );

        StreakCalculator.StreakResult result = streakCalculator.calculateStreak(timestamps);
        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.longestStreak()).isEqualTo(3);
    }

    @Test
    void calculatesCurrentStreakForYesterdayWhenNoSubmissionToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        List<Instant> timestamps = List.of(
                dayBefore.atStartOfDay(ZoneOffset.UTC).toInstant(),
                yesterday.atStartOfDay(ZoneOffset.UTC).toInstant()
        );

        StreakCalculator.StreakResult result = streakCalculator.calculateStreak(timestamps);
        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.longestStreak()).isEqualTo(2);
    }

    @Test
    void resetsCurrentStreakWhenNoSubmissionTodayOrYesterday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate threeDaysAgo = today.minusDays(3);

        List<Instant> timestamps = List.of(
                threeDaysAgo.atStartOfDay(ZoneOffset.UTC).toInstant()
        );

        StreakCalculator.StreakResult result = streakCalculator.calculateStreak(timestamps);
        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.longestStreak()).isEqualTo(1);
    }
}
