package com.apricorn.submission.repository;

import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface SubmissionRepository extends JpaRepository<Submission, Long>, JpaSpecificationExecutor<Submission> {
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"problem", "user"})
    Page<Submission> findAll(Specification<Submission> spec, @NonNull Pageable pageable);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"problem", "user"})
    Optional<Submission> findById(@NonNull Long id);

    long countByUserId(Long userId);

    long countByUserIdAndVerdict(Long userId, Verdict verdict);

    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.id = :userId")
    long countDistinctAttemptedProblemsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.id = :userId AND s.verdict = com.apricorn.submission.entity.Verdict.ACCEPTED")
    long countDistinctSolvedProblemsByUserId(@Param("userId") Long userId);
}
