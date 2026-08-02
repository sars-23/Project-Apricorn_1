package com.apricorn.judge.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.apricorn.submission.model.JudgeResult;
import com.apricorn.submission.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class StubJudgeServiceTest {
    private SubmissionService submissionService;
    private JudgeService judgeService;

    @BeforeEach
    void setUp() {
        submissionService = mock(SubmissionService.class);
        judgeService = new StubJudgeService(submissionService);
    }

    @Test
    void evaluatesSubmissionThroughLifecycle() {
        judgeService.evaluate(100L);

        InOrder inOrder = inOrder(submissionService);
        inOrder.verify(submissionService).markAsQueued(100L);
        inOrder.verify(submissionService).markAsRunning(100L);
        inOrder.verify(submissionService).updateJudgeResult(eq(100L), any(JudgeResult.class));
    }
}
