package com.apricorn.judge.service;

import com.apricorn.judge.config.JudgeProperties;
import com.apricorn.judge.executor.LanguageExecutor;
import com.apricorn.judge.runner.DockerRunner;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.problem.repository.TestCaseRepository;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.model.JudgeResult;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.submission.service.SubmissionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "judge.mode", havingValue = "docker", matchIfMissing = true)
public class DockerJudgeService implements JudgeService {
    private static final Logger log = LoggerFactory.getLogger(DockerJudgeService.class);

    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionService submissionService;
    private final List<LanguageExecutor> languageExecutors;
    private final JudgeProperties judgeProperties;
    private final DockerRunner dockerRunner;

    public DockerJudgeService(
            SubmissionRepository submissionRepository,
            TestCaseRepository testCaseRepository,
            @Lazy SubmissionService submissionService,
            List<LanguageExecutor> languageExecutors,
            JudgeProperties judgeProperties,
            DockerRunner dockerRunner) {
        this.submissionRepository = submissionRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionService = submissionService;
        this.languageExecutors = languageExecutors;
        this.judgeProperties = judgeProperties;
        this.dockerRunner = dockerRunner;
    }

    @Override
    @Async("judgeTaskExecutor")
    public void evaluate(Long submissionId) {
        log.info("DockerJudgeService evaluating submissionId: {}", submissionId);
        try {
            if (!dockerRunner.isDockerAvailable()) {
                log.error("Docker daemon unavailable when evaluating submissionId: {}", submissionId);
                submissionService.markAsQueued(submissionId);
                submissionService.markAsRunning(submissionId);
                JudgeResult errorResult = new JudgeResult(
                        Verdict.INTERNAL_ERROR, 0, 0, 0, 0,
                        "Docker runtime environment is unavailable on server."
                );
                submissionService.updateJudgeResult(submissionId, errorResult);
                return;
            }

            submissionService.markAsQueued(submissionId);
            submissionService.markAsRunning(submissionId);

            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new IllegalArgumentException("Submission not found with id: " + submissionId));

            List<TestCase> testCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(submission.getProblem().getId());

            LanguageExecutor executor = languageExecutors.stream()
                    .filter(e -> e.supports(submission.getLanguage()))
                    .findFirst()
                    .orElse(null);

            if (executor == null) {
                JudgeResult unsuppResult = new JudgeResult(
                        Verdict.INTERNAL_ERROR, 0, 0, 0, testCases.size(),
                        "Unsupported programming language: " + submission.getLanguage()
                );
                submissionService.updateJudgeResult(submissionId, unsuppResult);
                return;
            }

            JudgeResult result = executor.execute(submission, testCases, judgeProperties);
            submissionService.updateJudgeResult(submissionId, result);
            log.info("DockerJudgeService completed evaluation for submissionId: {} with verdict: {}", submissionId, result.verdict());

        } catch (Exception e) {
            log.error("Unexpected error in DockerJudgeService for submissionId: {}", submissionId, e);
            try {
                JudgeResult failResult = new JudgeResult(
                        Verdict.INTERNAL_ERROR, 0, 0, 0, 0,
                        "Internal error during submission evaluation."
                );
                submissionService.updateJudgeResult(submissionId, failResult);
            } catch (Exception ex) {
                log.error("Failed to persist INTERNAL_ERROR verdict for submissionId: {}", submissionId, ex);
            }
        }
    }
}
