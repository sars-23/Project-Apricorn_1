package com.apricorn.analytics.service;

import com.apricorn.analytics.dto.LeaderboardEntryResponse;
import com.apricorn.problem.dto.PageResponse;

public interface LeaderboardService {
    PageResponse<LeaderboardEntryResponse> getGlobalLeaderboard(int page, int size);
    LeaderboardEntryResponse getUserLeaderboardPosition(String userEmail);
}
