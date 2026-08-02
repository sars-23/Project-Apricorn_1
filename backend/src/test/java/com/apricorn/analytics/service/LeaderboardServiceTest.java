package com.apricorn.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.analytics.dto.LeaderboardEntryResponse;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaderboardServiceTest {
    private UserRepository userRepository;
    private SubmissionRepository submissionRepository;
    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        leaderboardService = new LeaderboardServiceImpl(userRepository, submissionRepository);
    }

    @Test
    void ranksUsersByDeterministicTieBreaking() {
        User u1 = new User(); u1.setId(1L); u1.setUsername("alice"); u1.setEmail("alice@example.com");
        User u2 = new User(); u2.setId(2L); u2.setUsername("bob"); u2.setEmail("bob@example.com");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        // Alice: 5 solved (2 hard)
        when(submissionRepository.countByUserId(1L)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndVerdict(1L, Verdict.ACCEPTED)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserId(1L)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(1L, Difficulty.HARD)).thenReturn(2L);

        // Bob: 5 solved (1 hard)
        when(submissionRepository.countByUserId(2L)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndVerdict(2L, Verdict.ACCEPTED)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserId(2L)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(2L, Difficulty.HARD)).thenReturn(1L);

        PageResponse<LeaderboardEntryResponse> leaderboard = leaderboardService.getGlobalLeaderboard(0, 20);

        assertThat(leaderboard.content()).hasSize(2);
        assertThat(leaderboard.content().get(0).username()).isEqualTo("alice");
        assertThat(leaderboard.content().get(0).rank()).isEqualTo(1);
        assertThat(leaderboard.content().get(1).username()).isEqualTo("bob");
        assertThat(leaderboard.content().get(1).rank()).isEqualTo(2);
    }

    @Test
    void retrievesUserLeaderboardPosition() {
        User u1 = new User(); u1.setId(1L); u1.setUsername("alice"); u1.setEmail("alice@example.com");
        when(userRepository.findAll()).thenReturn(List.of(u1));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(u1));
        when(submissionRepository.countByUserId(1L)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndVerdict(1L, Verdict.ACCEPTED)).thenReturn(5L);
        when(submissionRepository.countDistinctSolvedProblemsByUserId(1L)).thenReturn(5L);

        LeaderboardEntryResponse entry = leaderboardService.getUserLeaderboardPosition("alice@example.com");

        assertThat(entry.rank()).isEqualTo(1);
        assertThat(entry.username()).isEqualTo("alice");
    }
}
