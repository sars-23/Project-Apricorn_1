package com.apricorn.journey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.journey.dto.SolutionJourneyResponse;
import com.apricorn.problem.entity.ProblemStatus;
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

class JourneyServiceTest {
    private ProblemRepository problemRepository;
    private UserRepository userRepository;
    private SubmissionRepository submissionRepository;
    private JourneyService journeyService;

    private User userA;
    private User userB;
    private Problem problemX;

    @BeforeEach
    void setUp() {
        problemRepository = mock(ProblemRepository.class);
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        journeyService = new JourneyServiceImpl(problemRepository, userRepository, submissionRepository);

        userA = new User();
        userA.setId(1L);
        userA.setUsername("user_a");
        userA.setEmail("usera@example.com");

        userB = new User();
        userB.setId(2L);
        userB.setUsername("user_b");
        userB.setEmail("userb@example.com");

        problemX = new Problem();
        problemX.setId(10L);
        problemX.setTitle("Two Sum");
        problemX.setSlug("two-sum");
        problemX.setDifficulty(Difficulty.EASY);

        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problemX));
        when(userRepository.findByEmail("usera@example.com")).thenReturn(Optional.of(userA));
        when(userRepository.findByEmail("userb@example.com")).thenReturn(Optional.of(userB));
    }

    @Test
    void throwsExceptionForInvalidProblemSlug() {
        when(problemRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> journeyService.getSolutionJourney("usera@example.com", "unknown"))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void rendersEmptyJourneyWhenNoAttemptsExist() {
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of());

        SolutionJourneyResponse journey = journeyService.getSolutionJourney("usera@example.com", "two-sum");

        assertThat(journey.userStatus()).isEqualTo(ProblemStatus.UNSOLVED);
        assertThat(journey.summary().totalAttempts()).isEqualTo(0L);
        assertThat(journey.summary().firstAcceptedAttemptNumber()).isNull();
        assertThat(journey.summary().bestRuntimeMs()).isNull();
        assertThat(journey.attempts()).isEmpty();
        assertThat(journey.journeyStory()).contains("No attempts recorded");
    }

    @Test
    void rendersOneFailedAttemptJourney() {
        Submission s1 = createSubmission(1L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.WRONG_ANSWER, 50, 15000, Instant.now().minusSeconds(300));
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(s1));

        SolutionJourneyResponse journey = journeyService.getSolutionJourney("usera@example.com", "two-sum");

        assertThat(journey.userStatus()).isEqualTo(ProblemStatus.ATTEMPTED);
        assertThat(journey.summary().totalAttempts()).isEqualTo(1L);
        assertThat(journey.summary().firstAcceptedAttemptNumber()).isNull();
        assertThat(journey.summary().bestRuntimeMs()).isNull();
        assertThat(journey.attempts()).hasSize(1);
        assertThat(journey.attempts().get(0).attemptNumber()).isEqualTo(1);
        assertThat(journey.journeyStory()).contains("1 attempt(s) so far");
    }

    @Test
    void rendersFirstAttemptAcceptedJourney() {
        Submission s1 = createSubmission(1L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 40, 15000, Instant.now().minusSeconds(300));
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(s1));

        SolutionJourneyResponse journey = journeyService.getSolutionJourney("usera@example.com", "two-sum");

        assertThat(journey.userStatus()).isEqualTo(ProblemStatus.SOLVED);
        assertThat(journey.summary().totalAttempts()).isEqualTo(1L);
        assertThat(journey.summary().firstAcceptedAttemptNumber()).isEqualTo(1);
        assertThat(journey.summary().attemptsUntilFirstAccepted()).isEqualTo(1);
        assertThat(journey.summary().bestRuntimeMs()).isEqualTo(40);
        assertThat(journey.summary().bestMemoryKb()).isEqualTo(15000);
        assertThat(journey.attempts().get(0).isFirstAccepted()).isTrue();
        assertThat(journey.attempts().get(0).isBestRuntime()).isTrue();
        assertThat(journey.attempts().get(0).isBestMemory()).isTrue();
        assertThat(journey.journeyStory()).isEqualTo("Solved on your first attempt.");
    }

    @Test
    void calculatesFailedAttemptsThenAccepted() {
        Instant now = Instant.now();
        Submission s1 = createSubmission(1L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.WRONG_ANSWER, 55, 15000, now.minusSeconds(300));
        Submission s2 = createSubmission(2L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.COMPILATION_ERROR, null, null, now.minusSeconds(200));
        Submission s3 = createSubmission(3L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 60, 16000, now.minusSeconds(100));

        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(s1, s2, s3));

        SolutionJourneyResponse journey = journeyService.getSolutionJourney("usera@example.com", "two-sum");

        assertThat(journey.userStatus()).isEqualTo(ProblemStatus.SOLVED);
        assertThat(journey.summary().totalAttempts()).isEqualTo(3L);
        assertThat(journey.summary().firstAcceptedAttemptNumber()).isEqualTo(3);
        assertThat(journey.summary().attemptsUntilFirstAccepted()).isEqualTo(3);
        assertThat(journey.summary().bestRuntimeMs()).isEqualTo(60);
        assertThat(journey.attempts().get(2).isFirstAccepted()).isTrue();
        assertThat(journey.journeyStory()).contains("Accepted after 3 attempts.");
    }

    @Test
    void calculatesMultipleAcceptedWithImprovementAndTieBreaking() {
        Instant now = Instant.now();
        Submission s1 = createSubmission(1L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.WRONG_ANSWER, 55, 15000, now.minusSeconds(400));
        Submission s2 = createSubmission(2L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 60, 16000, now.minusSeconds(300));
        Submission s3 = createSubmission(3L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 45, 14000, now.minusSeconds(200));
        Submission s4 = createSubmission(4L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 45, 14000, now.minusSeconds(100));

        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(s1, s2, s3, s4));

        SolutionJourneyResponse journey = journeyService.getSolutionJourney("usera@example.com", "two-sum");

        assertThat(journey.summary().firstAcceptedAttemptNumber()).isEqualTo(2);
        assertThat(journey.summary().attemptsUntilFirstAccepted()).isEqualTo(2);
        assertThat(journey.summary().bestRuntimeMs()).isEqualTo(45);
        assertThat(journey.summary().runtimeImprovementMs()).isEqualTo(15);
        assertThat(journey.summary().runtimeImprovementPercent()).isEqualTo(25.0);

        // Attempt #2 is First Accepted
        assertThat(journey.attempts().get(1).isFirstAccepted()).isTrue();
        assertThat(journey.attempts().get(1).isBestRuntime()).isFalse();

        // Attempt #3 is Best Runtime (earliest tie winner)
        assertThat(journey.attempts().get(2).isFirstAccepted()).isFalse();
        assertThat(journey.attempts().get(2).isBestRuntime()).isTrue();

        // Attempt #4 does NOT get Best Runtime marker due to tie breaking
        assertThat(journey.attempts().get(3).isBestRuntime()).isFalse();

        assertThat(journey.journeyStory()).contains("Accepted after 2 attempts. You submitted 2 more accepted solution(s) afterward. Best runtime improved from 60 ms to 45 ms (25.0%).");
    }

    @Test
    void verifiesCrossUserIsolation() {
        Submission sUserA = createSubmission(1L, userA, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 50, 15000, Instant.now());
        Submission sUserB = createSubmission(2L, userB, problemX, Language.JAVA, SubmissionStatus.COMPLETED, Verdict.ACCEPTED, 40, 14000, Instant.now());

        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(1L, 10L)).thenReturn(List.of(sUserA));
        when(submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(2L, 10L)).thenReturn(List.of(sUserB));

        SolutionJourneyResponse journeyA = journeyService.getSolutionJourney("usera@example.com", "two-sum");
        SolutionJourneyResponse journeyB = journeyService.getSolutionJourney("userb@example.com", "two-sum");

        assertThat(journeyA.summary().bestRuntimeMs()).isEqualTo(50);
        assertThat(journeyB.summary().bestRuntimeMs()).isEqualTo(40);
        assertThat(journeyA.attempts()).extracting("submissionId").containsExactly(1L);
        assertThat(journeyB.attempts()).extracting("submissionId").containsExactly(2L);
    }

    private Submission createSubmission(Long id, User user, Problem problem, Language lang, SubmissionStatus status, Verdict verdict, Integer runtimeMs, Integer memoryKb, Instant submittedAt) {
        Submission s = new Submission();
        s.setId(id);
        s.setUser(user);
        s.setProblem(problem);
        s.setLanguage(lang);
        s.setSourceCode("public class Solution {}");
        s.setStatus(status);
        s.setVerdict(verdict);
        s.setExecutionTimeMs(runtimeMs);
        s.setMemoryUsedKb(memoryKb);
        return s;
    }
}
