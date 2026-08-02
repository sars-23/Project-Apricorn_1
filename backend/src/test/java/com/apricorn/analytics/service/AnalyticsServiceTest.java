package com.apricorn.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.analytics.dto.PlatformAnalyticsResponse;
import com.apricorn.analytics.dto.ProblemStatsResponse;
import com.apricorn.analytics.dto.UserDashboardResponse;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {
    private UserRepository userRepository;
    private ProblemRepository problemRepository;
    private SubmissionRepository submissionRepository;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        problemRepository = mock(ProblemRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        analyticsService = new AnalyticsServiceImpl(
                userRepository,
                problemRepository,
                submissionRepository,
                new StreakCalculator()
        );
    }

    @Test
    void calculatesDashboardForZeroSubmissionUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("new_user");
        user.setEmail("new@example.com");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.countByUserId(1L)).thenReturn(0L);
        when(submissionRepository.countByUserIdAndVerdict(1L, Verdict.ACCEPTED)).thenReturn(0L);

        UserDashboardResponse dashboard = analyticsService.getUserDashboard("new@example.com");

        assertThat(dashboard.username()).isEqualTo("new_user");
        assertThat(dashboard.totalSubmissions()).isEqualTo(0L);
        assertThat(dashboard.acceptanceRate()).isEqualTo(0.0);
        assertThat(dashboard.problemsSolved()).isEqualTo(0L);
        assertThat(dashboard.currentStreak()).isEqualTo(0);
    }

    @Test
    void calculatesDashboardStatsAndDifficultyBreakdown() {
        User user = new User();
        user.setId(1L);
        user.setUsername("coder");
        user.setEmail("coder@example.com");

        when(userRepository.findByEmail("coder@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.countByUserId(1L)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndVerdict(1L, Verdict.ACCEPTED)).thenReturn(4L);
        when(submissionRepository.countDistinctAttemptedProblemsByUserId(1L)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserId(1L)).thenReturn(3L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.EASY)).thenReturn(2L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.MEDIUM)).thenReturn(1L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.HARD)).thenReturn(0L);
        when(submissionRepository.findSubmissionTimestampsByUserId(1L)).thenReturn(List.of());
        when(submissionRepository.findTop10ByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of());

        UserDashboardResponse dashboard = analyticsService.getUserDashboard("coder@example.com");

        assertThat(dashboard.totalSubmissions()).isEqualTo(10L);
        assertThat(dashboard.acceptedSubmissions()).isEqualTo(4L);
        assertThat(dashboard.acceptanceRate()).isEqualTo(40.0);
        assertThat(dashboard.easySolved()).isEqualTo(2L);
        assertThat(dashboard.mediumSolved()).isEqualTo(1L);
        assertThat(dashboard.hardSolved()).isEqualTo(0L);
    }

    @Test
    void calculatesProblemStats() {
        Problem problem = new Problem();
        problem.setId(10L);
        problem.setSlug("two-sum");

        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(submissionRepository.countByProblemId(10L)).thenReturn(100L);
        when(submissionRepository.countByProblemIdAndVerdict(10L, Verdict.ACCEPTED)).thenReturn(45L);
        when(submissionRepository.countDistinctAttemptedUsersByProblemId(10L)).thenReturn(30L);
        when(submissionRepository.countDistinctSolvedUsersByProblemId(10L)).thenReturn(20L);

        ProblemStatsResponse stats = analyticsService.getProblemStats("two-sum");

        assertThat(stats.totalSubmissions()).isEqualTo(100L);
        assertThat(stats.acceptedSubmissions()).isEqualTo(45L);
        assertThat(stats.acceptanceRate()).isEqualTo(45.0);
        assertThat(stats.uniqueUsersAttempted()).isEqualTo(30L);
        assertThat(stats.uniqueUsersSolved()).isEqualTo(20L);
    }

    @Test
    void calculatesAdminPlatformAnalytics() {
        when(userRepository.count()).thenReturn(50L);
        when(problemRepository.count()).thenReturn(20L);
        when(submissionRepository.count()).thenReturn(500L);
        when(submissionRepository.countByVerdict(Verdict.ACCEPTED)).thenReturn(200L);
        when(submissionRepository.countActiveUsersToday(any())).thenReturn(15L);
        when(submissionRepository.countSubmissionsToday(any())).thenReturn(80L);

        PlatformAnalyticsResponse analytics = analyticsService.getAdminPlatformAnalytics();

        assertThat(analytics.totalUsers()).isEqualTo(50L);
        assertThat(analytics.totalProblems()).isEqualTo(20L);
        assertThat(analytics.totalSubmissions()).isEqualTo(500L);
        assertThat(analytics.acceptedSubmissions()).isEqualTo(200L);
        assertThat(analytics.overallAcceptanceRate()).isEqualTo(40.0);
        assertThat(analytics.activeUsersToday()).isEqualTo(15L);
        assertThat(analytics.submissionsToday()).isEqualTo(80L);
    }
}
