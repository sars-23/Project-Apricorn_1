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

## Java Submission Contract (V1 Function Contract)

Submissions must conform to the new V1 function-based contract:
- Code must define `class Solution` (or similar non-public top-level class).
- Code must implement the method specified by `Problem.methodName` with matching `Problem.returnType` and `Problem.parameterMetadata`.
- The judge dynamically generates a `Main.java` harness to instantiate `Solution`, deserialize JSON test cases, invoke the function, and compare the structured returned output.
- Direct standard input (`System.in`) and standard output (`System.out.println`) are ignored and should not be used for correctness.

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

1. **Workspace Provisioning**: Creates an isolated temporary directory (`/tmp/apricorn-judge/<submissionId>-<uuid>/`), writes user's `Solution.java`, and generates `Main.java` harness based on Problem contract.
2. **Compilation**: Invokes `javac Main.java Solution.java` inside Docker container. If compilation fails or times out, returns `COMPILATION_ERROR` with sanitized stderr.
3. **Test Case Iteration**: Executes `java Main <testCaseIndex>` for each testcase ordered by `orderIndex`.
4. **Limits & Verdict Mapping**:
   - Time Limit: Exceeding `Problem.timeLimitMs` -> `TIME_LIMIT_EXCEEDED`
   - Output Limit: Exceeding 1 MB -> `RUNTIME_ERROR`
   - Non-zero Exit Code / Exception -> `RUNTIME_ERROR`
   - Output Mismatch (via typed `OutputComparator` reading from `result.out`) -> `WRONG_ANSWER`
   - All Test Cases Pass -> `ACCEPTED`
   - Infrastructure Failure -> `INTERNAL_ERROR`
5. **Defensive Cleanup**: Temporary workspace directory is always deleted in a `finally` block.
