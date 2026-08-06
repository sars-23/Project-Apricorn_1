package com.apricorn.journey.service;

import com.apricorn.journey.dto.SolutionJourneyResponse;

public interface JourneyService {
    SolutionJourneyResponse getSolutionJourney(String userEmail, String problemSlug);
}
