# Code Submission System & Judge Engine Foundation

## Overview

The code submission system handles the complete lifecycle of coding problem submissions for Project Apricorn. It decouples the HTTP submission flow and state management from the code evaluation engine using a clean `JudgeService` abstraction, preparing the project for Sprint 4's isolated Docker sandbox engine while safely preventing host-OS code execution during Sprint 3.

## Submission Lifecycle

```text
User submits code
        │
        ▼
   [ PENDING ]
        │
        ▼
   [ QUEUED ]
        │
        ▼
   [ RUNNING ]
        │
        ▼
  [ COMPLETED ] ───► Terminal Verdict
```

### Terminal Verdicts

- `ACCEPTED`: Solution passed all sample and hidden test cases.
- `WRONG_ANSWER`: Solution produced output that did not match expected output.
- `COMPILATION_ERROR`: Code failed to compile.
- `RUNTIME_ERROR`: Solution threw an unhandled exception or non-zero exit status during execution.
- `TIME_LIMIT_EXCEEDED`: Execution exceeded problem time limit.
- `MEMORY_LIMIT_EXCEEDED`: Execution exceeded problem memory limit.
- `INTERNAL_ERROR`: Unexpected internal judge engine failure.

## Architecture

```text
                 HTTP Request
                      │
                      ▼
            SubmissionController
                      │
                      ▼
            SubmissionService
                 │          │
                 │          ▼
                 │     JudgeService Interface
                 │          │
                 ▼          ▼
           PostgreSQL   StubJudgeService (Sprint 3)
                            │
                            │ (Sprint 4)
                            ▼
                      DockerJudgeEngine
```

## Security & Ownership Controls

- **Source Code Privacy**: Submitted source code is visible only to the submission owner or users with `ROLE_ADMIN`. Endpoint `GET /api/v1/submissions/{id}` enforces strict ownership checks and returns HTTP `403 Forbidden` if an unauthorized user attempts to inspect another user's submission.
- **Principal Authenticity**: Client requests cannot supply a `userId`. All submissions derive user identity strictly from the verified Spring Security Context (`@AuthenticationPrincipal`).
- **Hidden Test Case Isolation**: No submission API response ever exposes hidden test-case inputs or expected outputs. Verdict outcomes (e.g. `WRONG_ANSWER`) are returned without revealing confidential test inputs.

## Database Schema & Indexes

- `submissions`: Stores submission records with foreign keys to `users(id)` and `problems(id)`.
- **Indexes**:
  - `idx_submissions_user_id` on `submissions(user_id)`
  - `idx_submissions_problem_id` on `submissions(problem_id)`
  - `idx_submissions_status` on `submissions(status)`
  - `idx_submissions_verdict` on `submissions(verdict)`
  - `idx_submissions_submitted_at` on `submissions(submitted_at)`
  - `idx_submissions_user_problem` compound index on `submissions(user_id, problem_id)`

## User Statistics Calculation

Database-side aggregation queries calculate user submission metrics efficiently without loading entity collections into JVM memory:
- `totalSubmissions`: `COUNT(*)` for user
- `acceptedSubmissions`: `COUNT(*)` where `verdict = ACCEPTED`
- `acceptanceRate`: `(acceptedSubmissions / totalSubmissions) * 100`
- `problemsAttempted`: `COUNT(DISTINCT problem_id)` for user
- `problemsSolved`: `COUNT(DISTINCT problem_id)` where `verdict = ACCEPTED`
