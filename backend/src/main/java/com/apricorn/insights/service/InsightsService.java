package com.apricorn.insights.service;

import com.apricorn.insights.dto.UserInsightsResponse;
import java.util.List;

public interface InsightsService {
    UserInsightsResponse getUserInsights(String userEmail);
    List<String> getProblemJourneyInsights(String userEmail, String problemSlug);
}
