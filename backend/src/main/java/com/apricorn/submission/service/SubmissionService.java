package com.apricorn.submission.service;

import com.apricorn.problem.dto.PageResponse;
import com.apricorn.submission.dto.CreateSubmissionRequest;
import com.apricorn.submission.dto.SubmissionCreatedResponse;
import com.apricorn.submission.dto.SubmissionDetailResponse;
import com.apricorn.submission.dto.SubmissionSummaryResponse;
import com.apricorn.submission.dto.UserSubmissionStatsResponse;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.model.JudgeResult;

public interface SubmissionService {
    SubmissionCreatedResponse createSubmission(String problemSlug, CreateSubmissionRequest request, String userEmail);
    SubmissionDetailResponse getSubmissionById(Long id, String currentUserEmail, boolean isAdmin);
    PageResponse<SubmissionSummaryResponse> getUserSubmissions(String userEmail, String problemSlug, Language language, SubmissionStatus status, Verdict verdict, int page, int size, String sort);
    PageResponse<SubmissionSummaryResponse> getUserSubmissionsForProblem(String userEmail, String problemSlug, int page, int size, String sort);
    UserSubmissionStatsResponse getUserSubmissionStats(String userEmail);

    void markAsQueued(Long submissionId);
    void markAsRunning(Long submissionId);
    void updateJudgeResult(Long submissionId, JudgeResult result);
}
