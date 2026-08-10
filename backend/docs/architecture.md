# Project Apricorn — System Architecture Specification

This document details the software architecture, component relationships, and execution boundaries of **Project Apricorn**.

---

## 1. System Architecture Diagram

```mermaid
graph TD
    User["Client Browser (Monaco Editor & UI)"]
    
    subgraph Web Layer
        WebCtrl["Web Controllers (Thymeleaf Pages)"]
        RestCtrl["REST Controllers (JSON APIs)"]
        JwtFilter["Security Filter (JWT / Cookie Auth)"]
    end
    
    subgraph Service Layer
        ProblemSvc["Problem Service"]
        SubSvc["Submission Service"]
        AnalyticsSvc["Analytics Engine 2.0 Service"]
        JourneySvc["Solution Journey Service"]
        InsightsSvc["Solution Insights Service"]
        JudgeSvc["Async Judge Service"]
    end
    
    subgraph Persistence Layer
        DB[("PostgreSQL Database (Flyway V1-V12)")]
    end
    
    subgraph Execution Infrastructure
        DockerExec["Java Docker Executor"]
        HarnessGen["Java Harness Generator"]
        DockerEngine["Local Docker Engine Daemon"]
        Container["Isolated Java Runner Container (--network none, 256MB, 0.5 CPU)"]
    end
    
    User -->|HTTP Requests| JwtFilter
    JwtFilter --> WebCtrl
    JwtFilter --> RestCtrl
    
    WebCtrl --> ProblemSvc
    WebCtrl --> AnalyticsSvc
    WebCtrl --> JourneySvc
    WebCtrl --> InsightsSvc
    
    RestCtrl --> SubSvc
    
    SubSvc -->|Save PENDING| DB
    SubSvc -->|Dispatch Async Event| JudgeSvc
    
    JudgeSvc -->|Update RUNNING / COMPLETED| DB
    JudgeSvc --> HarnessGen
    JudgeSvc --> DockerExec
    DockerExec -->|Docker API / CLI| DockerEngine
    DockerEngine -->|Run javac Main.java & java Main| Container
    
    AnalyticsSvc --> DB
    JourneySvc --> DB
    InsightsSvc --> DB
```

---

## 2. Layer Responsibilities

### Web & Security Layer (`com.apricorn.web`, `com.apricorn.security`)
- Handles HTTP requests, Thymeleaf page rendering, and REST API serialization.
- Enforces stateless authentication via JWT tokens and HttpOnly cookies.
- Protects private user endpoints and isolates user-specific submission queries.

### Service Layer (`com.apricorn.*.service`)
- **Problem Service**: Manages problem catalog search, tag/difficulty filtering, and user progress summaries.
- **Submission Service**: Orchestrates code submission creation, validation, and user history retrieval.
- **Async Judge Service**: Handles asynchronous judging dispatch, container execution, and verdict persistence.
- **Analytics Engine 2.0**: Computes real-time problem-solving statistics, verdict distributions, and activity trends.
- **Solution Journey Service**: Generates chronological attempt timelines, milestone badges, and performance comparisons.
- **Solution Insights Service**: Executes deterministic analysis over submission history to identify failure patterns and active blockers.

### Persistence Layer (`com.apricorn.*.repository`, `com.apricorn.*.entity`)
- Managed by Spring Data JPA and Flyway schema migrations (`V1` through `V12`).
- Indexed for user-scoped queries and chronological timeline queries (`idx_submissions_user_problem_submitted_at`).

### Execution Infrastructure (`com.apricorn.judge.executor`)
- `JavaHarnessGenerator`: Constructs executable `Main.java` wrappers that parse structured testcase parameters, invoke user `Solution` methods, serialize returns to `result.out`, and sanitize user standard output.
- `JavaDockerExecutor`: Launches short-lived Docker containers (`eclipse-temurin:21-jdk`) for isolated code compilation and test-case execution under strict memory, CPU, and network constraints.
