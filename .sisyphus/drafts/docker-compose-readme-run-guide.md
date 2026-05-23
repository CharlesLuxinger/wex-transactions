# Draft: Docker Compose Readme Run Guide

## Requirements (confirmed)
- create Docker, Docker Compose, README, HOW TO RUN
- reference examples from estaparking project paths provided by user

## Technical Decisions
- README scope: full rewrite (user confirmed)
- Compose scope: app + dependencies with PostgreSQL 18 (user confirmed)
- Compose filename: `compose.yml` (aligned with user example)
- Run guide location: `docs/run-and-test-guide.md` (aligned with user example)

## Research Findings
- No `Dockerfile`, `compose.yml`, `.dockerignore`, or run guide currently exist
- `README.md` exists but minimal (badges + title only)
- Runtime env pattern found in `src/main/resources/application.yaml`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- CI verification flow found in `.github/workflows/ci.yml`: ktlint check, detekt, tests + jacoco
- Local verification flow found in `AGENTS.md`: format, ktlint check, detekt, test
- Test infra exists: JUnit5, RestAssured, Testcontainers PostgreSQL 18.1, ArchUnit

## Open Questions
- None blocking

## Scope Boundaries
- INCLUDE: Dockerfile, compose.yml, full README rewrite, docs/run-and-test-guide.md
- EXCLUDE: application business logic changes; architecture refactors; unrelated CI changes
