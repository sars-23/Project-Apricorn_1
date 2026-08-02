package com.apricorn.problem.service;

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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseServiceImpl implements TestCaseService {
    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseMapper testCaseMapper;

    public TestCaseServiceImpl(TestCaseRepository testCaseRepository, ProblemRepository problemRepository, TestCaseMapper testCaseMapper) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    @Transactional
    public AdminTestCaseResponse addTestCase(Long problemId, CreateTestCaseRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + problemId));

        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(request.input());
        testCase.setExpectedOutput(request.expectedOutput());
        testCase.setSample(Boolean.TRUE.equals(request.isSample()));
        testCase.setOrderIndex(request.orderIndex() != null ? request.orderIndex() : 0);

        TestCase saved = testCaseRepository.save(testCase);
        return testCaseMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminTestCaseResponse updateTestCase(Long testCaseId, UpdateTestCaseRequest request) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new TestCaseNotFoundException("TestCase not found with id: " + testCaseId));

        testCase.setInput(request.input());
        testCase.setExpectedOutput(request.expectedOutput());
        if (request.isSample() != null) {
            testCase.setSample(request.isSample());
        }
        if (request.orderIndex() != null) {
            testCase.setOrderIndex(request.orderIndex());
        }

        TestCase updated = testCaseRepository.save(testCase);
        return testCaseMapper.toAdminResponse(updated);
    }

    @Override
    @Transactional
    public void deleteTestCase(Long testCaseId) {
        if (!testCaseRepository.existsById(testCaseId)) {
            throw new TestCaseNotFoundException("TestCase not found with id: " + testCaseId);
        }
        testCaseRepository.deleteById(testCaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTestCaseResponse> getTestCasesForProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException("Problem not found with id: " + problemId);
        }
        return testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId).stream()
                .map(testCaseMapper::toAdminResponse)
                .toList();
    }
}
