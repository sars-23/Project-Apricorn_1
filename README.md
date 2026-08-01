# Project Apricorn

Project Apricorn is a production-inspired online coding platform. This repository contains the initial Java 21 and Spring Boot 3 backend foundation; authentication and business modules are intentionally deferred.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose (for PostgreSQL)

## Run locally

1. Start PostgreSQL from the repository root:

   ```bash
   docker compose up -d
   ```

2. Start the backend:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

The development profile is active by default. It uses `jdbc:postgresql://localhost:5432/apricorn` with the local Docker credentials. Override values through `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` when required.

For production, activate the `prod` profile and provide all three database environment variables:

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://host:5432/apricorn DB_USERNAME=... DB_PASSWORD=... mvn spring-boot:run
```

## Endpoints

- `GET /api/v1/health` — returns service status and timestamp.
- `/swagger-ui.html` — interactive API documentation.
- `/api-docs` — OpenAPI specification.

## Architecture

The backend follows a clean, layered structure under `backend/src/main/java/com/apricorn`:

- `controller` exposes HTTP APIs and delegates work.
- `service` contains application use cases behind interfaces.
- `repository` and `entity` are reserved for persistence adapters and data models.
- `dto` and `mapper` define API/persistence boundaries.
- `config`, `security`, `exception`, `common`, and `util` hold cross-cutting concerns.
- `user`, `problem`, `submission`, `contest`, and `judge` are reserved feature modules.

Components use constructor injection. The current security configuration permits all requests only until authentication is introduced in a later phase.

## Verification

```bash
cd backend
mvn test
```
