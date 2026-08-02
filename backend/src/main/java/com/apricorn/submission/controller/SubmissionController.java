package com.apricorn.submission.controller;

import com.apricorn.problem.dto.PageResponse;
import com.apricorn.submission.dto.CreateSubmissionRequest;
import com.apricorn.submission.dto.SubmissionCreatedResponse;
import com.apricorn.submission.dto.SubmissionDetailResponse;
import com.apricorn.submission.dto.SubmissionSummaryResponse;
import com.apricorn.submission.dto.UserSubmissionStatsResponse;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Submissions", description = "Code submission & judge lifecycle APIs")
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/api/v1/problems/{problemSlug}/submissions")
    @Operation(summary = "Submit code", description = "Submits source code for a problem. Returns 202 Accepted with initial submission status.")
    public ResponseEntity<SubmissionCreatedResponse> submitCode(
            @PathVariable String problemSlug,
            @Valid @RequestBody CreateSubmissionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SubmissionCreatedResponse response = submissionService.createSubmission(problemSlug, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/api/v1/submissions/{id}")
    @Operation(summary = "Get submission detail", description = "Retrieves submission detail. Exposes source code only to the owner or ADMIN.")
    public SubmissionDetailResponse getSubmissionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return submissionService.getSubmissionById(id, userDetails.getUsername(), isAdmin);
    }

    @GetMapping("/api/v1/submissions")
    @Operation(summary = "Get my submissions", description = "Retrieves paginated submission history for the authenticated user with filtering.")
    public PageResponse<SubmissionSummaryResponse> getMySubmissions(
            @RequestParam(name = "problem", required = false) String problemSlug,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) Verdict verdict,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails) {
        return submissionService.getUserSubmissions(
                userDetails.getUsername(), problemSlug, language, status, verdict, page, size, sort);
    }

    @GetMapping("/api/v1/problems/{slug}/submissions/me")
    @Operation(summary = "Get my submissions for problem", description = "Retrieves the authenticated user's submission history for a specific problem.")
    public PageResponse<SubmissionSummaryResponse> getMySubmissionsForProblem(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails) {
        return submissionService.getUserSubmissionsForProblem(userDetails.getUsername(), slug, page, size, sort);
    }

    @GetMapping("/api/v1/users/me/submission-stats")
    @Operation(summary = "Get my submission stats", description = "Calculates total submissions, accepted count, acceptance rate, problems attempted, and problems solved for authenticated user.")
    public UserSubmissionStatsResponse getMySubmissionStats(@AuthenticationPrincipal UserDetails userDetails) {
        return submissionService.getUserSubmissionStats(userDetails.getUsername());
    }
}
