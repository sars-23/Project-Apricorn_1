# Secure Docker-Based Online Judge Engine Architecture

## Overview

The Project Apricorn Judge Engine provides isolated compilation and execution of untrusted Java submissions inside restricted Docker containers. It decouples submission lifecycle management from language execution via a clean Strategy pattern (`LanguageExecutor`) and abstracts container interaction (`DockerRunner`).

## Architecture Diagram

```text
SubmissionController
       │
       ▼
SubmissionService
       │
       ▼
JudgeService (Interface)
       │
       ▼
DockerJudgeService (or StubJudgeService fallback)
       │
       ▼
LanguageExecutor Strategy (JavaDockerExecutor)
       │
       ├─────────────────────────────────┐
       ▼                                 ▼
DockerRunner (DefaultDockerRunner)   OutputComparator
       │
       ▼
Restricted Docker Container
 (sh -c "javac Main.java && java Main")
       │
       ▼
  JudgeResult
       │
       ▼
SubmissionService (Persists Verdict & Metrics to PostgreSQL)
```

## Java Submission Contract

Submissions must conform to the standard competitive programming Java contract:
- Code must define `public class Main`.
- Input is read from standard input (`System.in` / `Scanner`).
- Output is written to standard output (`System.out.println`).

## Configuration Properties (`application.yml`)

```yaml
judge:
  mode: ${JUDGE_MODE:stub}                 # "docker" or "stub"
  java-image: eclipse-temurin:21-jdk       # Docker JDK runtime image
  workspace-root: /tmp/apricorn-judge      # Base directory for unique submission workspaces
  max-output-bytes: 1048576                 # 1 MB max stdout/stderr capture
  compile-timeout-ms: 10000                 # 10s compiler timeout
  container-memory-mb: 256                 # 256 MB RAM limit per container
  container-cpus: 0.5                      # 0.5 CPU cores limit per container
```

## Execution Flow & Verdict Mapping

1. **Workspace Provisioning**: Creates an isolated temporary directory (`/tmp/apricorn-judge/<submissionId>-<uuid>/`) and writes `Main.java`.
2. **Compilation**: Invokes `javac Main.java` inside Docker container. If compilation fails or times out, returns `COMPILATION_ERROR` with sanitized stderr.
3. **Test Case Iteration**: Executes `java Main` for each test case ordered by `orderIndex`.
4. **Limits & Verdict Mapping**:
   - Time Limit: Exceeding `Problem.timeLimitMs` -> `TIME_LIMIT_EXCEEDED`
   - Output Limit: Exceeding 1 MB -> `RUNTIME_ERROR`
   - Non-zero Exit Code / Exception -> `RUNTIME_ERROR`
   - Output Mismatch (via `OutputComparator`) -> `WRONG_ANSWER`
   - All Test Cases Pass -> `ACCEPTED`
   - Infrastructure Failure -> `INTERNAL_ERROR`
5. **Defensive Cleanup**: Temporary workspace directory is always deleted in a `finally` block.
