package com.apricorn.judge.service;

import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.model.JudgeResult;
import com.apricorn.submission.service.SubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Temporary deterministic stub judge implementation for Sprint 3.
 * DO NOT execute arbitrary user code on host machine.
 */
@Service
public class StubJudgeService implements JudgeService {
    private static final Logger log = LoggerFactory.getLogger(StubJudgeService.class);
    private final SubmissionService submissionService;

    public StubJudgeService(@Lazy SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Override
    public void evaluate(Long submissionId) {
        log.info("StubJudgeService evaluating submissionId: {}", submissionId);
        try {
            submissionService.markAsQueued(submissionId);
            submissionService.markAsRunning(submissionId);

            JudgeResult result = new JudgeResult(
                    Verdict.ACCEPTED,
                    45,
                    15400,
                    10,
                    10,
                    null
            );

            submissionService.updateJudgeResult(submissionId, result);
            log.info("StubJudgeService evaluation complete for submissionId: {}", submissionId);
        } catch (Exception e) {
            log.error("StubJudgeService error processing submissionId: {}", submissionId, e);
        }
    }
}
