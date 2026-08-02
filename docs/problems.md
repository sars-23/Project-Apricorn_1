# Coding Problem Management System

## Overview

The problem management module manages coding challenges, tags, and judge test cases for Project Apricorn. It exposes database-side paginated search and filtering for end-users while restricting administrative operations (problem CRUD, tag management, test case management) to `ADMIN` users.

## Architecture

```text
Client
  │
  ▼
ProblemController / TagController
  │
  ▼
ProblemService / TagService / TestCaseService
  │
  ├──────────────┐
  ▼              ▼
ProblemRepository TagRepository / TestCaseRepository
  │
  ▼
PostgreSQL (problems, tags, problem_tags, test_cases)
```

## Entity Relationship Diagram

```text
USER (users)
  │
  │ createdBy (1:N)
  ▼
PROBLEM (problems) ─────────────┐
  │                             │
  │ (1:N)                       │ (N:M)
  ▼                             ▼
TEST_CASE (test_cases)       PROBLEM_TAG (problem_tags)
                                │
                                ▼
                             TAG (tags)
```

## Hidden Test Case Security

- Test cases marked with `isSample = false` are hidden judge test cases used exclusively by the future Judge Engine.
- Public APIs (e.g., `GET /api/v1/problems/{slug}`) return problem details, description, constraints, and sample inputs/outputs, but **never** serialize or expose the hidden `testCases` collection.
- Hidden test cases are accessible only via admin endpoints (`GET /api/v1/admin/problems/{problemId}/test-cases`) requiring `ROLE_ADMIN`.

## Search, Filtering, Pagination & Performance

- **Filtering**: Supports filtering by difficulty (`EASY`, `MEDIUM`, `HARD`), tag slug (`tag`), and title search (`search`).
- **Database-side execution**: Search and filtering are executed directly in PostgreSQL using Spring Data JPA `Specification` and `CriteriaBuilder`. No in-memory filtering is performed.
- **N+1 Prevention**: `ProblemRepository` uses `@EntityGraph(attributePaths = {"tags"})` to fetch problem tag associations in a single query.
- **Pagination & Sorting**: Paginated results return `PageResponse<T>` metadata with a maximum page size limit of 100. Whitelisted sort parameters prevent unsafe query injections.

## Database Schema & Indexes

- `problems`: `slug` (unique), `difficulty`, `title`, `created_by_id` (FK to `users`), `time_limit_ms`, `memory_limit_mb`.
  - Indexes: `idx_problems_slug`, `idx_problems_difficulty`, `idx_problems_title`.
- `tags`: `name` (unique), `slug` (unique).
  - Index: `idx_tags_slug`.
- `problem_tags`: composite primary key `(problem_id, tag_id)` with cascading foreign keys.
- `test_cases`: `problem_id` (FK to `problems`), `input`, `expected_output`, `is_sample`, `order_index`.
  - Index: `idx_test_cases_problem_id`.
