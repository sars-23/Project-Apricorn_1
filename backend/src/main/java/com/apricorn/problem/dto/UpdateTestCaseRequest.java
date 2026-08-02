package com.apricorn.problem.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTestCaseRequest(
        @NotBlank String input,
        @NotBlank String expectedOutput,
        Boolean isSample,
        Integer orderIndex
) {}
