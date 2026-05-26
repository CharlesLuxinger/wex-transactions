# WEX Tech Challenge — Strict Validation Plan

## TL;DR
> **Summary**: Validate implementation against challenge requirements with clause-level, source-cited, executable checks, including runtime API validation.
> **Deliverables**:
> - Requirement validation matrix (atomic clauses)
> - Executable validation runbook (static + runtime)
> - Evidence bundle with binary pass/fail per clause
> - Conflict/blocker register with strict disposition
> **Effort**: Medium
> **Parallel**: YES - 3 waves
> **Critical Path**: Task 1 → Task 2 → Task 5 → Task 8 → Task 12

## Context
### Original Request
Create a strict plan to validate whether the implemented solution is correct versus tech challenge requirements. Read all markdown, understand code, do not assume, ask when required.

### Interview Summary
- Authority precedence fixed by requester for this validation scope: `SPEC > Business Spec > AGENTS`.
- Unresolved requirement detail policy fixed: **fail by default**.
- Deliverable fixed: full executable validation plan.
- Runtime API validation explicitly required.

### Metis Review (gaps addressed)
Metis required clause-level atomization, deterministic runtime setup, explicit blocker classes (`FAIL`, `BLOCKED`, `UNRESOLVED-CONFLICT`), no inferred requirements, and evidence-first acceptance criteria. This plan includes all. Precedence conflict with repository default source-order is explicitly overridden for this challenge-validation run per user instruction.

## Work Objectives
### Core Objective
Produce a decision-complete validation workflow that determines challenge compliance with zero ambiguity and zero assumption.

### Deliverables
1. Atomic requirement matrix with quote-based traceability.
2. Canonical validation gate profile (local + CI-equivalent).
3. Runtime API validation scenarios (happy + failure paths).
4. Evidence artifacts per requirement clause.
5. Consolidated compliance verdict with blocker classification.

### Definition of Done (verifiable conditions with commands)
- Matrix file exists and is complete for all REQ-01..REQ-25 clauses and business-spec rules.
- Validation runbook commands execute and produce artifacts.
- Every clause has one and only one final status: `PASS` | `FAIL` | `BLOCKED` | `UNRESOLVED-CONFLICT`.
- Final report includes strict verdict and remediation list.

### Must Have
- Source-cited assertions only (file + section/line reference).
- Runtime API checks executed against deterministic local environment.
- Negative path validation for every endpoint/rule family.
- Explicit handling of known ambiguities/conflicts.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No implementation/code changes.
- No assumption-based checks.
- No “looks correct” judgments.
- No hidden criteria outside authoritative sources.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after (execute existing suite + runtime validation); framework: JUnit5, ArchUnit, RestAssured, Testcontainers, Gradle gates.
- QA policy: Every task includes executable happy + failure scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. Shared dependencies extracted in Wave 1.

Wave 1: authority, requirement atomization, environment determinism, command profile.
Wave 2: static architecture/code/spec checks and runtime API checks.
Wave 3: evidence consolidation, strict verdicting, blocker registry, final compliance packet.

### Dependency Matrix (full, all tasks)
- 1 blocks all tasks.
- 2 depends on 1; blocks 5-12.
- 3 depends on 1; blocks 8-10.
- 4 depends on 1; blocks 6-7.
- 5 depends on 2.
- 6 depends on 2,4.
- 7 depends on 2,4.
- 8 depends on 2,3,6,7.
- 9 depends on 2,3,8.
- 10 depends on 2,3,8.
- 11 depends on 5,6,7,8,9,10.
- 12 depends on 11.

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 4 tasks → writing, unspecified-high
- Wave 2 → 6 tasks → unspecified-high, deep
- Wave 3 → 2 tasks → writing, deep

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Build authority and conflict protocol

  **What to do**: Create validation authority section used by all subsequent checks: `SPEC > Business Spec > AGENTS`; explicitly document this as a user-directed override for challenge validation (superseding repository generic source-order for this plan only); define conflict statuses (`UNRESOLVED-CONFLICT`, `BLOCKED`) and fail-default rule for unresolved requirement detail.
  **Must NOT do**: Resolve conflicts by preference or inferred implementation behavior.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: deterministic policy artifact.
  - Skills: `[]` - policy derived from repository docs.
  - Omitted: `context7` - not library/API documentation task.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [2,3,4,5,6,7,8,9,10,11,12] | Blocked By: []

  **References**:
  - Pattern: `.docs/SPEC.md` - authoritative REQ set and decision tokens.
  - Pattern: `.docs/wex-transactions-business-spec.md` - business-rule acceptance.
  - Pattern: `AGENTS.md` - architecture/test gate constraints.

  **Acceptance Criteria**:
  - [ ] Authority protocol artifact generated at `.sisyphus/evidence/task-1-authority-protocol.md` and used by matrix generation.
  - [ ] User-directed precedence override rationale is documented with source citations.
  - [ ] Conflict status taxonomy appears in final rubric.

  **QA Scenarios**:
  ```
  Scenario: Protocol completeness
    Tool: Bash
    Steps: Validate protocol file contains authority order + fail-default + status taxonomy.
    Expected: All required policy terms present once; no contradictions.
    Evidence: .sisyphus/evidence/task-1-authority-protocol.md

  Scenario: Conflict handling strictness
    Tool: Bash
    Steps: Inject one known ambiguity row in dry-run matrix.
    Expected: Row status resolves to UNRESOLVED-CONFLICT (not PASS).
    Evidence: .sisyphus/evidence/task-1-authority-protocol-error.md
  ```

  **Commit**: NO | Message: `docs(validation): define authority protocol` | Files: `.sisyphus/evidence/*`

