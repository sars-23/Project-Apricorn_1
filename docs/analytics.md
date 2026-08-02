# User Dashboard, Analytics & Global Leaderboard

## Overview

The analytics and leaderboard module for Project Apricorn calculates real-time coding performance, UTC daily streaks, distinct problem solve statistics, and global competitive rankings using database-side PostgreSQL aggregations.

## Architectural Flow

```text
Submission Activity
       │
       ▼
PostgreSQL Aggregation Engine (COUNT DISTINCT, GROUP BY, CASE WHEN)
       │
       ├─────────────────┬──────────────────┬─────────────────┐
       ▼                 ▼                  ▼                 ▼
User Dashboard    Global Leaderboard    Problem Stats    Admin Analytics
 (/users/me/db)    (/leaderboard)       (/problems/*/s)  (/admin/analytics)
```

## Analytics Formulas & Definitions

- **Solved Problem Definition**: A problem is counted as solved when the user has at least one `ACCEPTED` submission for that problem. Multiple `ACCEPTED` submissions for the same problem count as **one** solved problem.
- **Difficulty Counts (`easySolved`, `mediumSolved`, `hardSolved`)**: Number of DISTINCT problems of difficulty `EASY`, `MEDIUM`, or `HARD` solved by the user.
- **Acceptance Rate Formula**:
  $$\text{Acceptance Rate} = \left(\frac{\text{Accepted Submission Attempts}}{\text{Total Submission Attempts}}\right) \times 100$$
  Rounded to 1 decimal place. If `totalSubmissions == 0`, `acceptanceRate = 0.0`.

## UTC Coding Streak Algorithm

Calculated by `StreakCalculator`:
1. Converts submission timestamps (`submittedAt`) to distinct `LocalDate` values in the UTC timezone (`ZoneOffset.UTC`).
2. **Longest Streak**: Computes the maximum length of consecutive UTC days in the user's submission history.
3. **Current Streak**: Anchors at today UTC (or yesterday UTC if no submission today yet). Increments backwards as long as consecutive calendar dates contain submissions. Returns 0 if neither today nor yesterday has a submission.

## Global Leaderboard Ranking Policy

Users are ranked deterministically by:
1. `problemsSolved` (descending)
2. `hardSolved` (descending)
3. `mediumSolved` (descending)
4. `acceptanceRate` (descending)
5. `username` (ascending)

## Database Query Strategy & Performance

- **No N+1 Queries**: Statistics are retrieved via database aggregation queries (`countDistinctSolvedProblemsByUserIdAndDifficulty`, `countDistinctAttemptedUsersByProblemId`, etc.).
- **Privacy Controls**: Leaderboard endpoints expose only public usernames, ranks, and aggregate coding statistics. Passwords, hashes, emails, JWT tokens, and hidden test inputs/outputs are never serialized.
