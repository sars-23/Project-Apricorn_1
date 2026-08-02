package com.apricorn.analytics.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class StreakCalculator {
    public record StreakResult(int currentStreak, int longestStreak) {}

    public StreakResult calculateStreak(List<Instant> timestamps) {
        if (timestamps == null || timestamps.isEmpty()) {
            return new StreakResult(0, 0);
        }

        // Convert timestamps to unique UTC calendar dates sorted ascending
        Set<LocalDate> dateSet = new TreeSet<>();
        for (Instant timestamp : timestamps) {
            if (timestamp != null) {
                dateSet.add(timestamp.atZone(ZoneOffset.UTC).toLocalDate());
            }
        }

        if (dateSet.isEmpty()) {
            return new StreakResult(0, 0);
        }

        List<LocalDate> sortedDates = List.copyOf(dateSet);

        // 1. Calculate longest streak
        int longestStreak = 0;
        int currentCount = 0;
        LocalDate previousDate = null;

        for (LocalDate date : sortedDates) {
            if (previousDate == null || previousDate.plusDays(1).equals(date)) {
                currentCount++;
            } else {
                currentCount = 1;
            }
            longestStreak = Math.max(longestStreak, currentCount);
            previousDate = date;
        }

        // 2. Calculate current streak (anchored at today UTC or yesterday UTC)
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);

        int currentStreak = 0;
        LocalDate checkDate = null;

        if (dateSet.contains(today)) {
            checkDate = today;
        } else if (dateSet.contains(yesterday)) {
            checkDate = yesterday;
        }

        if (checkDate != null) {
            while (dateSet.contains(checkDate)) {
                currentStreak++;
                checkDate = checkDate.minusDays(1);
            }
        }

        return new StreakResult(currentStreak, longestStreak);
    }
}
