package com.apricorn.problem.repository;

import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.Tag;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class ProblemSpecifications {
    private ProblemSpecifications() {}

    public static Specification<Problem> isVisible() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("isVisible"));
    }

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

    public static Specification<Problem> hasUserStatus(com.apricorn.problem.entity.ProblemStatus status, 
                                                      java.util.List<Long> solvedProblemIds, 
                                                      java.util.List<Long> attemptedOnlyProblemIds, 
                                                      java.util.List<Long> allUserProblemIds) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) return null;
            switch (status) {
                case SOLVED:
                    if (solvedProblemIds == null || solvedProblemIds.isEmpty()) {
                        return criteriaBuilder.disjunction();
                    }
                    return root.get("id").in(solvedProblemIds);
                case ATTEMPTED:
                    if (attemptedOnlyProblemIds == null || attemptedOnlyProblemIds.isEmpty()) {
                        return criteriaBuilder.disjunction();
                    }
                    return root.get("id").in(attemptedOnlyProblemIds);
                case UNSOLVED:
                    if (allUserProblemIds == null || allUserProblemIds.isEmpty()) {
                        return null;
                    }
                    return criteriaBuilder.not(root.get("id").in(allUserProblemIds));
                default:
                    return null;
            }
        };
    }
}
