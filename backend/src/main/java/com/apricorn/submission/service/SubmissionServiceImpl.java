package com.apricorn.submission.service;

import com.apricorn.exception.InvalidSubmissionException;
import com.apricorn.exception.InvalidSubmissionStateException;
import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.exception.SubmissionNotFoundException;
import com.apricorn.exception.UnauthorizedSubmissionAccessException;
import com.apricorn.judge.service.JudgeService;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.dto.CreateSubmissionRequest;
import com.apricorn.submission.dto.SubmissionCreatedResponse;
import com.apricorn.submission.dto.SubmissionDetailResponse;
import com.apricorn.submission.dto.SubmissionSummaryResponse;
import com.apricorn.submission.dto.UserSubmissionStatsResponse;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.mapper.SubmissionMapper;
import com.apricorn.submission.model.JudgeResult;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.submission.repository.SubmissionSpecifications;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionServiceImpl implements SubmissionService {
    public static final int MAX_SOURCE_CODE_BYTES = 65536; // 64 KB limit
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("submittedAt", "completedAt", "id", "executionTimeMs", "memoryUsedKb");

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubmissionMapper submissionMapper;
    private final JudgeService judgeService;

    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            SubmissionMapper submissionMapper,
            JudgeService judgeService) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.submissionMapper = submissionMapper;
        this.judgeService = judgeService;
    }

    @Override
    @Transactional
    public SubmissionCreatedResponse createSubmission(String problemSlug, CreateSubmissionRequest request, String userEmail) {
        if (request.sourceCode() == null || request.sourceCode().isBlank()) {
            throw new InvalidSubmissionException("Source code cannot be empty.");
        }
        if (request.sourceCode().length() > MAX_SOURCE_CODE_BYTES) {
            throw new InvalidSubmissionException("Source code exceeds maximum allowed size of 64 KB.");
        }
        if (request.language() == null) {
            throw new InvalidSubmissionException("Programming language must be specified.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        Problem problem = problemRepository.findBySlug(problemSlug)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + problemSlug));

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(request.language());
        submission.setSourceCode(request.sourceCode());
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(Instant.now());

        Submission saved = submissionRepository.save(submission);

        // Dispatch to judge service abstraction
        judgeService.evaluate(saved.getId());

        return new SubmissionCreatedResponse(saved.getId(), saved.getStatus(), saved.getSubmittedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionById(Long id, String currentUserEmail, boolean isAdmin) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException("Submission not found with id: " + id));

        if (!isAdmin && !submission.getUser().getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new UnauthorizedSubmissionAccessException("You are not authorized to view this submission.");
        }

        return submissionMapper.toDetailResponse(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> getUserSubmissions(
            String userEmail, String problemSlug, Language language, SubmissionStatus status, Verdict verdict, int page, int size, String sort) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Sort parseSort = parseSortParam(sort);
        Pageable pageable = PageRequest.of(pageNum, pageSize, parseSort);

        Specification<Submission> spec = Specification.where(SubmissionSpecifications.hasUserId(user.getId()))
                .and(SubmissionSpecifications.hasProblemSlug(problemSlug))
                .and(SubmissionSpecifications.hasLanguage(language))
                .and(SubmissionSpecifications.hasStatus(status))
                .and(SubmissionSpecifications.hasVerdict(verdict));

        Page<Submission> pageResult = submissionRepository.findAll(spec, pageable);
        Page<SubmissionSummaryResponse> summaryPage = pageResult.map(submissionMapper::toSummaryResponse);

        return PageResponse.from(summaryPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> getUserSubmissionsForProblem(
            String userEmail, String problemSlug, int page, int size, String sort) {
        return getUserSubmissions(userEmail, problemSlug, null, null, null, page, size, sort);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSubmissionStatsResponse getUserSubmissionStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        Long userId = user.getId();
        long totalSubmissions = submissionRepository.countByUserId(userId);
        long acceptedSubmissions = submissionRepository.countByUserIdAndVerdict(userId, Verdict.ACCEPTED);
        double acceptanceRate = totalSubmissions > 0 ? ((double) acceptedSubmissions / totalSubmissions) * 100.0 : 0.0;
        long problemsAttempted = submissionRepository.countDistinctAttemptedProblemsByUserId(userId);
        long problemsSolved = submissionRepository.countDistinctSolvedProblemsByUserId(userId);

        return new UserSubmissionStatsResponse(
                totalSubmissions,
                acceptedSubmissions,
                Math.round(acceptanceRate * 100.0) / 100.0,
                problemsAttempted,
                problemsSolved
        );
    }

    @Override
    @Transactional
    public void markAsQueued(Long submissionId) {
        Submission submission = getSubmissionOrThrow(submissionId);
        if (submission.getStatus() == SubmissionStatus.COMPLETED) {
            throw new InvalidSubmissionStateException("Cannot queue submission in terminal status: " + submission.getStatus());
        }
        submission.setStatus(SubmissionStatus.QUEUED);
        submissionRepository.save(submission);
    }

    @Override
    @Transactional
    public void markAsRunning(Long submissionId) {
        Submission submission = getSubmissionOrThrow(submissionId);
        if (submission.getStatus() == SubmissionStatus.COMPLETED) {
            throw new InvalidSubmissionStateException("Cannot run submission in terminal status: " + submission.getStatus());
        }
        submission.setStatus(SubmissionStatus.RUNNING);
        submission.setStartedAt(Instant.now());
        submissionRepository.save(submission);
    }

    @Override
    @Transactional
    public void updateJudgeResult(Long submissionId, JudgeResult result) {
        Submission submission = getSubmissionOrThrow(submissionId);
        if (submission.getStatus() == SubmissionStatus.COMPLETED) {
            throw new InvalidSubmissionStateException("Submission already in terminal state.");
        }
        submission.setStatus(SubmissionStatus.COMPLETED);
        submission.setVerdict(result.verdict());
        submission.setExecutionTimeMs(result.executionTimeMs());
        submission.setMemoryUsedKb(result.memoryUsedKb());
        submission.setPassedTestCases(result.passedTestCases());
        submission.setTotalTestCases(result.totalTestCases());
        submission.setErrorMessage(result.errorMessage());
        submission.setCompletedAt(Instant.now());

        submissionRepository.save(submission);
    }

    private Submission getSubmissionOrThrow(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Submission not found with id: " + submissionId));
    }

    private Sort parseSortParam(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "submittedAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            property = "submittedAt";
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(direction, property);
    }
}
