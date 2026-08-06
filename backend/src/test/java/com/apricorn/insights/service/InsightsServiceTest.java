package com.apricorn.insights.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.insights.dto.UserInsightsResponse;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InsightsServiceTest {
    private ProblemRepository problemRepository;
    private UserRepository userRepository;
    private SubmissionRepository submissionRepository;
    private InsightsService insightsService;

    private User userA;
    private User userB;
    private Problem problemA;
    private Problem problemB;

    @BeforeEach
    void setUp() {
        problemRepository = mock(ProblemRepository.class);
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        insightsService = new InsightsServiceImpl(problemRepository, userRepository, submissionRepository);

        userA = new User();
        userA.setId(1L);
        userA.setUsername("usera");
        userA.setEmail("usera@example.com");

        userB = new User();
        userB.setId(2L);
        userB.setUsername("userb");
        userB.setEmail("userb@example.com");

        problemA = new Problem();
        problemA.setId(10L);
        problemA.setTitle("Two Sum");
        problemA.setSlug("two-sum");
        problemA.setDifficulty(Difficulty.EASY);

        problemB = new Problem();
        problemB.setId(20L);
        problemB.setTitle("Add Two Numbers");
        problemB.setSlug("add-two-numbers");
        problemB.setDifficulty(Difficulty.MEDIUM);

        when(userRepository.findByEmail("usera@example.com")).thenReturn(Optional.of(userA));
        when(userRepository.findByEmail("userb@example.com")).thenReturn(Optional.of(userB));
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problemA));
    }

    @Test
    void emptyUserRendersCleanResponse() {
        when(submissionRepository.findDistinctProblemIdsByUserId(1L)).thenReturn(List.of());

        UserInsightsResponse insights = insightsService.getUserInsights("usera@example.com");

        assertThat(insights.summary().problemsAttempted()).isEqualTo(0);
        assertThat(insights.summary().problemsSolved()).isEqualTo(0);
        assertThat(insights.summary().firstTrySolves()).isEqualTo(0);
        assertThat(insights.summary().averageAttemptsToSolve()).isNull();
        assertThat(insights.summary().acceptedSubmissionRate()).isNull();
        assertThat(insights.cards()).isEmpty();
        assertThat(insights.blockers()).isEmpty();
        assertThat(insights.failurePatterns()).isEmpty();
    }

    @Test
    void calculatesFirstTrySolvesAndAverageAttempts() {
        when(submissionRepository.findDistinctProblemIdsByUserId(1L)).thenReturn(List.of(10L, 20L));

        Instant now = Instant.now();
        // Problem A: First-try solve (1 attempt)
        Submission sA1 = createSubmission(1L, userA, problemA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 40, 15000, now.minusSeconds(500));
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(sA1));

        // Problem B: Solved on attempt #3
        Submission sB1 = createSubmission(2L, userA, problemB, SubmissionStatus.COMPLETED, Verdict.WRONG_ANSWER, 50, 15000, now.minusSeconds(400));
        Submission sB2 = createSubmission(3L, userA, problemB, SubmissionStatus.COMPLETED, Verdict.COMPILATION_ERROR, null, null, now.minusSeconds(300));
        Submission sB3 = createSubmission(4L, userA, problemB, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 60, 16000, now.minusSeconds(200));
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 20L)).thenReturn(List.of(sB1, sB2, sB3));

        when(submissionRepository.countByUserIdAndStatus(1L, SubmissionStatus.COMPLETED)).thenReturn(4L);
        when(submissionRepository.countByUserIdAndStatusAndVerdict(1L, SubmissionStatus.COMPLETED, Verdict.ACCEPTED)).thenReturn(2L);
        when(submissionRepository.findSubmissionsByUserIdSince(1L, now.minus(7, java.time.temporal.ChronoUnit.DAYS))).thenReturn(List.of());

        UserInsightsResponse insights = insightsService.getUserInsights("usera@example.com");

        assertThat(insights.summary().problemsAttempted()).isEqualTo(2);
        assertThat(insights.summary().problemsSolved()).isEqualTo(2);
        assertThat(insights.summary().firstTrySolves()).isEqualTo(1);
        assertThat(insights.summary().averageAttemptsToSolve()).isEqualTo(2.0); // (1 + 3) / 2 = 2.0
        assertThat(insights.summary().acceptedSubmissionRate()).isEqualTo(50.0);
    }

    @Test
    void excludesInternalErrorFromUserFailurePatterns() {
        when(submissionRepository.findDistinctProblemIdsByUserId(1L)).thenReturn(List.of(10L));

        Instant now = Instant.now();
        Submission s1 = createSubmission(1L, userA, problemA, SubmissionStatus.COMPLETED, Verdict.INTERNAL_ERROR, 0, 0, now.minusSeconds(300));
        Submission s2 = createSubmission(2L, userA, problemA, SubmissionStatus.COMPLETED, Verdict.WRONG_ANSWER, 45, 15000, now.minusSeconds(200));
        Submission s3 = createSubmission(3L, userA, problemA, SubmissionStatus.COMPLETED, Verdict.WRONG_ANSWER, 45, 15000, now.minusSeconds(100));
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(s1, s2, s3));

        UserInsightsResponse insights = insightsService.getUserInsights("usera@example.com");

        // INTERNAL_ERROR must NOT be present in failurePatterns
        assertThat(insights.failurePatterns()).noneMatch(fp -> fp.verdict() == Verdict.INTERNAL_ERROR);
        assertThat(insights.failurePatterns()).anyMatch(fp -> fp.verdict() == Verdict.WRONG_ANSWER && fp.count() == 2);
    }

    @Test
    void verifiesSmallSampleBehaviorForDifficultyStrength() {
        when(submissionRepository.findDistinctProblemIdsByUserId(1L)).thenReturn(List.of(10L));

        // 1 Easy problem solved
        Submission s1 = createSubmission(1L, userA, problemA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 40, 15000, Instant.now());
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(s1));

        UserInsightsResponse insights = insightsService.getUserInsights("usera@example.com");

        // Should NOT create DIFFICULTY STRENGTH card because evidence threshold requires >= 2 solved problems
        assertThat(insights.cards()).noneMatch(card -> card.category().equals("DIFFICULTY STRENGTH"));
    }

    @Test
    void verifiesCrossUserIsolation() {
        when(submissionRepository.findDistinctProblemIdsByUserId(1L)).thenReturn(List.of(10L));
        when(submissionRepository.findDistinctProblemIdsByUserId(2L)).thenReturn(List.of());

        Submission sUserA = createSubmission(1L, userA, problemA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 50, 15000, Instant.now());
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(sUserA));

        UserInsightsResponse insightsA = insightsService.getUserInsights("usera@example.com");
        UserInsightsResponse insightsB = insightsService.getUserInsights("userb@example.com");

        assertThat(insightsA.summary().problemsAttempted()).isEqualTo(1);
        assertThat(insightsB.summary().problemsAttempted()).isEqualTo(0);
    }

    private Submission createSubmission(Long id, User user, Problem problem, SubmissionStatus status, Verdict verdict, Integer runtimeMs, Integer memoryKb, Instant submittedAt) {
        Submission s = new Submission();
        s.setId(id);
        s.setUser(user);
        s.setProblem(problem);
        s.setLanguage(Language.JAVA);
        s.setSourceCode("public class Solution {}");
        s.setStatus(status);
        s.setVerdict(verdict);
        s.setExecutionTimeMs(runtimeMs);
        s.setMemoryUsedKb(memoryKb);
        s.setSubmittedAt(submittedAt);
        return s;
    }
}
