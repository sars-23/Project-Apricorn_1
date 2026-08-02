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
import java.util.HashSet;
import java.util.List;
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
public class ProblemServiceImpl implements ProblemService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "createdAt", "difficulty", "updatedAt", "id");

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ProblemMapper problemMapper;

    public ProblemServiceImpl(ProblemRepository problemRepository, TagRepository tagRepository, UserRepository userRepository, ProblemMapper problemMapper) {
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
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
    public PageResponse<ProblemSummaryResponse> getProblems(Difficulty difficulty, String tagSlug, String search, int page, int size, String sort) {
        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Sort parseSort = parseSortParam(sort);
        Pageable pageable = PageRequest.of(pageNum, pageSize, parseSort);

        Specification<Problem> spec = Specification.where(ProblemSpecifications.hasDifficulty(difficulty))
                .and(ProblemSpecifications.hasTagSlug(tagSlug))
                .and(ProblemSpecifications.searchByTitle(search));

        Page<Problem> pageResult = problemRepository.findAll(spec, pageable);
        Page<ProblemSummaryResponse> summaryPage = pageResult.map(problemMapper::toSummaryResponse);

        return PageResponse.from(summaryPage);
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
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
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
