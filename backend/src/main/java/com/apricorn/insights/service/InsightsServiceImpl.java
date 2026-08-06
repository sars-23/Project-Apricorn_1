package com.apricorn.insights.service;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.insights.dto.*;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsightsServiceImpl implements InsightsService {
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public InsightsServiceImpl(
            ProblemRepository problemRepository,
            UserRepository userRepository,
            SubmissionRepository submissionRepository) {
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserInsightsResponse getUserInsights(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + userEmail));

        Long userId = user.getId();
        List<Long> attemptedProblemIds = submissionRepository.findDistinctProblemIdsByUserId(userId);

        if (attemptedProblemIds.isEmpty()) {
            InsightSummaryResponse emptySummary = new InsightSummaryResponse(0, 0, 0, null, 0, null);
            return new UserInsightsResponse(
                    emptySummary,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    0, 0, 0
            );
        }

        long problemsAttemptedCount = attemptedProblemIds.size();
        long problemsSolvedCount = 0;
        long firstTrySolvesCount = 0;
        long sumAttemptsToFirstSolve = 0;

        Map<Verdict, Long> failureCounts = new EnumMap<>(Verdict.class);
        long totalUserTerminalFailures = 0;

        List<BlockerResponse> blockers = new ArrayList<>();
        List<PerformanceInsightResponse> performanceImprovements = new ArrayList<>();

        Map<Difficulty, long[]> difficultyStats = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) {
            difficultyStats.put(d, new long[2]); // [0: attempted, 1: solved]
        }

        List<InsightCardResponse> rawCards = new ArrayList<>();
        String repeatedFailureHeadline = null;
        String repeatedFailureEvidence = null;
        String breakthroughHeadline = null;
        String breakthroughEvidence = null;

        // Process per attempted problem
        for (Long problemId : attemptedProblemIds) {
            List<Submission> subs = submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(userId, problemId);
            if (subs.isEmpty()) continue;

            Problem problem = subs.get(0).getProblem();
            Difficulty diff = problem.getDifficulty();
            difficultyStats.get(diff)[0]++; // attempted distinct problem

            // 1. First-try check
            Submission firstSub = subs.get(0);
            boolean isFirstTry = firstSub.getStatus() == SubmissionStatus.COMPLETED && firstSub.getVerdict() == Verdict.ACCEPTED;

            // 2. Find first accepted submission index
            int firstAcceptedIndex = -1;
            for (int i = 0; i < subs.size(); i++) {
                Submission s = subs.get(i);
                if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED) {
                    firstAcceptedIndex = i;
                    break;
                }
            }

            boolean isSolved = (firstAcceptedIndex != -1);
            if (isSolved) {
                problemsSolvedCount++;
                difficultyStats.get(diff)[1]++; // solved distinct problem
                int attemptsToSolve = firstAcceptedIndex + 1;
                sumAttemptsToFirstSolve += attemptsToSolve;

                if (isFirstTry) {
                    firstTrySolvesCount++;
                }

                // Breakthrough / Persistence detection
                if (attemptsToSolve >= 6 && breakthroughHeadline == null) {
                    breakthroughHeadline = problem.getTitle() + " was solved after " + attemptsToSolve + " attempts.";
                    breakthroughEvidence = "First accepted on Attempt #" + attemptsToSolve + ". Demonstrated great persistence.";
                } else if (attemptsToSolve >= 3 && breakthroughHeadline == null) {
                    breakthroughHeadline = problem.getTitle() + " was cracked after " + attemptsToSolve + " attempts.";
                    breakthroughEvidence = "First accepted on Attempt #" + attemptsToSolve + ".";
                }

                // Performance improvement check
                Integer firstAcceptedRuntime = subs.get(firstAcceptedIndex).getExecutionTimeMs();
                Integer minRuntime = null;
                for (Submission s : subs) {
                    if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED && s.getExecutionTimeMs() != null) {
                        if (minRuntime == null || s.getExecutionTimeMs() < minRuntime) {
                            minRuntime = s.getExecutionTimeMs();
                        }
                    }
                }

                if (firstAcceptedRuntime != null && minRuntime != null && firstAcceptedRuntime > minRuntime && firstAcceptedRuntime > 0) {
                    int diffMs = firstAcceptedRuntime - minRuntime;
                    double pct = Math.round(((double) diffMs / firstAcceptedRuntime * 100.0) * 10.0) / 10.0;
                    performanceImprovements.add(new PerformanceInsightResponse(
                            problem.getId(),
                            problem.getTitle(),
                            problem.getSlug(),
                            firstAcceptedRuntime,
                            minRuntime,
                            diffMs,
                            pct
                    ));
                }
            } else {
                // Unsolved Blocker
                Submission latest = subs.get(subs.size() - 1);
                blockers.add(new BlockerResponse(
                        problem.getId(),
                        problem.getTitle(),
                        problem.getSlug(),
                        problem.getDifficulty(),
                        subs.size(),
                        latest.getVerdict(),
                        latest.getSubmittedAt()
                ));
            }

            // Failure verdict tracking (EXCLUDING INTERNAL_ERROR)
            Map<Verdict, Long> problemFailures = new EnumMap<>(Verdict.class);
            int checkLimit = isSolved ? firstAcceptedIndex : subs.size();
            for (int i = 0; i < checkLimit; i++) {
                Submission s = subs.get(i);
                if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() != null && s.getVerdict() != Verdict.ACCEPTED) {
                    if (s.getVerdict() != Verdict.INTERNAL_ERROR) { // Exclude system failures
                        failureCounts.put(s.getVerdict(), failureCounts.getOrDefault(s.getVerdict(), 0L) + 1);
                        totalUserTerminalFailures++;

                        problemFailures.put(s.getVerdict(), problemFailures.getOrDefault(s.getVerdict(), 0L) + 1);
                    }
                }
            }

            // Repeated failure pattern check for this problem
            if (repeatedFailureHeadline == null) {
                for (Map.Entry<Verdict, Long> entry : problemFailures.entrySet()) {
                    if (entry.getValue() >= 2) {
                        String vName = formatVerdict(entry.getKey());
                        if (isSolved) {
                            repeatedFailureHeadline = problem.getTitle() + " required " + entry.getValue() + " " + vName + " attempts before acceptance.";
                            repeatedFailureEvidence = "Multiple " + vName + " verdicts occurred prior to first accepted solution.";
                        } else {
                            repeatedFailureHeadline = problem.getTitle() + " has " + entry.getValue() + " " + vName + " attempts and remains unsolved.";
                            repeatedFailureEvidence = "Targeted debugging on " + vName + " scenarios recommended.";
                        }
                        break;
                    }
                }
            }
        }

        // Summary Calculations
        long currentUnsolvedCount = problemsAttemptedCount - problemsSolvedCount;
        Double avgAttemptsToSolve = problemsSolvedCount > 0 ? Math.round(((double) sumAttemptsToFirstSolve / problemsSolvedCount) * 10.0) / 10.0 : null;

        long totalJudgedSubmissions = submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.COMPLETED);
        long acceptedJudgedSubmissions = submissionRepository.countByUserIdAndStatusAndVerdict(userId, SubmissionStatus.COMPLETED, Verdict.ACCEPTED);
        Double acceptedSubmissionRate = totalJudgedSubmissions > 0 ? Math.round(((double) acceptedJudgedSubmissions / totalJudgedSubmissions * 100.0) * 10.0) / 10.0 : null;

        InsightSummaryResponse summary = new InsightSummaryResponse(
                problemsAttemptedCount,
                problemsSolvedCount,
                firstTrySolvesCount,
                avgAttemptsToSolve,
                currentUnsolvedCount,
                acceptedSubmissionRate
        );

        // Sort Blockers by latest attempt DESC
        blockers.sort(Comparator.comparing(BlockerResponse::latestAttemptAt, Comparator.nullsLast(Comparator.reverseOrder())));
        List<BlockerResponse> topBlockers = blockers.stream().limit(5).toList();

        // Sort Performance Improvements by improvementMs DESC
        performanceImprovements.sort(Comparator.comparingInt(PerformanceInsightResponse::improvementMs).reversed());

        // Failure Pattern Distribution
        List<FailurePatternResponse> failurePatterns = new ArrayList<>();
        Verdict mostFrequentFailureVerdict = null;
        long maxFailureCount = 0;

        for (Verdict v : Verdict.values()) {
            if (v == Verdict.ACCEPTED || v == Verdict.INTERNAL_ERROR) continue;
            long count = failureCounts.getOrDefault(v, 0L);
            if (count > 0) {
                double pct = totalUserTerminalFailures > 0 ? Math.round(((double) count / totalUserTerminalFailures * 100.0) * 10.0) / 10.0 : 0.0;
                failurePatterns.add(new FailurePatternResponse(v, formatVerdict(v), count, pct));
                if (count > maxFailureCount) {
                    maxFailureCount = count;
                    mostFrequentFailureVerdict = v;
                }
            }
        }

        // Difficulty Breakdown
        List<DifficultyInsightResponse> difficultyBreakdown = new ArrayList<>();
        Difficulty strongestDifficulty = null;
        double maxSuccessPct = -1.0;

        for (Difficulty d : Difficulty.values()) {
            long[] counts = difficultyStats.get(d);
            long attempted = counts[0];
            long solved = counts[1];
            double pct = attempted > 0 ? Math.round(((double) solved / attempted * 100.0) * 10.0) / 10.0 : 0.0;
            difficultyBreakdown.add(new DifficultyInsightResponse(d, formatEnumName(d.name()), attempted, solved, pct));

            // Evidence threshold: requires at least 2 solved problems to claim strongest difficulty
            if (solved >= 2 && pct > maxSuccessPct) {
                maxSuccessPct = pct;
                strongestDifficulty = d;
            }
        }

        // 7-day momentum
        Instant start7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Object[]> rows7Days = submissionRepository.findSubmissionsByUserIdSince(userId, start7Days);
        long recentSubmissions7Days = rows7Days.size();
        long recentAccepted7Days = rows7Days.stream().filter(r -> r[1] == Verdict.ACCEPTED).count();
        long recentSolved7Days = 0; // calculated dynamically if needed

        // Assemble Prioritized Insight Cards (capped at 6)
        List<InsightCardResponse> cards = new ArrayList<>();

        // 1. Current Blockers
        if (!topBlockers.isEmpty()) {
            BlockerResponse topB = topBlockers.get(0);
            cards.add(new InsightCardResponse(
                    "CURRENT FOCUS",
                    "Active Blocker: " + topB.title(),
                    topB.attemptCount() + " attempt(s) recorded. Latest verdict: " + (topB.latestVerdict() != null ? formatVerdict(topB.latestVerdict()) : "PENDING") + ".",
                    "Solve Again",
                    "/problems/" + topB.slug(),
                    "warning"
            ));
        }

        // 2. Repeated Failure Patterns
        if (repeatedFailureHeadline != null) {
            cards.add(new InsightCardResponse(
                    "DEBUGGING PATTERN",
                    repeatedFailureHeadline,
                    repeatedFailureEvidence,
                    "Review Problems",
                    "/problems?status=ATTEMPTED",
                    "danger"
            ));
        } else if (mostFrequentFailureVerdict != null && maxFailureCount >= 2) {
            cards.add(new InsightCardResponse(
                    "DEBUGGING PATTERN",
                    formatVerdict(mostFrequentFailureVerdict) + " is your most frequent failed verdict.",
                    maxFailureCount + " of " + totalUserTerminalFailures + " user-caused failed submissions.",
                    "Review Submissions",
                    "/submissions",
                    "danger"
            ));
        }

        // 3. Breakthrough / Persistence
        if (breakthroughHeadline != null) {
            cards.add(new InsightCardResponse(
                    "BREAKTHROUGH",
                    breakthroughHeadline,
                    breakthroughEvidence,
                    "View Journey",
                    "/problems",
                    "success"
            ));
        }

        // 4. Performance Improvement
        if (!performanceImprovements.isEmpty()) {
            PerformanceInsightResponse topP = performanceImprovements.get(0);
            cards.add(new InsightCardResponse(
                    "PERFORMANCE",
                    "Best runtime improvement on " + topP.title(),
                    "Improved from " + topP.firstAcceptedRuntimeMs() + " ms to " + topP.bestRuntimeMs() + " ms (" + topP.improvementPercent() + "% faster).",
                    "View Journey",
                    "/problems/" + topP.slug() + "/journey",
                    "info"
            ));
        }

        // 5. First Try Success
        if (firstTrySolvesCount > 0) {
            cards.add(new InsightCardResponse(
                    "FIRST TRY SUCCESS",
                    firstTrySolvesCount + " problem(s) solved on the first attempt.",
                    "Demonstrates strong initial solution design and correctness.",
                    "View Solved",
                    "/problems?status=SOLVED",
                    "success"
            ));
        }

        // 6. Difficulty Strength (if threshold met)
        if (strongestDifficulty != null) {
            long[] sCounts = difficultyStats.get(strongestDifficulty);
            cards.add(new InsightCardResponse(
                    "DIFFICULTY STRENGTH",
                    "Strongest current difficulty is " + formatEnumName(strongestDifficulty.name()),
                    sCounts[1] + " of " + sCounts[0] + " attempted problems solved (" + Math.round(((double) sCounts[1] / sCounts[0] * 100.0) * 10.0) / 10.0 + "% success rate).",
                    "Browse Problems",
                    "/problems?difficulty=" + strongestDifficulty.name(),
                    "primary"
            ));
        }

        // 7. Recent Momentum
        if (recentSubmissions7Days > 0 && cards.size() < 6) {
            cards.add(new InsightCardResponse(
                    "RECENT MOMENTUM",
                    recentSubmissions7Days + " submission(s) in the last 7 days.",
                    recentAccepted7Days + " accepted solution(s) recorded recently.",
                    "View Submissions",
                    "/submissions",
                    "info"
            ));
        }

        return new UserInsightsResponse(
                summary,
                cards,
                failurePatterns,
                difficultyBreakdown,
                topBlockers,
                performanceImprovements,
                recentSubmissions7Days,
                recentAccepted7Days,
                recentSolved7Days
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getProblemJourneyInsights(String userEmail, String problemSlug) {
        Problem problem = problemRepository.findBySlug(problemSlug)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + problemSlug));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + userEmail));

        List<Submission> subs = submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(user.getId(), problem.getId());
        if (subs.isEmpty()) return List.of();

        List<String> insights = new ArrayList<>();

        int firstAcceptedIndex = -1;
        for (int i = 0; i < subs.size(); i++) {
            Submission s = subs.get(i);
            if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED) {
                firstAcceptedIndex = i;
                break;
            }
        }

        boolean isSolved = (firstAcceptedIndex != -1);
        if (isSolved) {
            int attemptsToSolve = firstAcceptedIndex + 1;
            if (attemptsToSolve == 1) {
                insights.add("First-try solve! Solved on your very first submission.");
            } else {
                insights.add("Solved after " + attemptsToSolve + " attempts.");
            }

            // Count repeated failures prior to acceptance
            Map<Verdict, Integer> failCounts = new EnumMap<>(Verdict.class);
            for (int i = 0; i < firstAcceptedIndex; i++) {
                Submission s = subs.get(i);
                if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() != null && s.getVerdict() != Verdict.ACCEPTED && s.getVerdict() != Verdict.INTERNAL_ERROR) {
                    failCounts.put(s.getVerdict(), failCounts.getOrDefault(s.getVerdict(), 0) + 1);
                }
            }

            for (Map.Entry<Verdict, Integer> entry : failCounts.entrySet()) {
                if (entry.getValue() >= 2) {
                    insights.add(entry.getValue() + " " + formatVerdict(entry.getKey()) + " attempt(s) occurred before acceptance.");
                    break;
                }
            }

            // Runtime improvement check
            Integer firstRuntime = subs.get(firstAcceptedIndex).getExecutionTimeMs();
            Integer bestRuntime = null;
            for (Submission s : subs) {
                if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED && s.getExecutionTimeMs() != null) {
                    if (bestRuntime == null || s.getExecutionTimeMs() < bestRuntime) {
                        bestRuntime = s.getExecutionTimeMs();
                    }
                }
            }

            if (firstRuntime != null && bestRuntime != null && firstRuntime > bestRuntime && firstRuntime > 0) {
                int diff = firstRuntime - bestRuntime;
                double pct = Math.round(((double) diff / firstRuntime * 100.0) * 10.0) / 10.0;
                insights.add("Runtime improved by " + pct + "% (" + diff + " ms faster) after your first accepted solution.");
            }
        } else {
            insights.add(subs.size() + " attempt(s) recorded so far. Unsolved.");
        }

        return insights.stream().limit(3).toList();
    }

    private String formatVerdict(Verdict verdict) {
        if (verdict == null) return "Pending";
        String[] parts = verdict.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private String formatEnumName(String name) {
        if (name == null || name.isEmpty()) return "";
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