- [ ] 2. Atomize requirement matrix (clause level)

  **What to do**: Build one row per requirement clause from REQ-01..REQ-25 plus business-spec rules (description, date, amount, currency source, 6-month window, rounding, errors). Add columns: `ReqId`, `ClauseId`, `Source`, `Quote`, `ValidationMethod`, `Command`, `Expected`, `EvidencePath`, `Status`.
  **Must NOT do**: Merge multiple clauses into one row or create rows without direct quote.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: structured traceability artifact.
  - Skills: `[]` - source is internal markdown.
  - Omitted: `playwright-cli` - not needed for matrix creation.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [5,6,7,8,9,10,11,12] | Blocked By: [1]

  **References**:
  - Pattern: `.docs/SPEC.md` - REQ IDs and immutable decisions.
  - Pattern: `.docs/wex-transactions-business-spec.md` - acceptance criteria and business rules.
  - Pattern: `.specs/features/wex-tech-challenge/context.md` - deferred/unknown items.

  **Acceptance Criteria**:
  - [ ] Canonical matrix artifact generated at `.sisyphus/evidence/task-2-requirement-matrix.md`.
  - [ ] Every clause has unique `ClauseId`.
  - [ ] 100% rows include exact source quote.
  - [ ] 0 rows with empty command/expected/evidence.

  **QA Scenarios**:
  ```
  Scenario: Matrix integrity
    Tool: Bash
    Steps: Count non-empty ClauseId and compare with total rows.
    Expected: Counts match; duplicates = 0.
    Evidence: .sisyphus/evidence/task-2-matrix-integrity.txt

  Scenario: Quote traceability failure
    Tool: Bash
    Steps: Run checker for missing Source/Quote columns.
    Expected: Any missing citation triggers FAIL row.
    Evidence: .sisyphus/evidence/task-2-matrix-integrity-error.txt
  ```

  **Commit**: NO | Message: `docs(validation): create atomic requirement matrix` | Files: `.sisyphus/evidence/*`

