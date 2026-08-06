# Project Apricorn — Production Deployment Guide

This guide details the production deployment architecture, environment configuration, database migration requirements, Docker judge constraints, and operational readiness for **Project Apricorn**.

---

## 1. System Architecture & Trust Boundaries

```text
[ Internet Client ]
       │
       ▼
[ Reverse Proxy / Load Balancer (Nginx / ALB) ]
       │  (Port 8080 HTTPS termination)
       ▼
[ Project Apricorn Host Process (Java 21 / Spring Boot) ]
       ├──► [ PostgreSQL Database ] (Port 5432)
       └──► [ Local Docker Engine Daemon ] (Unix socket: /var/run/docker.sock)
                   │
                   ▼ (ephemeral containers)
            [ Isolated Judge Containers ] (apricorn-java-runner)
```

### Critical Deployment Constraint (Docker Judge Engine)
Project Apricorn executes untrusted Java code inside isolated Docker containers using local Docker CLI commands (`docker run --rm --network none ...`).
- **Production Host Requirement**: Spring Boot MUST run on an operating system or VM instance with access to a local Docker daemon (e.g. AWS EC2, GCP Compute Engine, DigitalOcean Droplet, or dedicated Linux VPS).
- **Security Boundary**: Spring Boot runs on the host process (or a trusted VM control plane). Do **NOT** casually mount `/var/run/docker.sock` into an untrusted public-facing container without host isolation.

---

## 2. Prerequisites & Environment Setup

- **Operating System**: Linux (Ubuntu 22.04 LTS / Debian 12 / RHEL 9 recommended) or Windows Server.
- **Java Runtime**: OpenJDK 21 LTS (`java -version`).
- **Build Tool**: Official Maven Wrapper (`./mvnw`).
- **Database**: PostgreSQL 16+.
- **Container Runtime**: Docker Engine 24.0+ (`docker --version`).

---

## 3. Environment Variables Reference

Configuration values are supplied via environment variables (`.env` file or system environment).

| Environment Variable | Required | Default | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` | Active Spring profile (`prod`). |
| `SERVER_PORT` | No | `8080` | HTTP port for application server. |
| `DB_HOST` | Yes | `localhost` | PostgreSQL host. |
| `DB_PORT` | No | `5432` | PostgreSQL port. |
| `DB_NAME` | Yes | `apricorn` | Database name. |
| `DB_USERNAME` | Yes | - | PostgreSQL user name. |
| `DB_PASSWORD` | Yes | - | PostgreSQL password. |
| `JWT_SECRET` | **YES** | - | Secret signing key (**MUST BE $\ge 32$ CHARACTERS**). Application startup aborts if missing. |
| `JWT_EXPIRATION_MS` | No | `3600000` | Token validity duration in milliseconds (default 1 hour). |
| `JUDGE_MODE` | No | `docker` | Code execution engine mode (`docker`). |

---

## 4. Database Setup & Flyway Schema Migrations

Production database management enforces `ddl-auto: validate` to preserve data integrity.
1. Schema initialization and incremental updates are managed automatically by Flyway (`spring.flyway.enabled=true`).
2. Migrations `V1` through `V9` run sequentially on application startup:
   - `V1`: Core schema tables.
   - `V2`–`V3`: Query indexes.
   - `V4` & `V6`: Seed canonical problems & test cases.
   - `V7`–`V9`: Status, verdict, and chronological journey timeline indexes.

---

## 5. Building & Packaging

To package the production executable JAR:
```bash
./mvnw clean package -DskipTests
```
The output artifact is generated at:
`target/apricorn-backend-0.0.1-SNAPSHOT.jar`

---

## 6. Docker Judge Runner Image Preparation

Before starting the Spring Boot host process, build the lightweight judge runner image:
```bash
docker build -t eclipse-temurin:21-jdk -f - . <<'EOF'
FROM eclipse-temurin:21-jdk-alpine
EOF
```
Verify container execution isolation:
- Network: `--network none`
- Memory Limit: `256m`
- CPU Ceiling: `0.5`
- Auto-removal: `--rm`

---

## 7. Starting the Production Server

Using system environment variables or `.env`:
```bash
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=apricorn
export DB_USERNAME=apricorn_app
export DB_PASSWORD="YourStrongDatabasePassword123!"
export JWT_SECRET="SuperSecretProductionSigningKeyWhichIsAtLeast32CharsLong!"

java -jar target/apricorn-backend-0.0.1-SNAPSHOT.jar
```

---

## 8. Health Verification & Monitoring

- **Health Check Endpoint**: `GET /actuator/health`
  - Expected Response: `200 OK`
  - Payload: `{"status":"UP"}`
- **OpenAPI Documentation**: `GET /swagger-ui.html` / `GET /api-docs`

---

## 9. V1 Deployment Hosting Recommendation

For V1 deployment simplicity and security:
- **Recommended Architecture**: Single Linux VPS (e.g. AWS EC2 t3.medium or Hetzner Cloud CPX21).
- **Services**:
  1. PostgreSQL 16 container or managed DB instance.
  2. Spring Boot application running as systemd service or Docker host process.
  3. Local Docker Engine running code execution runner containers.
  4. Nginx reverse proxy providing HTTPS termination via Let's Encrypt.
