package com.apricorn.problem.service;

import com.apricorn.problem.dto.AdminTestCaseResponse;
import com.apricorn.problem.dto.CreateTestCaseRequest;
import com.apricorn.problem.dto.UpdateTestCaseRequest;
import java.util.List;

public interface TestCaseService {
    AdminTestCaseResponse addTestCase(Long problemId, CreateTestCaseRequest request);
    AdminTestCaseResponse updateTestCase(Long testCaseId, UpdateTestCaseRequest request);
    void deleteTestCase(Long testCaseId);
    List<AdminTestCaseResponse> getTestCasesForProblem(Long problemId);
}
