package com.apricorn.submission.mapper;

import com.apricorn.submission.dto.ProblemReferenceResponse;
import com.apricorn.submission.dto.SubmissionDetailResponse;
import com.apricorn.submission.dto.SubmissionSummaryResponse;
import com.apricorn.submission.entity.Submission;
import org.springframework.stereotype.Component;

@Component
public class SubmissionMapper {
    public ProblemReferenceResponse toProblemReference(Submission submission) {
        if (submission == null || submission.getProblem() == null) return null;
        return new ProblemReferenceResponse(
                submission.getProblem().getId(),
                submission.getProblem().getTitle(),
                submission.getProblem().getSlug()
        );
    }

    public SubmissionSummaryResponse toSummaryResponse(Submission submission) {
        if (submission == null) return null;
        return new SubmissionSummaryResponse(
                submission.getId(),
                toProblemReference(submission),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedKb(),
                submission.getPassedTestCases(),
                submission.getTotalTestCases(),
                submission.getSubmittedAt(),
                submission.getCompletedAt()
        );
    }

    public SubmissionDetailResponse toDetailResponse(Submission submission) {
        if (submission == null) return null;
        return new SubmissionDetailResponse(
                submission.getId(),
                toProblemReference(submission),
                submission.getLanguage(),
                submission.getSourceCode(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedKb(),
                submission.getPassedTestCases(),
                submission.getTotalTestCases(),
                submission.getErrorMessage(),
                submission.getSubmittedAt(),
                submission.getCompletedAt()
        );
    }
}
