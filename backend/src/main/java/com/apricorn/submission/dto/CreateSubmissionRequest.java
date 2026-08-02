package com.apricorn.submission.dto;

import com.apricorn.submission.entity.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubmissionRequest(
        @NotNull Language language,
        @NotBlank @Size(min = 1, max = 65536) String sourceCode
) {}
