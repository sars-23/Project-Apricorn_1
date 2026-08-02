package com.apricorn.judge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apricorn.judge.config.JudgeProperties;
import com.apricorn.judge.executor.LanguageExecutor;
import com.apricorn.judge.runner.DockerRunner;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.TestCaseRepository;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.model.JudgeResult;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.submission.service.SubmissionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DockerJudgeServiceTest {
    private SubmissionRepository submissionRepository;
    private TestCaseRepository testCaseRepository;
    private SubmissionService submissionService;
    private LanguageExecutor languageExecutor;
    private DockerRunner dockerRunner;
    private DockerJudgeService dockerJudgeService;

    @BeforeEach
    void setUp() {
        submissionRepository = mock(SubmissionRepository.class);
        testCaseRepository = mock(TestCaseRepository.class);
        submissionService = mock(SubmissionService.class);
        languageExecutor = mock(LanguageExecutor.class);
        dockerRunner = mock(DockerRunner.class);

        dockerJudgeService = new DockerJudgeService(
                submissionRepository,
                testCaseRepository,
                submissionService,
                List.of(languageExecutor),
                new JudgeProperties(),
                dockerRunner
        );
    }

    @Test
    void updatesInternalErrorWhenDockerUnavailable() {
        when(dockerRunner.isDockerAvailable()).thenReturn(false);

        dockerJudgeService.evaluate(100L);

        verify(submissionService).markAsQueued(100L);
        verify(submissionService).markAsRunning(100L);

        ArgumentCaptor<JudgeResult> captor = ArgumentCaptor.forClass(JudgeResult.class);
        verify(submissionService).updateJudgeResult(eq(100L), captor.capture());

        assertThat(captor.getValue().verdict()).isEqualTo(Verdict.INTERNAL_ERROR);
        assertThat(captor.getValue().errorMessage()).contains("Docker runtime environment is unavailable");
    }

    @Test
    void delegatesEvaluationToLanguageExecutor() {
        Problem problem = new Problem();
        problem.setId(10L);

        Submission submission = new Submission();
        submission.setId(100L);
        submission.setLanguage(Language.JAVA);
        submission.setProblem(problem);

        when(dockerRunner.isDockerAvailable()).thenReturn(true);
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        when(languageExecutor.supports(Language.JAVA)).thenReturn(true);
        when(languageExecutor.execute(any(), any(), any()))
                .thenReturn(new JudgeResult(Verdict.ACCEPTED, 40, 15000, 5, 5, null));

        dockerJudgeService.evaluate(100L);

        verify(submissionService).markAsQueued(100L);
        verify(submissionService).markAsRunning(100L);

        ArgumentCaptor<JudgeResult> captor = ArgumentCaptor.forClass(JudgeResult.class);
        verify(submissionService).updateJudgeResult(eq(100L), captor.capture());

        assertThat(captor.getValue().verdict()).isEqualTo(Verdict.ACCEPTED);
    }
}