- [ ] 3. Define deterministic runtime environment

  **What to do**: Pin runtime validation setup: Docker compose lifecycle, env vars, health/readiness checks, deterministic exchange-rate seed step, and teardown. Seed at least one valid historical rate row used by retrieve-converted happy path. Declare nondeterministic checks invalid unless deterministic setup exists.
  **Must NOT do**: Use ad-hoc local state or stale containers.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: environment reproducibility and runtime gate correctness.
  - Skills: `[]` - repository runbooks sufficient.
  - Omitted: `context7` - no external library ambiguity.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [8,9,10] | Blocked By: [1]

  **References**:
  - Pattern: `README.md` - docker flow and env defaults.
  - Pattern: `.docs/run-and-test-guide.md` - startup/verification sequence.

  **Acceptance Criteria**:
  - [ ] Single authoritative startup and teardown command set documented.
  - [ ] Readiness gate defined before API tests.
  - [ ] Deterministic SQL seed step for `exchange_rates` documented with exact command and sample row.

  **QA Scenarios**:
  ```
  Scenario: Deterministic startup
    Tool: Bash
    Steps: Run docker build/up/ps/health checks exactly as runbook; seed using `docker compose exec -T postgres psql -U postgres -d wex_transactions -c "INSERT INTO exchange_rates(rate_date,source_currency,target_currency,exchange_rate,created_at) VALUES ('2026-01-15','United-States-Dollar','BRL',5.100000,'2026-01-15T12:00:00Z') ON CONFLICT (rate_date,source_currency,target_currency) DO NOTHING;"`; verify using `docker compose exec -T postgres psql -U postgres -d wex_transactions -c "SELECT rate_date,source_currency,target_currency,exchange_rate,created_at FROM exchange_rates WHERE source_currency='United-States-Dollar' AND target_currency='BRL' AND rate_date='2026-01-15';"`.
    Expected: App and DB healthy before API execution; seed row exists and is queryable.
    Evidence: .sisyphus/evidence/task-3-runtime-setup.txt

  Scenario: Nondeterministic state guard
    Tool: Bash
    Steps: Re-run without teardown; detect stale state markers.
    Expected: Validation marked BLOCKED until cleanup performed.
    Evidence: .sisyphus/evidence/task-3-runtime-setup-error.txt
  ```

  **Commit**: NO | Message: `docs(validation): define deterministic runtime setup` | Files: `.sisyphus/evidence/*`

- [ ] 4. Define canonical static gate command profile

  **What to do**: Freeze command sequence used for strict validation and include documented conflict note for coverage verification mismatch (README vs AGENTS). Use stricter superset for challenge validation.
  **Must NOT do**: Treat partial command runs as equivalent.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: CI/local gate reconciliation.
  - Skills: `[]` - internal command evidence only.
  - Omitted: `oracle` - not architecture design decision.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [6,7] | Blocked By: [1]

  **References**:
  - Pattern: `AGENTS.md` - local and CI ordered workflow.
  - Pattern: `README.md` - includes jacoco coverage verification in CI section.
  - Pattern: `.github/workflows/ci.yml` - authoritative CI pipeline commands.

  **Acceptance Criteria**:
  - [ ] Validation profile includes ktlint check, detekt, test, jacoco report, jacoco verification.
  - [ ] Conflict rationale documented with authority protocol.

  **QA Scenarios**:
  ```
  Scenario: Full static profile execution
    Tool: Bash
    Steps: Execute canonical command sequence in order.
    Expected: Any command failure marks related clauses FAIL.
    Evidence: .sisyphus/evidence/task-4-static-profile.txt

  Scenario: Partial run rejection
    Tool: Bash
    Steps: Omit one gate and run validator.
    Expected: Validator rejects run as incomplete (BLOCKED).
    Evidence: .sisyphus/evidence/task-4-static-profile-error.txt
  ```

  **Commit**: NO | Message: `docs(validation): define static gate profile` | Files: `.sisyphus/evidence/*`

