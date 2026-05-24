# Docker + Compose + Docs Delivery Plan

## TL;DR
> **Summary**: Add container runtime packaging and operator documentation for `wex-transactions` using PostgreSQL 18, with full README rewrite and dedicated run/test guide.
> **Deliverables**:
> - `Dockerfile`
> - `compose.yml`
> - `README.md` (full rewrite)
> - `docs/run-and-test-guide.md`
> **Effort**: Medium
> **Parallel**: YES - 2 waves
> **Critical Path**: Task 1 → Task 2 → Task 5

## Context
### Original Request
Create Docker, Docker Compose, README, and HOW TO RUN, following patterns from estaparking examples.

### Interview Summary
- README mode: full rewrite.
- Compose scope: app + dependency stack with PostgreSQL 18.
- Delivery scope fixed to runtime artifacts and documentation only.

### Metis Review (gaps addressed)
- Guardrail applied: no business-code mutation.
- Guardrail applied: avoid endpoint assumptions for health checks.
- Guardrail applied: prevent scope creep (`Makefile`, CI redesign, actuator additions).
- Default applied: run guide path set to `docs/run-and-test-guide.md` (user example alignment).

## Work Objectives
### Core Objective
Deliver reproducible local runtime via Docker Compose and clear operator docs aligned with project build/test truth.

### Deliverables
- Working `Dockerfile` for Spring Boot app build/runtime.
- Working `compose.yml` with `app` + `postgres` services.
- Rewritten `README.md` with setup, run, verify, troubleshoot sections.
- `docs/run-and-test-guide.md` with step-by-step run/test/teardown flows.

### Definition of Done (verifiable conditions with commands)
- `Test-Path -Path Dockerfile -PathType Leaf`
- `Test-Path -Path compose.yml -PathType Leaf`
- `Test-Path -Path README.md -PathType Leaf`
- `Test-Path -Path docs/run-and-test-guide.md -PathType Leaf`
- `docker build -t wex-transactions:test .` exits `0`.
- `docker compose -f compose.yml up -d` starts both services.
- `docker compose -f compose.yml ps` shows `app` and `postgres` running.
- `docker compose -f compose.yml logs app` shows Spring startup success and no datasource auth failure.
- `docker compose -f compose.yml down -v` removes stack.

