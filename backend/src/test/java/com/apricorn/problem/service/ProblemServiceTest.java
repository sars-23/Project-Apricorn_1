package com.apricorn.problem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.problem.dto.CreateProblemRequest;
import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.Tag;
import com.apricorn.problem.mapper.ProblemMapper;
import com.apricorn.problem.mapper.TagMapper;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.problem.repository.TagRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProblemServiceTest {
    private ProblemRepository problemRepository;
    private TagRepository tagRepository;
    private UserRepository userRepository;
    private ProblemService problemService;

    @BeforeEach
    void setUp() {
        problemRepository = mock(ProblemRepository.class);
        tagRepository = mock(TagRepository.class);
        userRepository = mock(UserRepository.class);
        problemService = new ProblemServiceImpl(
                problemRepository,
                tagRepository,
                userRepository,
                new ProblemMapper(new TagMapper())
        );
    }

    @Test
    void createsProblemWithUniqueSlugAndTags() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setUsername("admin");

        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Array");
        tag.setSlug("array");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        CreateProblemRequest request = new CreateProblemRequest(
                "Two Sum",
                "Description",
                Difficulty.EASY,
                "Input format",
                "Output format",
                "Constraints",
                "Sample Input",
                "Sample Output",
                "Explanation",
                2000,
                256,
                Set.of(1L)
        );

        ProblemDetailResponse response = problemService.createProblem(request, "admin@example.com");

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Two Sum");
        assertThat(response.slug()).isEqualTo("two-sum");
        assertThat(response.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(response.tags()).extracting("name").containsExactly("Array");
    }

    @Test
    void handlesSlugCollisionOnCreation() {
        User admin = new User();
        admin.setEmail("admin@example.com");

        Problem existing = new Problem();
        existing.setId(1L);
        existing.setSlug("two-sum");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(existing));
        when(problemRepository.findBySlug("two-sum-1")).thenReturn(Optional.empty());
        when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });

        CreateProblemRequest request = new CreateProblemRequest(
                "Two Sum",
                "Desc",
                Difficulty.MEDIUM,
                null, null, null, null, null, null, 2000, 256, null
        );

        ProblemDetailResponse response = problemService.createProblem(request, "admin@example.com");

        assertThat(response.slug()).isEqualTo("two-sum-1");
    }

    @Test
    void retrievesProblemBySlug() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("Two Sum");
        problem.setSlug("two-sum");
        problem.setDescription("Find two numbers.");
        problem.setDifficulty(Difficulty.EASY);

        when(problemRepository.findDetailBySlug("two-sum")).thenReturn(Optional.of(problem));

        ProblemDetailResponse response = problemService.getProblemBySlug("two-sum");

        assertThat(response.title()).isEqualTo("Two Sum");
        assertThat(response.slug()).isEqualTo("two-sum");
    }

    @Test
    void throwsNotFoundOnNonExistentSlug() {
        when(problemRepository.findDetailBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getProblemBySlug("unknown"))
                .isInstanceOf(ProblemNotFoundException.class);
    }
}
