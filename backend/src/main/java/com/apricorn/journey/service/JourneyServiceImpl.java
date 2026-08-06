package com.apricorn.journey.service;

import com.apricorn.exception.ProblemNotFoundException;
import com.apricorn.journey.dto.*;
import com.apricorn.problem.entity.ProblemStatus;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JourneyServiceImpl implements JourneyService {
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public JourneyServiceImpl(
            ProblemRepository problemRepository,
            UserRepository userRepository,
            SubmissionRepository submissionRepository) {
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SolutionJourneyResponse getSolutionJourney(String userEmail, String problemSlug) {
        Problem problem = problemRepository.findBySlug(problemSlug)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + problemSlug));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + userEmail));

        List<Submission> submissions = submissionRepository.findByUserIdAndProblemIdOrderBySubmittedAtAsc(user.getId(), problem.getId());

        if (submissions.isEmpty()) {
            ProblemStatus userStatus = ProblemStatus.UNSOLVED;
            JourneyProblemResponse problemResponse = new JourneyProblemResponse(
                    problem.getId(),
                    problem.getTitle(),
                    problem.getSlug(),
                    problem.getDifficulty(),
                    userStatus
            );
            JourneySummaryResponse emptySummary = new JourneySummaryResponse(
                    0L, null, null, null, 0L, null, null, null, null, null, null
            );
            return new SolutionJourneyResponse(
                    problemResponse,
                    userStatus,
                    emptySummary,
                    List.of(),
                    "No attempts recorded yet for this problem."
            );
        }

        long totalAttempts = submissions.size();
        Instant firstAttemptAt = submissions.get(0).getSubmittedAt();
        Instant latestAttemptAt = submissions.get(submissions.size() - 1).getSubmittedAt();

        int firstAcceptedIndex = -1;
        for (int i = 0; i < submissions.size(); i++) {
            Submission s = submissions.get(i);
            if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED) {
                firstAcceptedIndex = i;
                break;
            }
        }

        long acceptedSubmissionsCount = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED)
                .count();

        ProblemStatus userStatus = (acceptedSubmissionsCount > 0) ? ProblemStatus.SOLVED : ProblemStatus.ATTEMPTED;

        Integer firstAcceptedAttemptNumber = (firstAcceptedIndex != -1) ? (firstAcceptedIndex + 1) : null;
        Integer attemptsUntilFirstAccepted = firstAcceptedAttemptNumber;

        // Best Runtime (min executionTimeMs among COMPLETED ACCEPTED)
        Integer bestRuntimeMs = null;
        int bestRuntimeIndex = -1;
        for (int i = 0; i < submissions.size(); i++) {
            Submission s = submissions.get(i);
            if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED && s.getExecutionTimeMs() != null) {
                if (bestRuntimeMs == null || s.getExecutionTimeMs() < bestRuntimeMs) {
                    bestRuntimeMs = s.getExecutionTimeMs();
                    bestRuntimeIndex = i;
                }
            }
        }

        // Best Memory (min memoryUsedKb among COMPLETED ACCEPTED)
        Integer bestMemoryKb = null;
        int bestMemoryIndex = -1;
        for (int i = 0; i < submissions.size(); i++) {
            Submission s = submissions.get(i);
            if (s.getStatus() == SubmissionStatus.COMPLETED && s.getVerdict() == Verdict.ACCEPTED && s.getMemoryUsedKb() != null) {
                if (bestMemoryKb == null || s.getMemoryUsedKb() < bestMemoryKb) {
                    bestMemoryKb = s.getMemoryUsedKb();
                    bestMemoryIndex = i;
                }
            }
        }

        Integer firstAcceptedRuntimeMs = (firstAcceptedIndex != -1) ? submissions.get(firstAcceptedIndex).getExecutionTimeMs() : null;
        Integer runtimeImprovementMs = null;
        Double runtimeImprovementPercent = null;

        if (firstAcceptedRuntimeMs != null && bestRuntimeMs != null && firstAcceptedRuntimeMs > 0) {
            int diff = firstAcceptedRuntimeMs - bestRuntimeMs;
            if (diff > 0) {
                runtimeImprovementMs = diff;
                runtimeImprovementPercent = Math.round(((double) diff / firstAcceptedRuntimeMs * 100.0) * 10.0) / 10.0;
            } else {
                runtimeImprovementMs = 0;
                runtimeImprovementPercent = 0.0;
            }
        }

        List<JourneyAttemptResponse> attemptResponses = new ArrayList<>();
        for (int i = 0; i < submissions.size(); i++) {
            Submission s = submissions.get(i);
            int attemptNumber = i + 1;
            boolean isFirstAccepted = (i == firstAcceptedIndex);
            boolean isBestRuntime = (i == bestRuntimeIndex);
            boolean isBestMemory = (i == bestMemoryIndex);

            attemptResponses.add(new JourneyAttemptResponse(
                    attemptNumber,
                    s.getId(),
                    s.getLanguage(),
                    s.getStatus(),
                    s.getVerdict(),
                    s.getExecutionTimeMs(),
                    s.getMemoryUsedKb(),
                    s.getPassedTestCases(),
                    s.getTotalTestCases(),
                    s.getSubmittedAt(),
                    s.getSourceCode(),
                    s.getErrorMessage(),
                    isFirstAccepted,
                    isBestRuntime,
                    isBestMemory
            ));
        }

        JourneySummaryResponse summary = new JourneySummaryResponse(
                totalAttempts,
                firstAttemptAt,
                firstAcceptedAttemptNumber,
                attemptsUntilFirstAccepted,
                acceptedSubmissionsCount,
                bestRuntimeMs,
                bestMemoryKb,
                latestAttemptAt,
                firstAcceptedRuntimeMs,
                runtimeImprovementMs,
                runtimeImprovementPercent
        );

        JourneyProblemResponse problemResponse = new JourneyProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                userStatus
        );

        String journeyStory = generateJourneyStory(
                userStatus,
                attemptsUntilFirstAccepted,
                acceptedSubmissionsCount,
                firstAcceptedRuntimeMs,
                bestRuntimeMs,
                runtimeImprovementMs,
                runtimeImprovementPercent,
                totalAttempts,
                submissions.get(submissions.size() - 1).getVerdict()
        );

        return new SolutionJourneyResponse(
                problemResponse,
                userStatus,
                summary,
                attemptResponses,
                journeyStory
        );
    }

    private String generateJourneyStory(
            ProblemStatus userStatus,
            Integer attemptsUntilFirstAccepted,
            long acceptedSubmissionsCount,
            Integer firstAcceptedRuntimeMs,
            Integer bestRuntimeMs,
            Integer runtimeImprovementMs,
            Double runtimeImprovementPercent,
            long totalAttempts,
            Verdict latestVerdict) {

        if (userStatus == ProblemStatus.SOLVED) {
            if (attemptsUntilFirstAccepted != null && attemptsUntilFirstAccepted == 1) {
                if (acceptedSubmissionsCount == 1) {
                    return "Solved on your first attempt.";
                } else {
                    String story = "Solved on your first attempt. You submitted " + (acceptedSubmissionsCount - 1) + " more accepted solution(s) afterward.";
                    if (runtimeImprovementMs != null && runtimeImprovementMs > 0) {
                        story += " Best runtime improved by " + runtimeImprovementMs + " ms (" + runtimeImprovementPercent + "%).";
                    }
                    return story;
                }
            } else {
                String story = "Accepted after " + attemptsUntilFirstAccepted + " attempts.";
                if (acceptedSubmissionsCount > 1) {
                    story += " You submitted " + (acceptedSubmissionsCount - 1) + " more accepted solution(s) afterward.";
                }
                if (runtimeImprovementMs != null && runtimeImprovementMs > 0) {
                    story += " Best runtime improved from " + firstAcceptedRuntimeMs + " ms to " + bestRuntimeMs + " ms (" + runtimeImprovementPercent + "%).";
                }
                return story;
            }
        } else {
            String verdictStr = latestVerdict != null ? formatVerdict(latestVerdict) : "PENDING";
            return totalAttempts + " attempt(s) so far. Your latest attempt resulted in " + verdictStr + ".";
        }
    }

    private String formatVerdict(Verdict verdict) {
        if (verdict == null) return "PENDING";
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
}
