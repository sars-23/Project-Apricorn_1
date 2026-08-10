# Project Apricorn — Data & Domain Model Specification

This document describes the core domain entities, database relationships, and analytical derivation strategy in **Project Apricorn**.

---

## 1. Entity Relationship Diagram

```mermaid
erDiagram
    USER ||--o{ SUBMISSION : submits
    PROBLEM ||--o{ SUBMISSION : evaluates
    PROBLEM ||--o{ PROBLEM_TEST_CASE : contains
    PROBLEM }|--|{ PROBLEM_TAG : tagged_with

    USER {
        bigint id PK
        string email UK
        string username UK
        string password
        string role
        timestamp created_at
    }

    PROBLEM {
        bigint id PK
        string title
        string slug UK
        string difficulty
        text description
        text input_format
        text output_format
        text constraints
        text sample_input
        text sample_output
        text explanation
        string method_name
        string return_type
        text parameter_metadata
        text starter_code
        int time_limit_ms
        int memory_limit_mb
        timestamp created_at
    }

    PROBLEM_TEST_CASE {
        bigint id PK
        bigint problem_id FK
        text input
        text expected_output
        boolean is_sample
        int order_index
    }

    PROBLEM_TAG {
        bigint id PK
        string name UK
        string slug UK
    }

    SUBMISSION {
        bigint id PK
        bigint user_id FK
        bigint problem_id FK
        string language
        text source_code
        string status
        string verdict
        int passed_test_cases
        int total_test_cases
        int execution_time_ms
        int memory_used_kb
        text error_message
        timestamp submitted_at
    }
```

---

## 2. Dynamic Analytical Derivation Strategy

To maintain database consistency and eliminate data synchronization bugs, Project Apricorn **never stores duplicated summary counters or static analytical snapshots**.

Instead, all user analytics, journeys, and insights are computed dynamically from immutable `Submission` records:

### A. Growth Dashboard Analytics (`AnalyticsService`)
- **Total Solved**: `COUNT(DISTINCT problem_id)` where `verdict = ACCEPTED` and `status = COMPLETED`.
- **Acceptance Rate**: `(ACCEPTED Submissions / Judged Submissions) * 100`.
- **Difficulty Breakdown**: Grouped count of distinct solved problems matching `EASY`, `MEDIUM`, and `HARD`.

### B. Solution Journey (`JourneyService`)
- Derived by querying user submissions for a specific problem ID sorted by `submittedAt ASC`.
- **Milestones**:
  - `First Accepted`: The earliest attempt index where `verdict = ACCEPTED`.
  - `Personal Best Runtime`: The accepted attempt with the minimum `executionTimeMs`.
  - `Personal Best Memory`: The accepted attempt with the minimum `memoryUsedKb`.

### C. Solution Insights (`InsightsService`)
- Derived deterministically by evaluating all attempted problem submission histories for the user.
- Identifies first-try solves, terminal failure pattern weaknesses, debugging persistence, active blockers, and 7-day submission momentum.
