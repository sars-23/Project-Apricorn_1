package com.apricorn.problem.mapper;

import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.dto.ProblemSummaryResponse;
import com.apricorn.problem.dto.TagResponse;
import com.apricorn.problem.entity.Problem;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProblemMapper {
    private final TagMapper tagMapper;

    public ProblemMapper(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public ProblemSummaryResponse toSummaryResponse(Problem problem) {
        if (problem == null) return null;
        List<TagResponse> tags = problem.getTags() != null
                ? problem.getTags().stream()
                .map(tagMapper::toResponse)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList()
                : List.of();

        return new ProblemSummaryResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                tags,
                problem.getCreatedAt()
        );
    }

    public ProblemDetailResponse toDetailResponse(Problem problem) {
        if (problem == null) return null;
        List<TagResponse> tags = problem.getTags() != null
                ? problem.getTags().stream()
                .map(tagMapper::toResponse)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList()
                : List.of();

        String createdBy = problem.getCreatedBy() != null ? problem.getCreatedBy().getUsername() : null;

        return new ProblemDetailResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDescription(),
                problem.getDifficulty(),
                problem.getInputFormat(),
                problem.getOutputFormat(),
                problem.getConstraints(),
                problem.getSampleInput(),
                problem.getSampleOutput(),
                problem.getExplanation(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                tags,
                problem.getCreatedAt(),
                problem.getUpdatedAt(),
                createdBy
        );
    }
}
