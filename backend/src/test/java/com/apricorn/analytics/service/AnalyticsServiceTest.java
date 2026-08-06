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

        UserDashboardResponse dashboard = analyticsService.getUserDashboard("new@example.com");

        assertThat(dashboard.username()).isEqualTo("new_user");
        assertThat(dashboard.summary().totalSubmissions()).isEqualTo(0L);
        assertThat(dashboard.summary().judgedSubmissions()).isEqualTo(0L);
        assertThat(dashboard.summary().acceptanceRate()).isNull();
        assertThat(dashboard.summary().totalProblemsSolved()).isEqualTo(0L);
        assertThat(dashboard.activity()).hasSize(14);
        assertThat(dashboard.verdictDistribution()).isEmpty();
        assertThat(dashboard.languageDistribution()).isEmpty();
        assertThat(dashboard.continueSolving()).isEmpty();
    }

    @Test
    void calculatesDashboardStatsAndDifficultyBreakdown() {
        User user = new User();
        user.setId(1L);
        user.setUsername("coder");
        user.setEmail("coder@example.com");

        when(userRepository.findByEmail("coder@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.countByUserId(1L)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndStatus(1L, com.apricorn.submission.entity.SubmissionStatus.COMPLETED)).thenReturn(8L);
        when(submissionRepository.countByUserIdAndStatusAndVerdict(1L, com.apricorn.submission.entity.SubmissionStatus.COMPLETED, Verdict.ACCEPTED)).thenReturn(4L);
        when(submissionRepository.countDistinctAttemptedProblemsByUserId(1L)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserId(1L)).thenReturn(3L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.EASY)).thenReturn(2L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.MEDIUM)).thenReturn(1L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.HARD)).thenReturn(0L);
        when(submissionRepository.findSubmissionTimestampsByUserId(1L)).thenReturn(List.of());
        when(submissionRepository.findTop10ByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of());

        UserDashboardResponse dashboard = analyticsService.getUserDashboard("coder@example.com");

        assertThat(dashboard.summary().totalSubmissions()).isEqualTo(10L);
        assertThat(dashboard.summary().judgedSubmissions()).isEqualTo(8L);
        assertThat(dashboard.summary().acceptedSubmissions()).isEqualTo(4L);
        assertThat(dashboard.summary().acceptanceRate()).isEqualTo(50.0);
        assertThat(dashboard.difficultyProgress().easySolved()).isEqualTo(2L);
        assertThat(dashboard.difficultyProgress().mediumSolved()).isEqualTo(1L);
        assertThat(dashboard.difficultyProgress().hardSolved()).isEqualTo(0L);
        assertThat(dashboard.activity()).hasSize(14);
    }

    @Test
    void calculatesVerdictAndLanguageDistributionAndContinueSolving() {
        User user = new User();
        user.setId(2L);
        user.setUsername("analyst");
        user.setEmail("analyst@example.com");

        when(userRepository.findByEmail("analyst@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.countByUserId(2L)).thenReturn(4L);
        when(submissionRepository.countByUserIdAndStatus(2L, com.apricorn.submission.entity.SubmissionStatus.COMPLETED)).thenReturn(4L);
        when(submissionRepository.countByUserIdAndStatusAndVerdict(2L, com.apricorn.submission.entity.SubmissionStatus.COMPLETED, Verdict.ACCEPTED)).thenReturn(2L);

        List<Object[]> verdictList = java.util.Arrays.<Object[]>asList(
                new Object[]{Verdict.ACCEPTED, 2L},
                new Object[]{Verdict.WRONG_ANSWER, 1L},
                new Object[]{Verdict.COMPILATION_ERROR, 1L}
        );
        when(submissionRepository.findVerdictCountsByUserId(2L)).thenReturn(verdictList);

        List<Object[]> langList = java.util.Arrays.<Object[]>asList(
                new Object[]{com.apricorn.submission.entity.Language.JAVA, 4L}
        );
        when(submissionRepository.findLanguageCountsByUserId(2L)).thenReturn(langList);

        List<Object[]> continueList = java.util.Arrays.<Object[]>asList(
                new Object[]{10L, "Max Subarray", "max-subarray", Difficulty.MEDIUM, java.time.Instant.now()}
        );
        when(submissionRepository.findRecentAttemptedUnsolvedProblemsByUserId(org.mockito.ArgumentMatchers.eq(2L), any())).thenReturn(continueList);

        UserDashboardResponse dashboard = analyticsService.getUserDashboard("analyst@example.com");

        assertThat(dashboard.verdictDistribution()).hasSize(3);
        assertThat(dashboard.verdictDistribution()).extracting("verdict").containsExactly(Verdict.ACCEPTED, Verdict.WRONG_ANSWER, Verdict.COMPILATION_ERROR);
        assertThat(dashboard.languageDistribution()).hasSize(1);
        assertThat(dashboard.languageDistribution().get(0).language()).isEqualTo(com.apricorn.submission.entity.Language.JAVA);
        assertThat(dashboard.continueSolving()).hasSize(1);
        assertThat(dashboard.continueSolving().get(0).slug()).isEqualTo("max-subarray");
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
