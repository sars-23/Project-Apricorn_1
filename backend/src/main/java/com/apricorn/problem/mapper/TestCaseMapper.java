package com.apricorn.problem.mapper;

import com.apricorn.problem.dto.AdminTestCaseResponse;
import com.apricorn.problem.entity.TestCase;
import org.springframework.stereotype.Component;

@Component
public class TestCaseMapper {
    public AdminTestCaseResponse toAdminResponse(TestCase testCase) {
        if (testCase == null) return null;
        Long problemId = testCase.getProblem() != null ? testCase.getProblem().getId() : null;
        return new AdminTestCaseResponse(
                testCase.getId(),
                problemId,
                testCase.getInput(),
                testCase.getExpectedOutput(),
                testCase.isSample(),
                testCase.getOrderIndex(),
                testCase.getCreatedAt()
        );
    }
}
