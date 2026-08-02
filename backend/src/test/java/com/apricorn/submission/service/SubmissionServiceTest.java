package com.apricorn.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apricorn.exception.InvalidSubmissionException;
import com.apricorn.exception.InvalidSubmissionStateException;
import com.apricorn.exception.UnauthorizedSubmissionAccessException;
import com.apricorn.judge.service.JudgeService;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.dto.CreateSubmissionRequest;
import com.apricorn.submission.dto.SubmissionCreatedResponse;
import com.apricorn.submission.dto.SubmissionDetailResponse;
import com.apricorn.submission.dto.UserSubmissionStatsResponse;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.mapper.SubmissionMapper;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {
    private SubmissionRepository submissionRepository;
    private ProblemRepository problemRepository;
    private UserRepository userRepository;
    private JudgeService judgeService;
    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionRepository = mock(SubmissionRepository.class);
        problemRepository = mock(ProblemRepository.class);
        userRepository = mock(UserRepository.class);
        judgeService = mock(JudgeService.class);
        submissionService = new SubmissionServiceImpl(
                submissionRepository,
                problemRepository,
                userRepository,
                new SubmissionMapper(),
                judgeService
        );
    }

    @Test
    void createsSubmissionSuccessfully() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        Problem problem = new Problem();
        problem.setId(10L);
        problem.setSlug("two-sum");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        CreateSubmissionRequest request = new CreateSubmissionRequest(Language.JAVA, "public class Main {}");
        SubmissionCreatedResponse response = submissionService.createSubmission("two-sum", request, "user@example.com");

        assertThat(response.submissionId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING);
        verify(judgeService).evaluate(100L);
    }

    @Test
    void rejectsEmptySourceCode() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(Language.JAVA, "  ");
        assertThatThrownBy(() -> submissionService.createSubmission("two-sum", request, "user@example.com"))
                .isInstanceOf(InvalidSubmissionException.class);
    }

    @Test
    void rejectsOversizedSourceCode() {
        String oversized = "a".repeat(65537);
        CreateSubmissionRequest request = new CreateSubmissionRequest(Language.JAVA, oversized);
        assertThatThrownBy(() -> submissionService.createSubmission("two-sum", request, "user@example.com"))
                .isInstanceOf(InvalidSubmissionException.class);
    }

    @Test
    void allowsOwnerToGetSubmissionDetail() {
        User user = new User();
        user.setEmail("owner@example.com");

        Submission submission = new Submission();
        submission.setId(100L);
        submission.setUser(user);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Main {}");

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        SubmissionDetailResponse detail = submissionService.getSubmissionById(100L, "owner@example.com", false);

        assertThat(detail.id()).isEqualTo(100L);
        assertThat(detail.sourceCode()).isEqualTo("public class Main {}");
    }

    @Test
    void rejectsNonOwnerFromViewingSubmissionDetail() {
        User owner = new User();
        owner.setEmail("owner@example.com");

        Submission submission = new Submission();
        submission.setId(100L);
        submission.setUser(owner);

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> submissionService.getSubmissionById(100L, "other@example.com", false))
                .isInstanceOf(UnauthorizedSubmissionAccessException.class);
    }

    @Test
    void allowsAdminToViewAnySubmissionDetail() {
        User owner = new User();
        owner.setEmail("owner@example.com");

        Submission submission = new Submission();
        submission.setId(100L);
        submission.setUser(owner);
        submission.setSourceCode("class Test {}");

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        SubmissionDetailResponse detail = submissionService.getSubmissionById(100L, "admin@example.com", true);

        assertThat(detail.sourceCode()).isEqualTo("class Test {}");
    }

    @Test
    void validatesStateTransitions() {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setStatus(SubmissionStatus.COMPLETED);

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> submissionService.markAsQueued(1L))
                .isInstanceOf(InvalidSubmissionStateException.class);
    }

    @Test
    void calculatesUserSubmissionStats() {
        User user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.countByUserId(5L)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndVerdict(5L, Verdict.ACCEPTED)).thenReturn(4L);
        when(submissionRepository.countDistinctAttemptedProblemsByUserId(5L)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserId(5L)).thenReturn(3L);

        UserSubmissionStatsResponse stats = submissionService.getUserSubmissionStats("user@example.com");

        assertThat(stats.totalSubmissions()).isEqualTo(10L);
        assertThat(stats.acceptedSubmissions()).isEqualTo(4L);
        assertThat(stats.acceptanceRate()).isEqualTo(40.0);
        assertThat(stats.problemsAttempted()).isEqualTo(5L);
        assertThat(stats.problemsSolved()).isEqualTo(3L);
    }
}