### Must Have
- PostgreSQL 18 image family in compose.
- Env wiring through `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- Commands in docs reflect CI/local verification order with explicit split:
  - CI section must include `./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification` from `.github/workflows/ci.yml`.
  - Local section must include `./gradlew test` sequence from `AGENTS.md`.
- QA evidence files generated under `.sisyphus/evidence/`.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No source-code/business logic refactors.
- No Gradle/plugin/config redesign.
- No undocumented assumptions about API endpoints.
- No extra infra artifacts (`Makefile`, scripts, k8s manifests) unless explicitly requested.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after, existing Gradle + JUnit5/RestAssured/Testcontainers stack.
- QA policy: Every task includes happy-path and failure-path scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.

Wave 1:
- Task 1 (container build baseline)
- Task 2 (compose stack)
- Task 3 (README structure rewrite)
- Task 4 (run-and-test guide scaffold)

Wave 2:
- Task 5 (cross-doc command parity + env matrix + troubleshooting)
- Task 6 (end-to-end docker validation + evidence consolidation)

### Dependency Matrix (full, all tasks)
- Task 1: Blocked By none; Blocks 2, 6
- Task 2: Blocked By 1; Blocks 5, 6
- Task 3: Blocked By none; Blocks 5
- Task 4: Blocked By none; Blocks 5
- Task 5: Blocked By 2,3,4; Blocks 6
- Task 6: Blocked By 1,2,5; Blocks final wave

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 4 tasks → `unspecified-high`, `writing`
- Wave 2 → 2 tasks → `unspecified-high`, `writing`

## TODOs
> Implementation + Test = ONE task. Never separate.

- [ ] 1. Create production-safe Dockerfile baseline

  **What to do**:
  - Add multi-stage Dockerfile for Gradle build and runtime.
  - Pin Java runtime compatible with project (`Java 25` from Gradle config).
  - Expose application port `8080`.
  - Ensure image runs via packaged jar (non-dev mode).

  **Must NOT do**:
  - Do not alter application Kotlin/Java code.
  - Do not add new application dependencies.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: cross-check build/runtime compatibility.
  - Skills: [`kotlin-springboot`] - align Spring Boot runtime conventions.
  - Omitted: [`clean-ddd-hexagonal`] - not architecture redesign work.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2,6 | Blocked By: none

  **References**:
  - Build/runtime version source: `build.gradle.kts`
  - App packaging pattern: `build.gradle.kts`
  - Runtime env usage: `src/main/resources/application.yaml`

  **Acceptance Criteria**:
  - [ ] `Dockerfile` exists and has build + runtime stages.
  - [ ] `docker build -t wex-transactions:test .` exits `0`.

  **QA Scenarios**:
  ```
  Scenario: Happy path image build
    Tool: Bash
    Steps: docker build -t wex-transactions:test .
    Expected: Exit code 0; image present in docker images list
    Evidence: .sisyphus/evidence/task-1-dockerfile-build.txt

  Scenario: Failure path invalid Dockerfile path
    Tool: Bash
    Steps: docker build -f ./nonexistent.Dockerfile -t wex-transactions:test .
    Expected: Exit code 1; error about file not found
    Evidence: .sisyphus/evidence/task-1-dockerfile-build-error.txt
  ```

  **Commit**: YES | Message: `feat(devops): add dockerfile for app image` | Files: `Dockerfile`

- [ ] 2. Create compose stack (app + PostgreSQL 18)

  **What to do**:
  - Add `compose.yml` with services `app` and `postgres`.
  - Use PostgreSQL 18 image line.
  - Wire env vars for app DB connection (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
  - Add service dependency/ordering and persistent named volume.

  **Must NOT do**:
  - Do not add Redis or extra dependencies.
  - Do not add custom SQL bootstrap unless required by existing migrations behavior.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: runtime orchestration and env wiring.
  - Skills: [`kotlin-springboot`] - Spring env/run alignment.
  - Omitted: [`gh-cli`] - no GitHub ops needed.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 5,6 | Blocked By: 1

  **References**:
  - Env contract: `src/main/resources/application.yaml`
  - Persistence expectations: `src/main/resources/application.yaml`
  - Testcontainers version signal: `src/test/kotlin/com/charlesluxinger/wex_transactions/config/ContainersConfig.kt`

  **Acceptance Criteria**:
  - [ ] `compose.yml` exists with `app` and `postgres`.
  - [ ] `docker compose -f compose.yml up -d` exits `0`.
  - [ ] `docker compose -f compose.yml ps` lists both services as running/healthy.

  **QA Scenarios**:
  ```
  Scenario: Happy path compose startup
    Tool: Bash
    Steps: docker compose -f compose.yml up -d; docker compose -f compose.yml ps
    Expected: app + postgres containers up
    Evidence: .sisyphus/evidence/task-2-compose-up.txt

  Scenario: Failure path host port already occupied
    Tool: PowerShell
    Steps: $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, 8080); $listener.Start(); docker compose -f compose.yml up -d; $listener.Stop()
    Expected: Exit code 1; compose fails with port allocation error
    Evidence: .sisyphus/evidence/task-2-compose-port-error.txt
  ```

  **Commit**: YES | Message: `feat(devops): add compose stack with postgres` | Files: `compose.yml`

- [ ] 3. Rewrite README.md fully

  **What to do**:
  - Replace current minimal README with complete operational README.
  - Include: project overview, prerequisites, Docker quick start, local verification commands, CI verification commands, env table, troubleshooting, cleanup.
  - Apply command-source precedence: CI section mirrors `.github/workflows/ci.yml`; local section mirrors `AGENTS.md`.
  - Preserve or improve badge section consistency.

  **Must NOT do**:
  - Do not add undocumented feature claims.
  - Do not reference commands not present in project tooling.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: full technical documentation rewrite.
  - Skills: [`kotlin-springboot`] - ensure framework-accurate instructions.
  - Omitted: [`playwright-cli`] - no browser workflow.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 5 | Blocked By: none

  **References**:
  - Existing README: `README.md`
  - Local verification policy: `AGENTS.md`
  - CI gate order: `.github/workflows/ci.yml`
  - Runtime env keys: `src/main/resources/application.yaml`

  **Acceptance Criteria**:
  - [ ] README includes Docker build/run section.
  - [ ] README includes local verification command sequence.
  - [ ] README includes env var table for DB settings.
  - [ ] README includes troubleshooting + teardown section.

  **QA Scenarios**:
  ```
  Scenario: Happy path command parity review
    Tool: grep
    Steps: Run grep for `./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck|./gradlew detekt|./gradlew test|./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification` in `README.md`
    Expected: All four patterns matched in README
    Evidence: .sisyphus/evidence/task-3-readme-parity.txt

  Scenario: Failure path stale command detection
    Tool: grep
    Steps: Run grep for `docker-compose.yml` in `README.md`
    Expected: Zero matches
    Evidence: .sisyphus/evidence/task-3-readme-command-error.txt
  ```

  **Commit**: YES | Message: `docs(readme): rewrite setup and run instructions` | Files: `README.md`

- [ ] 4. Create docs/run-and-test-guide.md

  **What to do**:
  - Create focused operator guide with explicit sequence:
    1) build image, 2) start stack, 3) inspect logs, 4) run verification, 5) teardown.
  - Include failure handling paths: wrong DB credentials, missing Docker daemon, stale volume reset.
  - Include evidence-oriented command blocks.

  **Must NOT do**:
  - Do not duplicate full README text verbatim.
  - Do not introduce commands absent from repo ecosystem.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: procedural runbook authoring.
  - Skills: [`kotlin-springboot`] - Spring operational accuracy.
  - Omitted: [`clean-ddd-hexagonal`] - not architecture scope.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 5 | Blocked By: none

  **References**:
  - Verification workflow: `AGENTS.md`
  - CI command gates: `.github/workflows/ci.yml`
  - Runtime env keys: `src/main/resources/application.yaml`

  **Acceptance Criteria**:
  - [ ] File exists at `docs/run-and-test-guide.md`.
  - [ ] Contains startup, validation, and teardown sequences.
  - [ ] Contains failure playbook for credential and volume issues.

  **QA Scenarios**:
  ```
  Scenario: Happy path guide execution
    Tool: Bash
    Steps: `docker build -t wex-transactions:test .` → `docker compose -f compose.yml up -d` → `docker compose -f compose.yml ps` → `docker compose -f compose.yml logs app` → `docker compose -f compose.yml logs postgres` → `docker compose -f compose.yml down -v`
    Expected: All commands exit 0; ps shows app/postgres; app logs show startup completion; postgres logs show readiness
    Evidence: .sisyphus/evidence/task-4-guide-happy.txt

  Scenario: Failure path wrong DB credentials via post-password-change restart
    Tool: PowerShell
    Steps: docker compose -f compose.yml up -d; docker compose -f compose.yml exec -T postgres psql -U postgres -c "ALTER USER postgres PASSWORD 'changedpw'"; docker compose -f compose.yml restart app; docker compose -f compose.yml logs app --tail 30; docker compose -f compose.yml down -v
    Expected: App logs contain authentication or connection error after password change; stack stops cleanly on teardown
    Evidence: .sisyphus/evidence/task-4-guide-db-error.txt
  ```

  **Commit**: YES | Message: `docs(runbook): add run and test guide` | Files: `docs/run-and-test-guide.md`

- [ ] 5. Align cross-document consistency

  **What to do**:
  - Reconcile README and run guide to avoid drift.
  - Ensure env variable names/default semantics match `application.yaml`.
  - Ensure compose service names and commands are identical across docs.

  **Must NOT do**:
  - Do not leave conflicting command variants.
  - Do not keep ambiguous path names (`docker-compose.yml` vs `compose.yml`).

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: consistency and documentation QA.
  - Skills: [] - no specialized skill required.
  - Omitted: [`gh-cli`] - repository hosting ops not needed.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 6 | Blocked By: 2,3,4

  **References**:
  - Compose spec file: `compose.yml`
  - Runtime env source: `src/main/resources/application.yaml`
  - Docs targets: `README.md`, `docs/run-and-test-guide.md`

  **Acceptance Criteria**:
  - [ ] All command snippets reference `compose.yml`.
  - [ ] Env table keys match exactly: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
  - [ ] Troubleshooting guidance consistent across both docs.

  **QA Scenarios**:
  ```
  Scenario: Happy path doc diff consistency
    Tool: grep
    Steps: Run grep for `compose.yml|DB_URL|DB_USERNAME|DB_PASSWORD` in both `README.md` and `docs/run-and-test-guide.md`
    Expected: Both files match compose filename and all three env keys
    Evidence: .sisyphus/evidence/task-5-doc-consistency.txt

  Scenario: Failure path inconsistent filename sentinel
    Tool: grep
    Steps: Run grep for `docker-compose.yml` in both `README.md` and `docs/run-and-test-guide.md`
    Expected: Zero matches in both files
    Evidence: .sisyphus/evidence/task-5-doc-consistency-error.txt
  ```

  **Commit**: YES | Message: `docs(consistency): align compose and env instructions` | Files: `README.md`, `docs/run-and-test-guide.md`

- [ ] 6. Execute end-to-end container validation wave

  **What to do**:
  - Run full container lifecycle checks using final artifacts.
  - Collect logs and command outputs as evidence.
  - Validate teardown leaves clean state.

  **Must NOT do**:
  - Do not skip failure-path checks.
  - Do not declare completion without evidence files.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: integrated runtime validation.
  - Skills: [] - direct command execution and evidence capture.
  - Omitted: [`playwright-cli`] - non-UI backend service.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: Final Verification Wave | Blocked By: 1,2,5

  **References**:
  - Runtime stack: `Dockerfile`, `compose.yml`
  - Docs instructions: `README.md`, `docs/run-and-test-guide.md`

  **Acceptance Criteria**:
  - [ ] `docker build -t wex-transactions:test .` succeeds.
  - [ ] `docker compose -f compose.yml up -d` succeeds.
  - [ ] `docker compose -f compose.yml logs postgres` shows DB readiness.
  - [ ] `docker compose -f compose.yml logs app` shows successful Spring boot.
  - [ ] `docker compose -f compose.yml down -v` succeeds.

  **QA Scenarios**:
  ```
  Scenario: Happy path full lifecycle
    Tool: Bash
    Steps: docker build -t wex-transactions:test . && docker compose -f compose.yml up -d && docker compose -f compose.yml ps && docker compose -f compose.yml logs app && docker compose -f compose.yml logs postgres && docker compose -f compose.yml down -v
    Expected: All commands exit 0; `ps` lists both services; logs include DB ready + Spring started indicators
    Evidence: .sisyphus/evidence/task-6-e2e-lifecycle.txt

  Scenario: Failure path stale volume credential drift recovery
    Tool: PowerShell
    Steps: docker compose -f compose.yml up -d; docker compose -f compose.yml exec -T postgres psql -U postgres -c "ALTER USER postgres PASSWORD 'driftingpw'"; docker compose -f compose.yml down; docker compose -f compose.yml up -d; docker compose -f compose.yml logs app --tail 30 | Out-File -FilePath .sisyphus/evidence/task-6-pre-recovery.log; docker compose -f compose.yml down -v; docker compose -f compose.yml up -d; docker compose -f compose.yml logs app --tail 30 | Out-File -FilePath .sisyphus/evidence/task-6-post-recovery.log; docker compose -f compose.yml down -v
    Expected: Pre-recovery logs show authentication failure (password drift persisted in volume); post-recovery logs show successful startup (volume cleaned, fresh DB with original password); final teardown exits 0
    Evidence: .sisyphus/evidence/task-6-e2e-volume-recovery.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> Do NOT auto-proceed after verification.

- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high
- [ ] F4. Scope Fidelity Check — deep

**Final Wave QA Scenarios**:
```
Scenario: F1 — Plan Compliance Audit
  Tool: agent (oracle)
  Steps: Present deliverables (Dockerfile, compose.yml, README.md, docs/run-and-test-guide.md) and plan. Oracle verifies all Definition of Done conditions met, all guardrails respected, no scope creep.
  Expected: Oracle approves all compliance checks; no violations found
  Evidence: .sisyphus/evidence/final-f1-compliance.txt

Scenario: F2 — Code Quality Review
  Tool: agent (unspecified-high)
  Steps: Read Dockerfile, compose.yml, and any new/changed docs. Check for hardcoded secrets, insecure defaults, missing health checks, inconsistent style.
  Expected: No security issues; all artifacts follow project conventions; approval granted
  Evidence: .sisyphus/evidence/final-f2-quality.txt

Scenario: F3 — Real Manual QA
  Tool: agent (unspecified-high)
  Steps: Execute full lifecycle: docker build → compose up → ps → logs → down -v. Verify env table in docs matches application.yaml. Verify CI/local command split.
  Expected: All lifecycle commands exit 0; docs commands match project truth sources
  Evidence: .sisyphus/evidence/final-f3-qa.txt

Scenario: F4 — Scope Fidelity Check
  Tool: agent (deep)
  Steps: Diff workspace against original source—confirm no business logic changes, no Gradle/plugin modifications, no undocumented artifacts added.
  Expected: Only Dockerfile, compose.yml, README.md, docs/run-and-test-guide.md changed or added; zero source-code mutations
  Evidence: .sisyphus/evidence/final-f4-scope.txt
```

## Commit Strategy
- Commit per task where artifacts are created/changed.
- Keep documentation commits separate from infra config commits.
- Do not squash until all verification waves pass.

## Success Criteria
- All four deliverables exist and are consistent.
- Docker and Compose lifecycle validated with evidence.
- Docs are executable, accurate, and aligned with project truth sources.
- Final verification wave approved by all review agents and user "okay" received.
