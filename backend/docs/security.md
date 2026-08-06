# Project Apricorn — Security & Sandbox Specification

This document describes the security controls, authentication mechanisms, data isolation safeguards, and container sandbox guarantees implemented in **Project Apricorn**.

---

## 1. Authentication & Authorization Controls

- **Password Hashing**: User passwords are encrypted using BCrypt (`BCryptPasswordEncoder`).
- **Stateless Authentication**:
  - Web UI uses HttpOnly `JWT` cookies (`jwt_token`).
  - REST APIs accept `Authorization: Bearer <token>` headers.
- **JWT Protection**:
  - Tokens are signed with HMAC-SHA256.
  - Startup validation enforces that `JWT_SECRET` must be at least 32 characters/bytes long. Unconfigured or insecure secrets abort application startup immediately.
- **Cross-User Data Isolation**:
  - REST endpoints and page controllers extract the authenticated principal's ID (`@AuthenticationPrincipal UserPrincipal`).
  - Access to raw source code, submission details, or solution journeys belonging to another user throws `UnauthorizedSubmissionAccessException` (`403 FORBIDDEN`).

---

## 2. Container Sandbox Security

Untrusted user Java code is executed inside Docker containers (`eclipse-temurin:21-jdk`).

```text
[ Untrusted User Java Source ] ──► [ Isolated Host Workspace ] ──► [ Docker Container ]
                                                                       ├── --network none
                                                                       ├── --memory 256m
                                                                       ├── --cpus 0.5
                                                                       └── --rm (auto-cleanup)
```

### Sandbox Restrictions
1. **Network Isolation**: Containers run with `--network none`, blocking all inbound/outbound socket connections, HTTP requests, or DNS lookups.
2. **Memory Ceiling**: Default limit of 256MB (`--memory 256m`) prevents memory exhaustion attacks or heap bombs.
3. **CPU Allocation**: Execution capped at 0.5 CPU core (`--cpus 0.5`) to prevent CPU starvation.
4. **Execution Timeout**: Per-problem timeout (`timeLimitMs` + 1s buffer) terminates infinite loops via `Process.destroyForcibly()`.
5. **Container Lifecycle**: Containers are instantiated with `--rm` to ensure automatic destruction upon completion or failure.
6. **Workspace Cleanup**: Temporary host directories containing source files are deleted inside a `finally` block using `FileSystemUtils.deleteRecursively()`.

---

## 3. Honest Trust Boundary Disclosures

- **Docker Host Access**: In production, the Spring Boot application invokes local Docker engine commands. The host process requires access to `/var/run/docker.sock` or local Docker API.
- **Single-Host Isolation**: While container restrictions (`--network none`, `--memory`, `--cpus`) prevent user code from accessing external networks or starving host memory, container security relies on Linux kernel namespaces and cgroups. Mounts of `/var/run/docker.sock` into an untrusted container are strictly avoided.
