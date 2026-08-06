package com.apricorn.journey.dto;

import com.apricorn.problem.entity.ProblemStatus;
import com.apricorn.problem.entity.Difficulty;

public record JourneyProblemResponse(
        Long id,
        String title,
        String slug,
        Difficulty difficulty,
        ProblemStatus userStatus
) {}
