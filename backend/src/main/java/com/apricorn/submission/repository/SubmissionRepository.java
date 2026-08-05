package com.apricorn.submission.repository;

import com.apricorn.problem.entity.Difficulty;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import java.time.Instant;
import java.util.List;
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

    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.id = :userId AND s.verdict = com.apricorn.submission.entity.Verdict.ACCEPTED AND s.problem.difficulty = :difficulty")
    long countDistinctSolvedProblemsByUserIdAndDifficulty(@Param("userId") Long userId, @Param("difficulty") Difficulty difficulty);

    @Query("SELECT DISTINCT s.submittedAt FROM Submission s WHERE s.user.id = :userId ORDER BY s.submittedAt ASC")
    List<Instant> findSubmissionTimestampsByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"problem"})
    List<Submission> findTop10ByUserIdOrderBySubmittedAtDesc(Long userId);

    long countByProblemId(Long problemId);

    long countByProblemIdAndVerdict(Long problemId, Verdict verdict);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.problem.id = :problemId")
    long countDistinctAttemptedUsersByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.problem.id = :problemId AND s.verdict = com.apricorn.submission.entity.Verdict.ACCEPTED")
    long countDistinctSolvedUsersByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.submittedAt >= :startOfDay")
    long countActiveUsersToday(@Param("startOfDay") Instant startOfDay);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.submittedAt >= :startOfDay")
    long countSubmissionsToday(@Param("startOfDay") Instant startOfDay);

    long countByVerdict(Verdict verdict);

    long countByLanguage(com.apricorn.submission.entity.Language language);

    @Query("SELECT s.problem.id, COUNT(CASE WHEN s.verdict = com.apricorn.submission.entity.Verdict.ACCEPTED THEN 1 END), COUNT(s.id) " +
           "FROM Submission s WHERE s.problem.id IN :problemIds AND s.status = com.apricorn.submission.entity.SubmissionStatus.COMPLETED " +
           "GROUP BY s.problem.id")
    List<Object[]> findAcceptanceStatsForProblemIds(@Param("problemIds") List<Long> problemIds);

    @Query("SELECT s.problem.id, s.verdict FROM Submission s " +
           "WHERE s.user.id = :userId AND s.problem.id IN :problemIds AND s.status = com.apricorn.submission.entity.SubmissionStatus.COMPLETED")
    List<Object[]> findUserVerdictsForProblemIds(@Param("userId") Long userId, @Param("problemIds") List<Long> problemIds);

    @Query("SELECT DISTINCT s.problem.id FROM Submission s WHERE s.user.id = :userId AND s.verdict = :verdict AND s.status = com.apricorn.submission.entity.SubmissionStatus.COMPLETED")
    List<Long> findDistinctProblemIdsByUserIdAndVerdict(@Param("userId") Long userId, @Param("verdict") Verdict verdict);

    @Query("SELECT DISTINCT s.problem.id FROM Submission s WHERE s.user.id = :userId AND s.status = com.apricorn.submission.entity.SubmissionStatus.COMPLETED")
    List<Long> findDistinctProblemIdsByUserId(@Param("userId") Long userId);
}
