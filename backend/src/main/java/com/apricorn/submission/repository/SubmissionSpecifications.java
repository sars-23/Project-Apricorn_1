package com.apricorn.submission.repository;

import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import org.springframework.data.jpa.domain.Specification;

public final class SubmissionSpecifications {
    private SubmissionSpecifications() {}

    public static Specification<Submission> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) return null;
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Submission> hasProblemSlug(String problemSlug) {
        return (root, query, criteriaBuilder) -> {
            if (problemSlug == null || problemSlug.isBlank()) return null;
            return criteriaBuilder.equal(root.get("problem").get("slug"), problemSlug.toLowerCase().trim());
        };
    }

    public static Specification<Submission> hasLanguage(Language language) {
        return (root, query, criteriaBuilder) -> {
            if (language == null) return null;
            return criteriaBuilder.equal(root.get("language"), language);
        };
    }

    public static Specification<Submission> hasStatus(SubmissionStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) return null;
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Submission> hasVerdict(Verdict verdict) {
        return (root, query, criteriaBuilder) -> {
            if (verdict == null) return null;
            return criteriaBuilder.equal(root.get("verdict"), verdict);
        };
    }
}
