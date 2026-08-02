package com.apricorn.analytics.service;

import com.apricorn.analytics.dto.PlatformAnalyticsResponse;
import com.apricorn.analytics.dto.ProblemStatsResponse;
import com.apricorn.analytics.dto.UserDashboardResponse;

public interface AnalyticsService {
    UserDashboardResponse getUserDashboard(String userEmail);
    ProblemStatsResponse getProblemStats(String problemSlug);
    PlatformAnalyticsResponse getAdminPlatformAnalytics();
}
