package com.apricorn.analytics.service;

import com.apricorn.analytics.dto.LeaderboardEntryResponse;
import com.apricorn.problem.dto.PageResponse;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public LeaderboardServiceImpl(UserRepository userRepository, SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaderboardEntryResponse> getGlobalLeaderboard(int page, int size) {
        int pageNum = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        List<LeaderboardEntryResponse> rankedList = calculateRankedLeaderboard();

        int start = Math.min(pageNum * pageSize, rankedList.size());
        int end = Math.min(start + pageSize, rankedList.size());
        List<LeaderboardEntryResponse> pageContent = rankedList.subList(start, end);

        PageImpl<LeaderboardEntryResponse> pageResult = new PageImpl<>(
                pageContent, PageRequest.of(pageNum, pageSize), rankedList.size()
        );

        return PageResponse.from(pageResult);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardEntryResponse getUserLeaderboardPosition(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        List<LeaderboardEntryResponse> rankedList = calculateRankedLeaderboard();

        return rankedList.stream()
                .filter(entry -> entry.username().equalsIgnoreCase(user.getUsername()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Leaderboard position not found for user: " + user.getUsername()));
    }

    private List<LeaderboardEntryResponse> calculateRankedLeaderboard() {
        List<User> users = userRepository.findAll();
        List<LeaderboardEntryUnranked> unrankedEntries = new ArrayList<>();

        for (User u : users) {
            Long userId = u.getId();
            long totalSubs = submissionRepository.countByUserId(userId);
            long acceptedSubs = submissionRepository.countByUserIdAndVerdict(userId, Verdict.ACCEPTED);
            double acceptanceRate = totalSubs > 0 ? Math.round(((double) acceptedSubs / totalSubs * 100.0) * 10.0) / 10.0 : 0.0;

            long problemsSolved = submissionRepository.countDistinctSolvedProblemsByUserId(userId);
            long easySolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.EASY);
            long mediumSolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.MEDIUM);
            long hardSolved = submissionRepository.countDistinctSolvedProblemsByUserIdAndDifficulty(userId, Difficulty.HARD);

            unrankedEntries.add(new LeaderboardEntryUnranked(
                    u.getUsername(),
                    problemsSolved,
                    easySolved,
                    mediumSolved,
                    hardSolved,
                    acceptanceRate
            ));
        }

        // Deterministic sorting strategy:
        // 1. problemsSolved desc
        // 2. hardSolved desc
        // 3. mediumSolved desc
        // 4. acceptanceRate desc
        // 5. username asc
        unrankedEntries.sort(
                Comparator.comparingLong(LeaderboardEntryUnranked::problemsSolved).reversed()
                        .thenComparing(Comparator.comparingLong(LeaderboardEntryUnranked::hardSolved).reversed())
                        .thenComparing(Comparator.comparingLong(LeaderboardEntryUnranked::mediumSolved).reversed())
                        .thenComparing(Comparator.comparingDouble(LeaderboardEntryUnranked::acceptanceRate).reversed())
                        .thenComparing(LeaderboardEntryUnranked::username)
        );

        List<LeaderboardEntryResponse> rankedList = new ArrayList<>();
        for (int i = 0; i < unrankedEntries.size(); i++) {
            LeaderboardEntryUnranked entry = unrankedEntries.get(i);
            rankedList.add(new LeaderboardEntryResponse(
                    i + 1,
                    entry.username(),
                    entry.problemsSolved(),
                    entry.easySolved(),
                    entry.mediumSolved(),
                    entry.hardSolved(),
                    entry.acceptanceRate()
            ));
        }

        return rankedList;
    }

    private record LeaderboardEntryUnranked(
            String username,
            long problemsSolved,
            long easySolved,
            long mediumSolved,
            long hardSolved,
            double acceptanceRate
    ) {}
}
