package com.apricorn.problem.controller;

import com.apricorn.problem.dto.CreateProblemRequest;
import com.apricorn.problem.dto.ProblemDetailResponse;
import com.apricorn.problem.dto.UpdateProblemRequest;
import com.apricorn.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/problems")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Problems", description = "Admin problem management APIs")
public class AdminProblemController {
    private final ProblemService problemService;

    public AdminProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create problem", description = "Creates a new coding problem.")
    public ProblemDetailResponse createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return problemService.createProblem(request, user.getUsername());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update problem", description = "Updates an existing coding problem.")
    public ProblemDetailResponse updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProblemRequest request) {
        return problemService.updateProblem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete problem", description = "Deletes a coding problem by ID.")
    public void deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
    }
}
