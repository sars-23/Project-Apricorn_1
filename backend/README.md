# Project Apricorn

Project Apricorn is a full-featured, self-hosted Online Judge and Competitive Programming platform. It empowers users to solve algorithmic problems, submit code for evaluation, and receive real-time verdicts (Accepted, Wrong Answer, Compilation Error, Time Limit Exceeded) against hidden test cases. 

## Features
- **User Authentication**: Secure JWT-based authentication and PostgreSQL-backed session management.
- **Problem Workspace**: A rich split-screen workspace utilizing Monaco Editor, featuring syntax highlighting, autosave, and detailed verdict panels.
- **Docker Judge**: Safe, isolated execution of user-submitted Java code via native Docker daemon integration, enforcing strict CPU, Memory, and Time limits.
- **Responsive Design**: Modern Glassmorphism UI built with Bootstrap 5 and vanilla JavaScript.
- **Seed Problems**: Includes 15+ canonical problems across Easy, Medium, and Hard categories out of the box.

## Architecture & Flow

### Judge Execution Flow
1. User clicks **Submit** from the UI.
2. Spring Boot `SubmissionService` saves the submission in a `PENDING` state to PostgreSQL.
3. The Submission is dispatched asynchronously to the `DockerJudgeService`.
4. A secure temporary directory is created with the user's `Main.java`.
5. Docker runs a compilation step via `eclipse-temurin:21-jdk-alpine`.
6. Docker runs the compiled byte-code against each hidden test case, strictly enforcing memory/time limits.
7. Results are compared against expected outputs. The state is updated to `COMPLETED` along with execution metrics.
8. The UI asynchronously polls and renders the final verdict along with actual/expected output diffs.

## Tech Stack
- **Frontend**: HTML5, Thymeleaf, Bootstrap 5, Monaco Editor, Vanilla JS.
- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA.
- **Database**: PostgreSQL 15, managed via Flyway migrations.
- **Execution Engine**: Docker (Java Runner).

## Installation Guide

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker (running locally)
- PostgreSQL (running locally or via docker-compose)

### Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-org/apricorn.git
   cd apricorn/backend
   ```

2. **Configure Database**:
   Create a database named `apricorn` in PostgreSQL. Ensure the credentials in `src/main/resources/application.yml` match your local setup.

3. **Run Migrations & Tests**:
   Ensure everything is properly set up by running the regression test suite.
   ```bash
   mvn clean test
   ```

4. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```
   The application will be available at `http://localhost:8080`.

## Future Roadmap
- **Phase 4 - User Platform**: Dashboard, Leaderboards, User Profiles, and Submission History.
- **Phase 5 - Contest Mode**: Timed competitive programming contests and real-time leaderboards.
- **Multi-language Support**: Expanding execution containers to support Python, C++, and Go.
