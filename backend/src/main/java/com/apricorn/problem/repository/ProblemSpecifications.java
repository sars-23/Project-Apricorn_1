package com.apricorn.problem.repository;

import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.Tag;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class ProblemSpecifications {
    private ProblemSpecifications() {}

    public static Specification<Problem> hasDifficulty(Difficulty difficulty) {
        return (root, query, criteriaBuilder) -> {
            if (difficulty == null) return null;
            return criteriaBuilder.equal(root.get("difficulty"), difficulty);
        };
    }

    public static Specification<Problem> hasTagSlug(String tagSlug) {
        return (root, query, criteriaBuilder) -> {
            if (tagSlug == null || tagSlug.isBlank()) return null;
            query.distinct(true);
            Join<Problem, Tag> tags = root.join("tags");
            return criteriaBuilder.equal(tags.get("slug"), tagSlug.toLowerCase().trim());
        };
    }

    public static Specification<Problem> searchByTitle(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) return null;
            String pattern = "%" + search.toLowerCase().trim() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
        };
    }
}
