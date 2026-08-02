package com.apricorn.problem.service;

import com.apricorn.problem.dto.CreateProblemRequest;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.dto.ProblemSummaryResponse;
import com.apricorn.problem.dto.UpdateProblemRequest;
import com.apricorn.problem.entity.Difficulty;

public interface ProblemService {
    ProblemDetailResponse createProblem(CreateProblemRequest request, String createdByEmail);
    ProblemDetailResponse updateProblem(Long id, UpdateProblemRequest request);
    void deleteProblem(Long id);
    ProblemDetailResponse getProblemBySlug(String slug);
    PageResponse<ProblemSummaryResponse> getProblems(Difficulty difficulty, String tagSlug, String search, int page, int size, String sort);
}
