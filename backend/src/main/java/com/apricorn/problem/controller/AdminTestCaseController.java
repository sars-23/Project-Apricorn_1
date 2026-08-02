package com.apricorn.problem.controller;

import com.apricorn.problem.dto.AdminTestCaseResponse;
import com.apricorn.problem.dto.CreateTestCaseRequest;
import com.apricorn.problem.dto.UpdateTestCaseRequest;
import com.apricorn.problem.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Test Cases", description = "Admin test case management APIs")
public class AdminTestCaseController {
    private final TestCaseService testCaseService;

    public AdminTestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping("/problems/{problemId}/test-cases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add test case", description = "Adds a test case to a problem.")
    public AdminTestCaseResponse addTestCase(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateTestCaseRequest request) {
        return testCaseService.addTestCase(problemId, request);
    }

    @PutMapping("/test-cases/{testCaseId}")
    @Operation(summary = "Update test case", description = "Updates an existing test case.")
    public AdminTestCaseResponse updateTestCase(
            @PathVariable Long testCaseId,
            @Valid @RequestBody UpdateTestCaseRequest request) {
        return testCaseService.updateTestCase(testCaseId, request);
    }

    @DeleteMapping("/test-cases/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete test case", description = "Deletes a test case.")
    public void deleteTestCase(@PathVariable Long testCaseId) {
        testCaseService.deleteTestCase(testCaseId);
    }

    @GetMapping("/problems/{problemId}/test-cases")
    @Operation(summary = "Get problem test cases", description = "Returns all test cases (including hidden judge test cases) for a problem.")
    public List<AdminTestCaseResponse> getTestCasesForProblem(@PathVariable Long problemId) {
        return testCaseService.getTestCasesForProblem(problemId);
    }
}
