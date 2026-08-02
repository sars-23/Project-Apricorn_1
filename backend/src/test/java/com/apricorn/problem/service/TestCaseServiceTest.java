package com.apricorn.problem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.exception.TestCaseNotFoundException;
import com.apricorn.problem.dto.AdminTestCaseResponse;
import com.apricorn.problem.dto.CreateTestCaseRequest;
import com.apricorn.problem.dto.UpdateTestCaseRequest;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.problem.mapper.TestCaseMapper;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.problem.repository.TestCaseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCaseServiceTest {
    private TestCaseRepository testCaseRepository;
    private ProblemRepository problemRepository;
    private TestCaseService testCaseService;

    @BeforeEach
    void setUp() {
        testCaseRepository = mock(TestCaseRepository.class);
        problemRepository = mock(ProblemRepository.class);
        testCaseService = new TestCaseServiceImpl(testCaseRepository, problemRepository, new TestCaseMapper());
    }

    @Test
    void addsTestCaseToProblem() {
        Problem problem = new Problem();
        problem.setId(1L);

        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(inv -> {
            TestCase tc = inv.getArgument(0);
            tc.setId(10L);
            return tc;
        });

        AdminTestCaseResponse response = testCaseService.addTestCase(1L, new CreateTestCaseRequest("1 2", "3", true, 1));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.problemId()).isEqualTo(1L);
        assertThat(response.input()).isEqualTo("1 2");
        assertThat(response.expectedOutput()).isEqualTo("3");
        assertThat(response.isSample()).isTrue();
    }

    @Test
    void throwsNotFoundOnAddingTestCaseToNonExistentProblem() {
        when(problemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testCaseService.addTestCase(99L, new CreateTestCaseRequest("1", "2", false, 0)))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void updatesTestCase() {
        TestCase testCase = new TestCase();
        testCase.setId(10L);
        testCase.setInput("old input");
        testCase.setExpectedOutput("old output");

        when(testCaseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminTestCaseResponse response = testCaseService.updateTestCase(10L, new UpdateTestCaseRequest("new input", "new output", false, 2));

        assertThat(response.input()).isEqualTo("new input");
        assertThat(response.expectedOutput()).isEqualTo("new output");
    }

    @Test
    void deletesTestCase() {
        when(testCaseRepository.existsById(10L)).thenReturn(true);

        testCaseService.deleteTestCase(10L);

        verify(testCaseRepository).deleteById(10L);
    }
}
