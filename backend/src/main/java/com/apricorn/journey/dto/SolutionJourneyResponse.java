package com.apricorn.journey.dto;

import com.apricorn.problem.entity.ProblemStatus;
import java.util.List;

public record SolutionJourneyResponse(
        JourneyProblemResponse problem,
        ProblemStatus userStatus,
        JourneySummaryResponse summary,
        List<JourneyAttemptResponse> attempts,
        String journeyStory,
        List<String> journeyInsights
) {
    public SolutionJourneyResponse(
            JourneyProblemResponse problem,
            ProblemStatus userStatus,
            JourneySummaryResponse summary,
            List<JourneyAttemptResponse> attempts,
            String journeyStory
    ) {
        this(problem, userStatus, summary, attempts, journeyStory, List.of());
    }
}
