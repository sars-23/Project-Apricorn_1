package com.apricorn.problem.repository;

import com.apricorn.problem.entity.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    Optional<Tag> findBySlug(String slug);
}
