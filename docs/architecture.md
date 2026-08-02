# Project Apricorn Architecture & Database Design

## Overview

Project Apricorn is a production-inspired online judge and competitive programming platform built with Spring Boot 3.3.5, Java 21 LTS, PostgreSQL 16, Flyway, JWT Authentication, and a restricted Docker execution engine.

## System Architecture Diagram

```mermaid
flowchart TD
    Client[Client / Frontend / HTTP]
    
    subgraph Spring Boot Backend
        SecurityFilter[Security Filter Chain & JWT Auth Filter]
        Controllers[REST Controllers API]
        Services[Domain & Analytics Services]
        TaskExec[ThreadPoolTaskExecutor / judgeTaskExecutor]
    end
    
    subgraph Persistence Layer
        Flyway[Flyway Migrations V1..V3]
        PostgreSQL[(PostgreSQL 16 Database)]
    end
    
    subgraph Online Judge Engine
        JudgeService[DockerJudgeService / StubJudgeService]
        Executor[JavaDockerExecutor Strategy]
        Runner[DefaultDockerRunner]
        DockerContainer[Restricted Docker Sandbox Container]
    end

    Client -->|HTTP / Bearer JWT| SecurityFilter
    SecurityFilter --> Controllers
    Controllers --> Services
    Services --> PostgreSQL
    Services -->|Async Evaluate| TaskExec
    TaskExec --> JudgeService
    JudgeService --> Executor
    Executor --> Runner
    Runner -->|docker run --network none| DockerContainer
```

## Database Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar role
        timestamp created_at
    }

    PROBLEMS {
        bigint id PK
        varchar title UK
        varchar slug UK
        text description
        varchar difficulty
        text input_format
        text output_format
        text constraints
        text sample_input
        text sample_output
        text sample_explanation
        int time_limit_ms
        int memory_limit_mb
        bigint created_by FK
        timestamp created_at
        timestamp updated_at
    }

    TAGS {
        bigint id PK
        varchar name UK
        varchar slug UK
        timestamp created_at
    }

    PROBLEM_TAGS {
        bigint problem_id PK, FK
        bigint tag_id PK, FK
    }

    TEST_CASES {
        bigint id PK
        bigint problem_id FK
        text input
        text expected_output
        boolean is_sample
        int order_index
        timestamp created_at
    }

    SUBMISSIONS {
        bigint id PK
        bigint user_id FK
        bigint problem_id FK
        varchar language
        text source_code
        varchar status
        varchar verdict
        int execution_time_ms
        int memory_used_kb
        int passed_test_cases
        int total_test_cases
        text error_message
        timestamp submitted_at
        timestamp updated_at
    }

    USERS ||--o{ SUBMISSIONS : "submits"
    PROBLEMS ||--o{ SUBMISSIONS : "has"
    PROBLEMS ||--o{ TEST_CASES : "contains"
    PROBLEMS }|--|{ TAGS : "categorized by (PROBLEM_TAGS)"
    USERS ||--o{ PROBLEMS : "creates"
```
