package com.apricorn.analytics.service;

import com.apricorn.analytics.dto.*;
import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final StreakCalculator streakCalculator;

    public AnalyticsServiceImpl(
            UserRepository userRepository,
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository,
            StreakCalculator streakCalculator) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.streakCalculator = streakCalculator;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDashboardResponse getUserDashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        Long userId = user.getId();

        // 1. Summary Counts
        long totalSubmissions = submissionRepository.countByUserId(userId);
        long judgedSubmissions = submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.COMPLETED);
        long acceptedSubmissions = submissionRepository.countByUserIdAndStatusAndVerdict(userId, SubmissionStatus.COMPLETED, Verdict.ACCEPTED);
        Double acceptanceRate = judgedSubmissions > 0 ? Math.round(((double) acceptedSubmissions / judgedSubmissions * 100.0) * 10.0) / 10.0 : null;
        long problemsSolved = submissionRepository.countDistinctSolvedProblemsByUserId(userId);
        long totalCatalogProblems = problemRepository.count();
        long problemsAttempted = submissionRepository.countDistinctAttemptedProblemsByUserId(userId);

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                problemsSolved,
                totalCatalogProblems,
                totalSubmissions,
                judgedSubmissions,
                acceptedSubmissions,
                acceptanceRate
        );

        // 2. Difficulty Progress
        long easySolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.EASY);
        long totalEasy = problemRepository.countByDifficulty(Difficulty.EASY);
        long mediumSolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.MEDIUM);
        long totalMedium = problemRepository.countByDifficulty(Difficulty.MEDIUM);
        long hardSolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.HARD);
        long totalHard = problemRepository.countByDifficulty(Difficulty.HARD);

        DifficultyProgressResponse difficultyProgress = new DifficultyProgressResponse(
                easySolved, totalEasy,
                mediumSolved, totalMedium,
                hardSolved, totalHard
        );

        // 3. Verdict Distribution (Judged Submissions Only)
        List<Object[]> verdictCountsRaw = submissionRepository.findVerdictCountsByUserId(userId);
        Map<Verdict, Long> verdictCountMap = new EnumMap<>(Verdict.class);
        for (Object[] row : verdictCountsRaw) {
            if (row[0] != null && row[1] != null) {
                verdictCountMap.put((Verdict) row[0], ((Number) row[1]).longValue());
            }
        }

        List<VerdictStatResponse> verdictDistribution = new ArrayList<>();
        for (Verdict v : Verdict.values()) {
            long count = verdictCountMap.getOrDefault(v, 0L);
            if (count > 0) {
                double pct = judgedSubmissions > 0 ? Math.round(((double) count / judgedSubmissions * 100.0) * 10.0) / 10.0 : 0.0;
                verdictDistribution.add(new VerdictStatResponse(v, formatEnumName(v.name()), count, pct));
            }
        }

        // 4. Continuous 14-Day Activity
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(13);
        Instant startInstant = startDate.atStartOfDay(zone).toInstant();

        List<Object[]> activityRows = submissionRepository.findSubmissionsByUserIdSince(userId, startInstant);
        Map<LocalDate, long[]> dailyMap = new HashMap<>();
        for (Object[] row : activityRows) {
            Instant submittedAt = (Instant) row[0];
            Verdict verdict = (Verdict) row[1];
            if (submittedAt != null) {
                LocalDate dateKey = submittedAt.atZone(zone).toLocalDate();
                long[] counts = dailyMap.computeIfAbsent(dateKey, k -> new long[2]);
                counts[0]++; // total count
                if (verdict == Verdict.ACCEPTED) {
                    counts[1]++; // accepted count
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        List<DailyActivityResponse> activity = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            long[] counts = dailyMap.getOrDefault(d, new long[2]);
            activity.add(new DailyActivityResponse(d, d.format(formatter), counts[0], counts[1]));
        }

        // 5. Language Distribution
        List<Object[]> langCountsRaw = submissionRepository.findLanguageCountsByUserId(userId);
        Map<Language, Long> langCountMap = new EnumMap<>(Language.class);
        for (Object[] row : langCountsRaw) {
            if (row[0] != null && row[1] != null) {
                langCountMap.put((Language) row[0], ((Number) row[1]).longValue());
            }
        }

        List<LanguageStatResponse> languageDistribution = new ArrayList<>();
        for (Language l : Language.values()) {
            long count = langCountMap.getOrDefault(l, 0L);
            if (count > 0) {
                double pct = totalSubmissions > 0 ? Math.round(((double) count / totalSubmissions * 100.0) * 10.0) / 10.0 : 0.0;
                languageDistribution.add(new LanguageStatResponse(l, formatEnumName(l.name()), count, pct));
            }
        }

        // 6. Continue Solving (Top 3 attempted-unsolved problems)
        List<Object[]> continueRaw = submissionRepository.findRecentAttemptedUnsolvedProblemsByUserId(userId, PageRequest.of(0, 3));
        List<ContinueSolvingResponse> continueSolving = continueRaw.stream()
                .map(row -> new ContinueSolvingResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        (Difficulty) row[3],
                        (Instant) row[4]
                ))
                .toList();

        // 7. Recent Submissions
        List<Submission> recentSubmissionsList = submissionRepository.findTop10ByUserIdOrderBySubmittedAtDesc(userId);
        List<RecentSubmissionResponse> recentSubmissions = recentSubmissionsList.stream()
                .map(s -> new RecentSubmissionResponse(
                        s.getId(),
                        s.getProblem().getTitle(),
                        s.getProblem().getSlug(),
                        s.getLanguage(),
                        s.getStatus(),
                        s.getVerdict(),
                        s.getExecutionTimeMs(),
                        s.getMemoryUsedKb(),
                        s.getSubmittedAt()
                ))
                .toList();

        // 8. Streak
        List<Instant> timestamps = submissionRepository.findSubmissionTimestampsByUserId(userId);
        StreakCalculator.StreakResult streak = streakCalculator.calculateStreak(timestamps);

        double legacyAcceptanceRate = acceptanceRate != null ? acceptanceRate : 0.0;

        return new UserDashboardResponse(
                user.getUsername(),
                summary,
                difficultyProgress,
                verdictDistribution,
                activity,
                languageDistribution,
                recentSubmissions,
                continueSolving,
                totalSubmissions,
                acceptedSubmissions,
                legacyAcceptanceRate,
                problemsAttempted,
                problemsSolved,
                easySolved,
                mediumSolved,
                hardSolved,
                streak.currentStreak(),
                streak.longestStreak()
        );
    }

    private String formatEnumName(String name) {
        if (name == null || name.isEmpty()) return "";
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemStatsResponse getProblemStats(String problemSlug) {
        Problem problem = problemRepository.findBySlug(problemSlug)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + problemSlug));

        Long problemId = problem.getId();
        long totalSubmissions = submissionRepository.countByProblemId(problemId);
        long acceptedSubmissions = submissionRepository.countByProblemIdAndVerdict(problemId, Verdict.ACCEPTED);
        double acceptanceRate = totalSubmissions > 0 ? Math.round(((double) acceptedSubmissions / totalSubmissions * 100.0) * 10.0) / 10.0 : 0.0;
        long uniqueUsersAttempted = submissionRepository.countDistinctAttemptedUsersByProblemId(problemId);
        long uniqueUsersSolved = submissionRepository.countDistinctSolvedUsersByProblemId(problemId);

        return new ProblemStatsResponse(
                totalSubmissions,
                acceptedSubmissions,
                acceptanceRate,
                uniqueUsersAttempted,
                uniqueUsersSolved
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformAnalyticsResponse getAdminPlatformAnalytics() {
        long totalUsers = userRepository.count();
        long totalProblems = problemRepository.count();
        long totalSubmissions = submissionRepository.count();
        long acceptedSubmissions = submissionRepository.countByVerdict(Verdict.ACCEPTED);
        double overallAcceptanceRate = totalSubmissions > 0 ? Math.round(((double) acceptedSubmissions / totalSubmissions * 100.0) * 10.0) / 10.0 : 0.0;

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long activeUsersToday = submissionRepository.countActiveUsersToday(startOfDay);
        long submissionsToday = submissionRepository.countSubmissionsToday(startOfDay);

        Map<Difficulty, Long> difficultyDistribution = new EnumMap<>(Difficulty.class);
        for (Difficulty diff : Difficulty.values()) {
            difficultyDistribution.put(diff, problemRepository.countByDifficulty(diff));
        }

        Map<Verdict, Long> verdictDistribution = new EnumMap<>(Verdict.class);
        for (Verdict v : Verdict.values()) {
            verdictDistribution.put(v, submissionRepository.countByVerdict(v));
        }

        Map<Language, Long> languageDistribution = new EnumMap<>(Language.class);
        for (Language l : Language.values()) {
            languageDistribution.put(l, submissionRepository.countByLanguage(l));
        }

        return new PlatformAnalyticsResponse(
                totalUsers,
                totalProblems,
                totalSubmissions,
                acceptedSubmissions,
                overallAcceptanceRate,
                activeUsersToday,
                submissionsToday,
                difficultyDistribution,
                verdictDistribution,
                languageDistribution
        );
    }
}