- [ ] 5. Validate architecture and layering compliance

  **What to do**: Verify hexagonal/DDD package and dependency rules using architecture tests and direct source-path checks: controller boundaries, use-case ownership, dependency direction.
  **Must NOT do**: Infer compliance from naming only.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: strict boundary and dependency verification.
  - Skills: [`clean-ddd-hexagonal`] - enforce architecture constraints.
  - Omitted: `playwright-cli` - not runtime HTTP behavior.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [11] | Blocked By: [2]

  **References**:
  - Test: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/DependencyDirectionTest.kt`
  - Test: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/ControllerBoundaryTest.kt`
  - Test: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/UseCaseOwnershipTest.kt`
  - Pattern: `AGENTS.md` package details table.

  **Acceptance Criteria**:
  - [ ] All architecture tests pass.
  - [ ] No controller directly accesses repository/adapter.

  **QA Scenarios**:
  ```
  Scenario: Architecture happy path
    Tool: Bash
    Steps: Run ./gradlew test --tests "com.charlesluxinger.wex_transactions.architecture.*"
    Expected: 100% pass for architecture suite.
    Evidence: .sisyphus/evidence/task-5-architecture-tests.txt

  Scenario: Boundary violation detection
    Tool: Bash
    Steps: Run focused test checks and inspect failures if any.
    Expected: Any boundary failure maps to impacted requirement clauses as FAIL.
    Evidence: .sisyphus/evidence/task-5-architecture-tests-error.txt
  ```

  **Commit**: NO | Message: `docs(validation): architecture compliance evidence` | Files: `.sisyphus/evidence/*`

- [ ] 6. Validate domain and business-rule static compliance

  **What to do**: Map domain invariants and use-case logic to business rules: required fields, amount positivity/scale, date validity, currency validity, rounding, unique ID semantics, rate window rule handling.
  **Must NOT do**: Accept behavior not directly mapped to requirement clause.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: detailed source-to-requirement mapping.
  - Skills: [`kotlin-patterns`] - consistent Kotlin interpretation.
  - Omitted: `context7` - no external API semantics required.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [11] | Blocked By: [2,4]

  **References**:
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/Purchase.kt`
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt`
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/TransactionDate.kt`
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/purchase/StorePurchaseUseCaseImpl.kt`
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`

  **Acceptance Criteria**:
  - [ ] Each business rule clause mapped to concrete code location(s).
  - [ ] Any mismatch marked FAIL with clause linkage.

  **QA Scenarios**:
  ```
  Scenario: Rule mapping completeness
    Tool: Bash
    Steps: Validate every business clause has >=1 code reference and >=1 test/evidence command.
    Expected: 0 orphan clauses.
    Evidence: .sisyphus/evidence/task-6-business-rule-map.md

  Scenario: Rule mismatch handling
    Tool: Bash
    Steps: Detect clause with contradictory code path.
    Expected: Clause status FAIL, with mismatch note and source links.
    Evidence: .sisyphus/evidence/task-6-business-rule-map-error.md
  ```

  **Commit**: NO | Message: `docs(validation): business rule static compliance` | Files: `.sisyphus/evidence/*`

- [ ] 7. Validate error taxonomy and contract compliance

  **What to do**: Verify error behavior for invalid input, not found, stale/unavailable rate, and validation problems. Check alignment with Problem Details and requirement clauses.
  **Must NOT do**: Accept generic 500/opaque errors as compliant.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: strict error-contract verification.
  - Skills: `[]` - internal contract definitions available.
  - Omitted: `oracle` - no design alternatives needed.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [11] | Blocked By: [2,4]

  **References**:
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client/error/GlobalExceptionHandler.kt`
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/DomainException.kt`
  - Spec: `.docs/SPEC.md` REQ-11..REQ-14

  **Acceptance Criteria**:
  - [ ] Error clauses mapped to explicit status/body expectations.
  - [ ] Missing/incorrect mapping produces FAIL.

  **QA Scenarios**:
  ```
  Scenario: Error contract mapping
    Tool: Bash
    Steps: Build table of exception -> HTTP status -> response fields from source and tests.
    Expected: Table complete for all error requirements.
    Evidence: .sisyphus/evidence/task-7-error-taxonomy.md

  Scenario: Missing requirement handling
    Tool: Bash
    Steps: Detect requirement with no explicit exception path.
    Expected: Clause marked FAIL or UNRESOLVED-CONFLICT per authority rules.
    Evidence: .sisyphus/evidence/task-7-error-taxonomy-error.md
  ```

  **Commit**: NO | Message: `docs(validation): error taxonomy compliance` | Files: `.sisyphus/evidence/*`

- [ ] 8. Execute runtime API happy-path validation

  **What to do**: Run end-to-end API validation split into two strict checks: (A) POST purchase validates contract + persistence only, without asserting treasury correctness due to dummy adapter; (B) GET retrieve-converted validates conversion correctness against deterministic seeded `exchange_rates` dataset from Task 3.
  **Must NOT do**: Use undocumented endpoints/status assumptions.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: deterministic runtime execution and evidence capture.
  - Skills: [`playwright-cli`] - optional for API evidence orchestration if needed.
  - Omitted: `clean-ddd-hexagonal` - runtime behavior focus.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [9,10,11] | Blocked By: [2,3,6,7]

  **References**:
  - Guide: `.docs/run-and-test-guide.md` (runtime steps)
  - Controller: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client/purchase/PurchaseControllerV1.kt`
  - Controller: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1.kt`

  **Acceptance Criteria**:
  - [ ] Happy-path API calls executed with captured request/response evidence.
  - [ ] POST assertions limited to request/response contract and persistence clauses.
  - [ ] GET assertions enforce seeded-rate conversion clauses.
  - [ ] Clause-level pass/fail updated based on runtime results.

  **QA Scenarios**:
  ```
  Scenario: Purchase then retrieve converted
    Tool: Bash
    Steps: Start services; POST purchase payload; GET converted by returned id; capture full HTTP transcripts.
    Expected: Responses satisfy matrix expectations for required fields and value rules.
    Evidence: .sisyphus/evidence/task-8-runtime-happy.http

  Scenario: Runtime startup failure
    Tool: Bash
    Steps: Execute API flow with app unavailable.
    Expected: Validation marks affected clauses BLOCKED with startup evidence.
    Evidence: .sisyphus/evidence/task-8-runtime-happy-error.txt
  ```

  **Commit**: NO | Message: `docs(validation): runtime happy-path evidence` | Files: `.sisyphus/evidence/*`

- [ ] 9. Execute runtime API negative-path validation

  **What to do**: Run invalid input and domain-failure scenarios (invalid currency/date/amount, missing purchase, stale/unavailable rates where applicable) and verify strict error clauses.
  **Must NOT do**: Reuse happy-path assertions for negative tests.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: error-path strictness validation.
  - Skills: `[]` - internal requirement matrix drives cases.
  - Omitted: `kotlin-patterns` - execution-focused task.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [11] | Blocked By: [2,3,8]

  **References**:
  - Spec: `.docs/SPEC.md` error requirement IDs
  - Business: `.docs/wex-transactions-business-spec.md` invalid-rule conditions

  **Acceptance Criteria**:
  - [ ] Every negative clause executed at least once.
  - [ ] Response status/body matches mapped contract or explicit conflict status.

  **QA Scenarios**:
  ```
  Scenario: Invalid payload rules
    Tool: Bash
    Steps: Send malformed date, invalid currency, zero/negative amount.
    Expected: Deterministic error responses mapped to matrix clauses.
    Evidence: .sisyphus/evidence/task-9-runtime-negative.http

  Scenario: Missing purchase retrieval
    Tool: Bash
    Steps: GET converted with non-existent id.
    Expected: Not-found clause PASS only if status/body exactly match mapped requirement.
    Evidence: .sisyphus/evidence/task-9-runtime-negative-error.http
  ```

  **Commit**: NO | Message: `docs(validation): runtime negative-path evidence` | Files: `.sisyphus/evidence/*`

- [ ] 10. Execute edge-case runtime validation (date/rate/rounding)

  **What to do**: Validate edge cases around 6-month window boundary, rounding precision, and rate timestamp semantics; classify unresolved arithmetic details as `UNRESOLVED-CONFLICT` unless authoritative text is explicit.
  **Must NOT do**: Invent date arithmetic policy.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: nuanced temporal and numeric correctness checks.
  - Skills: `[]` - governed by internal specs.
  - Omitted: `context7` - no external framework dependency.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [11] | Blocked By: [2,3,8]

  **References**:
  - Business: `.docs/wex-transactions-business-spec.md` 6-month and rounding rules
  - Domain: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt`
  - Use case: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`

  **Acceptance Criteria**:
  - [ ] Boundary cases produce deterministic status classification.
  - [ ] Any unresolved arithmetic interpretation is not marked PASS.

  **QA Scenarios**:
  ```
  Scenario: Boundary-inclusive six-month window
    Tool: Bash
    Steps: Execute cases exactly on boundary and just outside boundary.
    Expected: Boundary treatment matches explicit source; otherwise UNRESOLVED-CONFLICT.
    Evidence: .sisyphus/evidence/task-10-edge-window.md

  Scenario: Rounding precision mismatch
    Tool: Bash
    Steps: Trigger conversion values requiring rounding at 2 decimals.
    Expected: Non-2-decimal or wrong rounding -> FAIL.
    Evidence: .sisyphus/evidence/task-10-edge-rounding-error.md
  ```

  **Commit**: NO | Message: `docs(validation): runtime edge-case evidence` | Files: `.sisyphus/evidence/*`

- [ ] 11. Build strict compliance verdict ledger

  **What to do**: Aggregate all evidence into clause-level ledger with final status and rationale; include hotspots (dummy adapter, placeholder ID, rate provenance mismatch, timestamp reconstruction) as explicit checks tied to requirements.
  **Must NOT do**: Produce aggregate PASS with unresolved clause statuses.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: high-precision reporting and classification.
  - Skills: `[]` - all data is internal evidence.
  - Omitted: `playwright-cli` - no browser interaction needed.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: [12] | Blocked By: [5,6,7,8,9,10]

  **References**:
  - Evidence: `.sisyphus/evidence/task-*`
  - Matrix: requirement clause artifact from Task 2
  - Spec: `.docs/SPEC.md`, `.docs/wex-transactions-business-spec.md`

  **Acceptance Criteria**:
  - [ ] Canonical verdict ledger artifact generated at `.sisyphus/evidence/task-11-verdict-ledger.md`.
  - [ ] Every clause has final status and evidence link.
  - [ ] No clause left unclassified.

  **QA Scenarios**:
  ```
  Scenario: Ledger completeness
    Tool: Bash
    Steps: Validate row count equals clause count from Task 2.
    Expected: Exact match, no missing final status.
    Evidence: .sisyphus/evidence/task-11-verdict-ledger.txt

  Scenario: Illegal pass detection
    Tool: Bash
    Steps: Scan for PASS rows missing evidence path.
    Expected: Zero such rows; otherwise FAIL build of ledger.
    Evidence: .sisyphus/evidence/task-11-verdict-ledger-error.txt
  ```

  **Commit**: NO | Message: `docs(validation): strict clause verdict ledger` | Files: `.sisyphus/evidence/*`

- [ ] 12. Publish final validation packet

  **What to do**: Produce final packet with summary verdict, requirement-level outcomes, blocker list, unresolved conflicts, and prioritized remediation backlog.
  **Must NOT do**: Hide blocker impact or downgrade fail-default outcomes.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: final communication artifact.
  - Skills: `[]` - synthesizes existing evidence.
  - Omitted: `oracle` - decision already deterministic.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: [] | Blocked By: [11]

  **References**:
  - Ledger: `.sisyphus/evidence/task-11-verdict-ledger.md`
  - Matrix: `.sisyphus/evidence/task-2-requirement-matrix.md`
  - Authority protocol: `.sisyphus/evidence/task-1-authority-protocol.md`

  **Acceptance Criteria**:
  - [ ] Final packet includes binary overall verdict and rationale.
  - [ ] Remediation backlog maps each item to failed clauses.

  **QA Scenarios**:
  ```
  Scenario: Final packet traceability
    Tool: Bash
    Steps: Verify each remediation item references at least one failed/blocker clause.
    Expected: 100% linked; no orphan remediation items.
    Evidence: .sisyphus/evidence/task-12-final-packet.md

  Scenario: Verdict consistency check
    Tool: Bash
    Steps: Recompute overall verdict from clause statuses.
    Expected: Published verdict equals computed verdict.
    Evidence: .sisyphus/evidence/task-12-final-packet-error.md
  ```

  **Commit**: NO | Message: `docs(validation): final challenge compliance packet` | Files: `.sisyphus/evidence/*`

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
  - Tool: task (subagent_type="oracle")
  - Steps: Invoke oracle with prompt pointing to matrix, ledger, and evidence folder; require checklist output for tasks 1-12 and clause coverage.
  - Expected: No missing task, no missing evidence, no rule drift.
  - Evidence: `.sisyphus/evidence/f1-plan-compliance.md`
- [ ] F2. Code Quality Review — unspecified-high
  - Tool: Bash
  - Steps: Re-run canonical static gate profile; map any failures to clause statuses.
  - Expected: Gate outcomes exactly reflected in verdict ledger.
  - Evidence: `.sisyphus/evidence/f2-code-quality.txt`
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
  - Tool: Bash
  - Steps: Re-run runtime happy + negative API scripts with seeded data from Task 3.
  - Expected: Runtime results match ledger statuses; no undocumented deviations.
  - Evidence: `.sisyphus/evidence/f3-runtime-qa.http`
- [ ] F4. Scope Fidelity Check — deep
  - Tool: task (category="deep")
  - Steps: Invoke deep agent to inspect changed files and output scope-conformance report confirming only validation artifacts changed.
  - Expected: Scope fidelity PASS; any scope breach recorded as blocker.
  - Evidence: `.sisyphus/evidence/f4-scope-fidelity.md`

## Commit Strategy
- No code commits.
- Optional docs-only commit for validation artifacts if requested.

## Success Criteria
- 100% authoritative requirement clauses evaluated.
- 100% clauses have evidence path.
- 0 unresolved assumptions.
- Compliance verdict is reproducible by rerunning the runbook.
