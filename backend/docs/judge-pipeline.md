# Project Apricorn — Judge Pipeline Specification

This document details the real-time execution lifecycle of code submissions evaluated by **Project Apricorn**.

---

## 1. Submission Lifecycle Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User as Client Browser
    participant API as Submission Controller
    participant DB as PostgreSQL DB
    participant Listener as Async Submission Listener
    participant Judge as Judge Service
    participant Executor as Java Docker Executor
    participant Docker as Docker Engine

    User->>API: POST /api/v1/problems/{slug}/submissions (Language, Source Code)
    API->>DB: Save Submission (status = PENDING, verdict = null)
    API-->>User: 202 Accepted (submissionId)
    
    API->>Listener: Publish SubmissionCreatedEvent
    Listener->>Judge: judgeSubmissionAsync(submissionId)
    Judge->>DB: Update Submission status = RUNNING
    
    Judge->>Harness: Generate Main.java (wraps Solution method)
    Judge->>Executor: execute(Submission, TestCases)
    Executor->>Executor: Create isolated temp host workspace
    Executor->>Docker: docker run javac Main.java Solution.java
    
    alt Compilation Error
        Docker-->>Executor: javac exit code != 0 (stdout/stderr error)
        Executor-->>Judge: Result (COMPILATION_ERROR, compilation log)
    else Compilation Success
        loop For each Structured Test Case
            Executor->>Docker: docker run java Main (executes test input JSON)
            alt Execution Timeout
                Docker-->>Executor: Timeout exceeded (> timeLimitMs + 1000ms)
                Executor-->>Judge: Result (TIME_LIMIT_EXCEEDED)
            else Runtime Exception
                Docker-->>Executor: Exception thrown in Solution method
                Executor-->>Judge: Result (RUNTIME_ERROR)
            else Output Mismatch
                Docker-->>Executor: Serialized return value != expectedOutput JSON
                Executor-->>Judge: Result (WRONG_ANSWER, diff details)
            else Correct Output
                Docker-->>Executor: Return value == expectedOutput JSON
            end
        end
    end

    Executor->>Executor: Clean up temp host workspace
    Executor-->>Judge: Final Execution Result (Verdict, Passed/Total Tests, Runtime, Memory)
    Judge->>DB: Save Submission (status = COMPLETED, verdict, executionTimeMs, memoryUsedKb)
    
    loop Every 1.5s until COMPLETED
        User->>API: GET /api/v1/submissions/{id}
        API->>DB: Fetch Submission Status
        API-->>User: Submission Detail (status, verdict, metrics)
    end
```

---

## 2. Verdict Definitions

- **`ACCEPTED`**: The code compiled successfully and produced exactly matching output for 100% of hidden test cases within memory and time constraints.
- **`WRONG_ANSWER`**: The code compiled and executed, but output diverged from expected output on one or more test cases. Detailed input, expected output, and actual output diffs are rendered to the owner.
- **`COMPILATION_ERROR`**: The Java compiler (`javac`) returned a non-zero exit code. Raw compiler output is captured and saved for user inspection.
- **`RUNTIME_ERROR`**: The Java virtual machine threw an unhandled exception (e.g. `ArrayIndexOutOfBoundsException`, `NullPointerException`). Exception trace snippets are preserved.
- **`TIME_LIMIT_EXCEEDED`**: Execution exceeded the configured per-problem time limit (`timeLimitMs`).
- **`INTERNAL_ERROR`**: Platform or system error (e.g. missing test cases in database or Docker daemon failure). This status is explicitly excluded from user failure metrics.

---

## 3. Safety & Zero-Test-Case Protection

1. **Empty Test-Case Protection**: If a problem has zero associated test cases in the database, `JudgeServiceImpl` immediately sets `verdict = INTERNAL_ERROR` and logs a critical warning. A problem with zero test cases can **never** return `ACCEPTED`.
2. **Container Cleanup**: Host workspaces and temporary source files are deleted inside a `finally` block. Docker containers are launched with `--rm` to auto-remove upon exit.
3. **Execution Limits**:
   - Memory Cap: `--memory <memoryLimitMb>m`
   - CPU Ceiling: `--cpus 0.5`
   - Network: `--network none`
