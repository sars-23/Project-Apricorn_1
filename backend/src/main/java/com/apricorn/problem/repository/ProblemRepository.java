package com.apricorn.problem.repository;

import com.apricorn.problem.entity.Problem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {
    Optional<Problem> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByTitle(String title);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"tags"})
    Page<Problem> findAll(Specification<Problem> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"tags", "createdBy"})
    Optional<Problem> findDetailBySlug(String slug);
}
