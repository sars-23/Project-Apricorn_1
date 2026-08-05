package com.apricorn.problem.service;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.problem.dto.CreateProblemRequest;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.dto.ProblemSummaryResponse;
import com.apricorn.problem.dto.UpdateProblemRequest;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.Tag;
import com.apricorn.problem.mapper.ProblemMapper;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.problem.repository.ProblemSpecifications;
import com.apricorn.problem.repository.TagRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import com.apricorn.util.SlugUtils;
import com.apricorn.problem.dto.ProblemProgressSummaryResponse;
import com.apricorn.problem.entity.ProblemStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemServiceImpl implements ProblemService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "createdAt", "difficulty", "updatedAt", "id");

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ProblemMapper problemMapper;

    public ProblemServiceImpl(ProblemRepository problemRepository, TagRepository tagRepository, UserRepository userRepository, SubmissionRepository submissionRepository, ProblemMapper problemMapper) {
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.problemMapper = problemMapper;
    }

    @Override
    @Transactional
    public ProblemDetailResponse createProblem(CreateProblemRequest request, String createdByEmail) {
        User creator = userRepository.findByEmail(createdByEmail)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + createdByEmail));

        String baseSlug = SlugUtils.toSlug(request.title());
        String slug = generateUniqueSlug(baseSlug, null);

        Problem problem = new Problem();
        problem.setTitle(request.title().trim());
        problem.setSlug(slug);
        problem.setDescription(request.description());
        problem.setDifficulty(request.difficulty());
        problem.setInputFormat(request.inputFormat());
        problem.setOutputFormat(request.outputFormat());
        problem.setConstraints(request.constraints());
        problem.setSampleInput(request.sampleInput());
        problem.setSampleOutput(request.sampleOutput());
        problem.setExplanation(request.explanation());
        problem.setTimeLimitMs(request.timeLimitMs() != null ? request.timeLimitMs() : 2000);
        problem.setMemoryLimitMb(request.memoryLimitMb() != null ? request.memoryLimitMb() : 256);
        problem.setCreatedBy(creator);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            problem.setTags(new HashSet<>(tags));
        }

        Problem saved = problemRepository.save(problem);
        return problemMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ProblemDetailResponse updateProblem(Long id, UpdateProblemRequest request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id));

        if (!problem.getTitle().equalsIgnoreCase(request.title().trim())) {
            String baseSlug = SlugUtils.toSlug(request.title());
            problem.setSlug(generateUniqueSlug(baseSlug, id));
        }

        problem.setTitle(request.title().trim());
        problem.setDescription(request.description());
        problem.setDifficulty(request.difficulty());
        problem.setInputFormat(request.inputFormat());
        problem.setOutputFormat(request.outputFormat());
        problem.setConstraints(request.constraints());
        problem.setSampleInput(request.sampleInput());
        problem.setSampleOutput(request.sampleOutput());
        problem.setExplanation(request.explanation());
        if (request.timeLimitMs() != null) problem.setTimeLimitMs(request.timeLimitMs());
        if (request.memoryLimitMb() != null) problem.setMemoryLimitMb(request.memoryLimitMb());

        if (request.tagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            problem.setTags(new HashSet<>(tags));
        }

        Problem updated = problemRepository.save(problem);
        return problemMapper.toDetailResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProblem(Long id) {
        if (!problemRepository.existsById(id)) {
            throw new ProblemNotFoundException("Problem not found with id: " + id);
        }
        problemRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblemBySlug(String slug) {
        Problem problem = problemRepository.findDetailBySlug(slug)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + slug));
        return problemMapper.toDetailResponse(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> getProblems(Difficulty difficulty, String tagSlug, String search, ProblemStatus status, String currentUserEmail, int page, int size, String sort) {
        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Optional<User> currentUser = (currentUserEmail != null && !currentUserEmail.isBlank())
                ? userRepository.findByEmail(currentUserEmail)
                : Optional.empty();

        Long userId = currentUser.map(User::getId).orElse(null);

        List<Long> solvedProblemIds = null;
        List<Long> attemptedOnlyProblemIds = null;
        List<Long> allUserProblemIds = null;

        if (userId != null) {
            solvedProblemIds = submissionRepository.findDistinctProblemIdsByUserIdAndVerdict(userId, Verdict.ACCEPTED);
            allUserProblemIds = submissionRepository.findDistinctProblemIdsByUserId(userId);
            
            Set<Long> solvedSet = new HashSet<>(solvedProblemIds);
            attemptedOnlyProblemIds = allUserProblemIds.stream()
                    .filter(id -> !solvedSet.contains(id))
                    .toList();
        }

        Sort parseSort = parseSortParam(sort);
        Pageable pageable = PageRequest.of(pageNum, pageSize, parseSort);

        Specification<Problem> spec = Specification.where(ProblemSpecifications.hasDifficulty(difficulty))
                .and(ProblemSpecifications.hasTagSlug(tagSlug))
                .and(ProblemSpecifications.searchByTitle(search))
                .and(ProblemSpecifications.hasUserStatus(status, solvedProblemIds, attemptedOnlyProblemIds, allUserProblemIds));

        Page<Problem> pageResult = problemRepository.findAll(spec, pageable);

        if (pageResult.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        List<Long> pageProblemIds = pageResult.getContent().stream().map(Problem::getId).toList();

        // Batch Query 1: Acceptance rates for problems on this page
        Map<Long, Double> acceptanceRateMap = new HashMap<>();
        List<Object[]> statsList = submissionRepository.findAcceptanceStatsForProblemIds(pageProblemIds);
        for (Object[] row : statsList) {
            Long pId = (Long) row[0];
            long acceptedCount = ((Number) row[1]).longValue();
            long totalCount = ((Number) row[2]).longValue();
            if (totalCount > 0) {
                double rate = Math.round(((double) acceptedCount / totalCount * 100.0) * 10.0) / 10.0;
                acceptanceRateMap.put(pId, rate);
            }
        }

        // Batch Query 2: User problem statuses for problems on this page
        Map<Long, ProblemStatus> userStatusMap = new HashMap<>();
        if (userId != null) {
            List<Object[]> userVerdicts = submissionRepository.findUserVerdictsForProblemIds(userId, pageProblemIds);
            for (Object[] row : userVerdicts) {
                Long pId = (Long) row[0];
                Verdict verdict = (Verdict) row[1];
                if (verdict == Verdict.ACCEPTED) {
                    userStatusMap.put(pId, ProblemStatus.SOLVED);
                } else if (!userStatusMap.containsKey(pId) || userStatusMap.get(pId) != ProblemStatus.SOLVED) {
                    userStatusMap.put(pId, ProblemStatus.ATTEMPTED);
                }
            }
        }

        Page<ProblemSummaryResponse> summaryPage = pageResult.map(p -> 
                problemMapper.toSummaryResponse(
                        p, 
                        acceptanceRateMap.get(p.getId()), 
                        userStatusMap.getOrDefault(p.getId(), ProblemStatus.UNSOLVED)
                )
        );

        // Optional sort by acceptance rate in-memory if requested
        if ("acceptance".equalsIgnoreCase(sort) || "acceptance,desc".equalsIgnoreCase(sort)) {
            List<ProblemSummaryResponse> content = summaryPage.getContent().stream()
                    .sorted((a, b) -> Double.compare(
                            b.acceptanceRate() != null ? b.acceptanceRate() : -1.0,
                            a.acceptanceRate() != null ? a.acceptanceRate() : -1.0))
                    .toList();
            summaryPage = new PageImpl<>(content, pageable, summaryPage.getTotalElements());
        } else if ("acceptance,asc".equalsIgnoreCase(sort)) {
            List<ProblemSummaryResponse> content = summaryPage.getContent().stream()
                    .sorted((a, b) -> Double.compare(
                            a.acceptanceRate() != null ? a.acceptanceRate() : 999.0,
                            b.acceptanceRate() != null ? b.acceptanceRate() : 999.0))
                    .toList();
            summaryPage = new PageImpl<>(content, pageable, summaryPage.getTotalElements());
        }

        return PageResponse.from(summaryPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemProgressSummaryResponse getProgressSummary(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return null;
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return null;
        }

        Long userId = user.getId();
        long solvedTotal = submissionRepository.countDistinctSolvedProblemsByUserId(userId);
        long totalProblems = problemRepository.count();

        long solvedEasy = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.EASY);
        long totalEasy = problemRepository.countByDifficulty(Difficulty.EASY);

        long solvedMedium = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.MEDIUM);
        long totalMedium = problemRepository.countByDifficulty(Difficulty.MEDIUM);

        long solvedHard = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.HARD);
        long totalHard = problemRepository.countByDifficulty(Difficulty.HARD);

        return new ProblemProgressSummaryResponse(
                solvedTotal, totalProblems,
                solvedEasy, totalEasy,
                solvedMedium, totalMedium,
                solvedHard, totalHard
        );
    }

    private String generateUniqueSlug(String baseSlug, Long currentId) {
        String slug = baseSlug;
        int count = 1;
        while (true) {
            var existing = problemRepository.findBySlug(slug);
            if (existing.isEmpty() || (currentId != null && existing.get().getId().equals(currentId))) {
                return slug;
            }
            slug = baseSlug + "-" + count++;
        }
    }

    private Sort parseSortParam(String sort) {
        if (sort == null || sort.isBlank() || sort.startsWith("acceptance")) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            property = "createdAt";
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(direction, property);
    }
}
