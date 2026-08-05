package com.apricorn.analytics.service;

import com.apricorn.analytics.dto.PlatformAnalyticsResponse;
import com.apricorn.analytics.dto.ProblemStatsResponse;
import com.apricorn.analytics.dto.RecentSubmissionResponse;
import com.apricorn.analytics.dto.UserDashboardResponse;
import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
        long totalSubmissions = submissionRepository.countByUserId(userId);
        long acceptedSubmissions = submissionRepository.countByUserIdAndVerdict(userId, Verdict.ACCEPTED);
        double acceptanceRate = totalSubmissions > 0 ? Math.round(((double) acceptedSubmissions / totalSubmissions * 100.0) * 10.0) / 10.0 : 0.0;
        long problemsAttempted = submissionRepository.countDistinctAttemptedProblemsByUserId(userId);
        long problemsSolved = submissionRepository.countDistinctSolvedProblemsByUserId(userId);

        long easySolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.EASY);
        long mediumSolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.MEDIUM);
        long hardSolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.HARD);

        List<Instant> timestamps = submissionRepository.findSubmissionTimestampsByUserId(userId);
        StreakCalculator.StreakResult streak = streakCalculator.calculateStreak(timestamps);

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

        return new UserDashboardResponse(
                user.getUsername(),
                totalSubmissions,
                acceptedSubmissions,
                acceptanceRate,
                problemsAttempted,
                problemsSolved,
                easySolved,
                mediumSolved,
                hardSolved,
                streak.currentStreak(),
                streak.longestStreak(),
                recentSubmissions
        );
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
