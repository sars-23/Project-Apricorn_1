package com.apricorn.problem.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTestCaseRequest(
        @NotBlank String input,
        @NotBlank String expectedOutput,
        Boolean isSample,
        Integer orderIndex
) {}
