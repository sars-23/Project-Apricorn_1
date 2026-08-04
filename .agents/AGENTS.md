# Project Apricorn Rules

## IMPORTANT ENGINEERING RULE

Before writing or modifying ANY code:
1. Inspect the existing implementation.
2. Never assume DTO fields or service methods exist.
3. Never fabricate getters, setters, or APIs.
4. Reuse the existing architecture.
5. If integration requires changing backend contracts, STOP and explain why instead of making breaking changes.
6. The application must compile after every phase.
7. Run:
   - `mvn clean test`
   - `mvn spring-boot:run`
8. Do not report success unless both commands succeed.
