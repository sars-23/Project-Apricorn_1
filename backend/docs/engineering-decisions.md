# Project Apricorn — Architecture Decision Records (ADRs)

This document records the major engineering decisions, trade-offs, and future scalability paths for **Project Apricorn**.

---

## ADR 1: Java 21 & Spring Boot 3.3.5
- **Context**: Requires a robust, statically-typed, modern enterprise stack.
- **Decision**: Adopted Java 21 LTS and Spring Boot 3.3.5.
- **Rationale**: Leverages virtual threads, record classes, modern Spring Data JPA features, and native Docker integration.

---

## ADR 2: PostgreSQL & Flyway Schema Migrations
- **Context**: Relational database with strict data consistency requirements for user accounts, submissions, and problem catalogs.
- **Decision**: PostgreSQL 16 managed by Flyway migrations (`V1` through `V9`).
- **Rationale**: Enables version-controlled schema evolution and reproducible fresh database setups without manual SQL execution.

---

## ADR 3: Native Docker Container Isolation for Code Evaluation
- **Context**: Executing untrusted user code requires strict process isolation, network blocking, and resource capping.
- **Decision**: Launch short-lived Docker containers (`eclipse-temurin:21-jdk`) per submission.
- **Rationale**: Native kernel namespaces and cgroups provide reliable network isolation (`--network none`) and memory/CPU limits without complex custom sandboxing binaries.

---

## ADR 4: Asynchronous Submission Judging Pipeline
- **Context**: Synchronous judging causes HTTP thread blocking and slow user response times.
- **Decision**: `POST /api/v1/problems/{slug}/submissions` returns `202 ACCEPTED` immediately and dispatches judging asynchronously via Spring `@Async` and `SubmissionEventListener`.
- **Rationale**: Keeps REST APIs sub-second fast while client workspace UI polls execution progress every 1.5s.

---

## ADR 5: Deterministic Solution Insights vs LLM / External AI APIs
- **Context**: Providing actionable feedback on user submission patterns.
- **Decision**: Implement a deterministic, database-derived insights engine rather than calling external LLM APIs.
- **Rationale**: Guarantees 100% reproducible, zero-cost, zero-latency, explainable insights derived strictly from verified database submission history without hallucination or third-party API dependencies.

---

## ADR 6: Testcontainers PostgreSQL for Integration Testing
- **Context**: Integration tests require a real PostgreSQL database instance without polluting developer or staging databases.
- **Decision**: Adopt Testcontainers PostgreSQL driver (`jdbc:tc:postgresql:16-alpine:///apricorn_test`).
- **Rationale**: Spins up an isolated PostgreSQL container during `./mvnw clean test`, ensuring `mvn clean test` leaves development PostgreSQL untouched.

---

## ADR 7: V1 Single-Node Architecture & Future Scaling Path
- **Context**: V1 architecture uses single-host Spring Boot with local Docker daemon and in-memory `@Async` judging event dispatch.
- **Decision**: Accept single-node design for V1 simplicity.
- **Future Scaling Path**:
  - Replace in-memory `@Async` dispatch with a durable queue (e.g. Redis / RabbitMQ / SQS).
  - Decouple judge workers into dedicated containerized worker nodes for horizontal scalability.
  - Add batch retrieval for solution insights processing.
