# WEX Tech Challenge — Implementation Tasks Plan

## TL;DR
> **Summary**: Execute the challenge through vertical feature slices with explicit quality/architecture gates and evidence capture.
> **Deliverables**:
> - Decision-closed implementation task backlog
> - Wave-based execution order with dependencies
> - Task-level acceptance criteria and QA scenarios
> - Coverage-verification activation tasks (local + CI)
> **Effort**: Large
> **Parallel**: YES - 4 waves
> **Critical Path**: Task 1 → Task 2 → Task 3/4 → Task 5/6 → Task 7/8 → Task 9 → Task 10

## Context
### Original Request
Break `.docs/wex.md` and `.docs/SPEC.md` requirements plus `.specs/features/ROADMAP.md` into implementation tasks. Focus only on planning task decomposition.

### Interview Summary
- Task slicing model: **Vertical slices per feature flow**.
- Coverage policy: **Add explicit tasks to activate `jacocoTestCoverageVerification` in local + CI**.
- Output shape: **Feature-oriented vertical breakdown**.

### Metis Review (gaps addressed)
- Added upfront decision-closure task for unresolved API/format/rounding specifics (excluding date acceptance, now fixed as ISO-8601 variants normalized to canonical output).
- Isolated coverage activation as dedicated tasks, not hidden in feature tasks.
- Added architecture hardening task to avoid vacuous architecture test passes as packages fill.
- Added explicit boundary scenarios for conversion-date lookup and failure paths.

## Work Objectives
### Core Objective
Produce a decision-complete implementation execution plan that an agent can execute with zero judgment calls.

### Deliverables
- Ordered TODO list with dependencies and parallel waves
- Task-level acceptance criteria and executable QA scenarios
- Required references to current repo constraints and specs
- Final verification wave definition

### Definition of Done (verifiable conditions with commands)
- Plan file contains complete TODOs with dependencies and QA scenarios.
- All tasks reference source constraints/docs and include evidence output paths.
- Verification commands are executable and align with CI/local gates:
  - `./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck`
  - `./gradlew --no-daemon detekt`
  - `./gradlew --no-daemon test jacocoTestReport`
  - `./gradlew --no-daemon test jacocoTestCoverageVerification`

### Must Have
- Vertical slices by feature flow
- Hexagonal + DDD boundary adherence
- Explicit failure-path scenarios
- Coverage-verification activation tasks

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No scope expansion beyond agreed challenge requirements
- No invented API contract details without explicit decision markers
- No vague acceptance criteria
- No manual-only verification steps

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after (existing JUnit5/Spring/ArchUnit), with API/integration enablement tasks included
- QA policy: Every task includes happy-path + failure/edge scenario
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Wave sizes are dependency-driven for this challenge scope.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

- **Wave 1 (Foundation + decision closure)**: Tasks 1-2
- **Wave 2 (Feature A slice + API test harness)**: Tasks 3-4
- **Wave 3 (Feature B slice + error contract)**: Tasks 5-6
- **Wave 4 (Quality hardening + coverage activation + traceability)**: Tasks 7-10

### Dependency Matrix (full, all tasks)
- Task 1 blocks all business-slice tasks.
- Task 2 blocks tasks requiring DB migration/repository persistence.
- Task 3 blocks Task 5 (retrieval depends on stored purchase).
- Task 4 supports validation of Tasks 3,5,6.
- Task 5 and Task 6 can run in parallel after Task 3 and Task 1.
- Task 7 depends on concrete package/classes created by Tasks 3/5/6.
- Task 8 depends on current Gradle + CI workflow state.
- Task 9 depends on completion evidence from Tasks 3-8.
- Task 10 depends on all prior tasks.

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 2 tasks → deep, unspecified-high
- Wave 2 → 2 tasks → unspecified-high, writing
- Wave 3 → 2 tasks → unspecified-high
- Wave 4 → 4 tasks → unspecified-high, deep, writing

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.
> Status legend: parent task `[x]` means planning/decomposition completed; acceptance checkboxes are executed during implementation.

- [x] 1. Contract Decision Closure Slice (non-date items only)

  **What to do**: Lock unresolved contract decisions before implementation: Problem Details field schema, rounding mode for converted amount, treasury lookup date-normalization rule, and exchange-rate precision/storage policy. Date acceptance is fixed: accept ISO-8601 equivalent inputs and normalize to canonical output format `yyyy-MM-dd'T'HH:mm:ssXXX`.
  **Must NOT do**: Do not start coding controllers/services/adapters in this task.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: decision-risk removal across all downstream tasks.
  - Skills: `[]` - internal docs/repo constraints are sufficient.
  - Omitted: `[context7]` - no external API dependency decision required yet.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [2,3,4,5,6,7,8,9,10] | Blocked By: []

  **References** (executor has NO interview context - be exhaustive):
  - Requirement source: `.docs/wex.md`
  - Requirement source: `.docs/SPEC.md`
  - Milestones/evidence pattern: `.specs/features/ROADMAP.md`
  - Architecture constraints: `AGENTS.md`
  - Planning context: `.specs/features/wex-tech-challenge/context.md`

  **Acceptance Criteria** (agent-executable only):
  - [ ] A decision record is added to plan execution artifacts covering five policy sections (error schema, rounding mode, lookup normalization, fixed date normalization policy, precision/storage policy).
  - [ ] No `[DECISION NEEDED]` placeholder remains for these five policy sections after this task.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Decision completeness check
    Tool: Bash
    Steps: Validate decision record file contains four required sections plus a fixed-date-policy section using grep checks.
    Expected: 5/5 required policy sections present; no unresolved markers.
    Evidence: .sisyphus/evidence/task-1-contract-decision-closure.txt

  Scenario: Ambiguity regression check
    Tool: Bash
    Steps: Search task files for "TBD", "decide later", "to be defined".
    Expected: No matches in active task backlog.
    Evidence: .sisyphus/evidence/task-1-contract-decision-closure-error.txt
  ```

  **Commit**: YES | Message: `chore(challenge): close contract decisions` | Files: [task artifacts only]

- [x] 2. Foundation Enablement Slice (DB + migrations + baseline wiring)

  **What to do**: Set PostgreSQL/Flyway baseline, package scaffolding aligned with hexagonal conventions, and required configuration structure.
  **Must NOT do**: Do not implement business rules in this slice.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: multi-file setup with strict conventions.
  - Skills: [`clean-ddd-hexagonal`] - enforce package and boundary rules.
  - Omitted: [`kotlin-coroutines-flows`] - no coroutine design requirement here.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [3,5,7] | Blocked By: [1]

  **References**:
  - Structure and layering: `.specs/codebase/STRUCTURE.md`
  - Conventions: `.specs/codebase/CONVENTIONS.md`
  - Stack baseline: `.specs/codebase/STACK.md`
  - Architecture guardrails: `AGENTS.md`

  **Acceptance Criteria**:
  - [ ] Task list includes explicit Flyway + PostgreSQL setup tasks with clear order.
  - [ ] All planned package paths follow inbound/outbound/application/infra conventions.

  **QA Scenarios**:
  ```
  Scenario: Package convention validation
    Tool: Bash
    Steps: Run path/name checks against planned file map.
    Expected: Controllers, ports, adapters, use-case impl naming all conform.
    Evidence: .sisyphus/evidence/task-2-foundation-enablement.txt

  Scenario: Boundary violation guard
    Tool: Bash
    Steps: Assert no planned controller step references repository adapter access.
    Expected: Zero violations.
    Evidence: .sisyphus/evidence/task-2-foundation-enablement-error.txt
  ```

  **Commit**: YES | Message: `chore(platform): prepare persistence foundation slice` | Files: [task artifacts only]

- [x] 3. Vertical Slice A — Store Purchase Transaction

  **What to do**: Implement end-to-end flow: request validation, command model mapping, use case orchestration, repository persistence, response with generated `Long` id.
  **Must NOT do**: Do not include conversion logic from retrieval feature.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: complete feature slice with domain/application/infra interactions.
  - Skills: [`clean-ddd-hexagonal`] - preserve boundaries.
  - Omitted: [`playwright-cli`] - backend API flow only.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [5,6,9] | Blocked By: [1,2]

  **References**:
  - Feature requirement A: `.docs/SPEC.md`
  - Challenge requirement: `.docs/wex.md`
  - Roadmap milestone mapping: `.specs/features/ROADMAP.md`
  - Architecture tests baseline: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/`

  **Acceptance Criteria**:
  - [ ] Planned tasks cover success path: valid request persists and returns generated id.
  - [ ] Planned tasks cover failure paths: description length > 50, zero/negative amount, invalid date according to chosen contract.
  - [ ] Planned tasks explicitly map controller → inbound port → use case impl → outbound port → adapter.

  **QA Scenarios**:
  ```
  Scenario: Happy path store purchase
    Tool: Bash
    Steps: Execute API test for valid payload; assert HTTP status and response includes generated Long id.
    Expected: Success response with id > 0 and persisted payload fields.
    Evidence: .sisyphus/evidence/task-3-store-purchase.txt

  Scenario: Validation rejection path
    Tool: Bash
    Steps: Execute API tests for invalid description/amount/date payloads.
    Expected: Problem Details responses with expected status/code fields.
    Evidence: .sisyphus/evidence/task-3-store-purchase-error.txt
  ```

  **Commit**: YES | Message: `feat(purchase): implement store transaction slice` | Files: [task artifacts only]

- [x] 4. API Verification Harness Slice (RestAssured + integration scaffolding)

  **What to do**: Enable API/integration verification infrastructure required by slices A/B/C, including explicit dependency additions for RestAssured and Testcontainers plus integration-test setup consistent with project testing policy.
  **Must NOT do**: Do not duplicate business assertions from feature slices.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: test-infra enablement with repo conventions.
  - Skills: [`tdd`] - formalizes executable verification progression.
  - Omitted: [`kotlin-specialist`] - no advanced language pattern need.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [5,6,9] | Blocked By: [1]

  **References**:
  - Testing policy: `.specs/codebase/TESTING.md`
  - Declared dependencies: `build.gradle.kts`
  - CI verification: `.github/workflows/ci.yml`

  **Acceptance Criteria**:
  - [ ] Planned tasks include explicit dependency additions/validation for RestAssured and Testcontainers plus API test wiring.
  - [ ] Planned tasks include naming/location conventions for API and integration tests.

  **QA Scenarios**:
  ```
  Scenario: Test harness bootstrap check
    Tool: Bash
    Steps: Run `./gradlew --no-daemon test --tests "*Api*"` after harness setup task execution.
    Expected: Test engine discovers and runs API-focused tests without missing dependency errors.
    Evidence: .sisyphus/evidence/task-4-api-harness.txt

  Scenario: Missing dependency failure check
    Tool: Bash
    Steps: Simulate harness command before dependency setup in dry-run sequence.
    Expected: Predictable failure that is eliminated post-setup.
    Evidence: .sisyphus/evidence/task-4-api-harness-error.txt
  ```

  **Commit**: YES | Message: `test(api): enable verification harness slice` | Files: [task artifacts only]

- [x] 5. Vertical Slice B — Retrieve Converted Purchase

  **What to do**: Implement retrieval by id, treasury exchange-rate lookup, nearest-prior fallback (<= 6 months), conversion calculation, and response projection.
  **Must NOT do**: Do not redesign storage contract from slice A.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: feature orchestration with external dependency and edge logic.
  - Skills: [`clean-ddd-hexagonal`] - preserves port/adapter separation.
  - Omitted: [`context7`] - behavior defined by challenge docs, not third-party API SDK.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [9] | Blocked By: [1,3,4]

  **References**:
  - Feature requirement B: `.docs/SPEC.md`
  - Challenge detail: `.docs/wex.md`
  - Roadmap conversion checks: `.specs/features/ROADMAP.md`

  **Acceptance Criteria**:
  - [ ] Planned tasks include lookup order: exact date, else nearest prior, bounded by 6 months.
  - [ ] Planned tasks include fields: `id`, `description`, `transactionDate`, `originalUsdAmount`, `exchangeRateUsed`, `convertedAmount`.
  - [ ] Planned tasks include no-rate-within-window failure path.

  **QA Scenarios**:
  ```
  Scenario: Conversion happy path
    Tool: Bash
    Steps: Execute retrieval test with exact-date rate and assert converted amount and exchangeRateUsed.
    Expected: Correct projection and successful response.
    Evidence: .sisyphus/evidence/task-5-retrieve-converted.txt

  Scenario: Boundary + failure path
    Tool: Bash
    Steps: Execute tests for exactly-6-month fallback accepted and 6-month+1-day rejected.
    Expected: Accepted boundary succeeds; out-of-window returns Problem Details.
    Evidence: .sisyphus/evidence/task-5-retrieve-converted-error.txt
  ```

  **Commit**: YES | Message: `feat(conversion): implement retrieval conversion slice` | Files: [task artifacts only]

- [x] 6. Vertical Slice C — Unified Error Contract (Problem Details)

  **What to do**: Implement a single, consistent error envelope across validation errors, not-found/id errors, treasury-window errors, and upstream integration failures.
  **Must NOT do**: Do not introduce an oversized error taxonomy beyond challenge scope.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: cross-cutting behavior touching all feature slices.
  - Skills: [`clean-ddd-hexagonal`] - keep exception translation at proper boundaries.
  - Omitted: [`interface-design`] - non-UI backend concern.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [9] | Blocked By: [1,3,5]

  **References**:
  - Error handling requirements: `.docs/SPEC.md`
  - Challenge behavior expectations: `.docs/wex.md`
  - Roadmap quality expectations: `.specs/features/ROADMAP.md`

  **Acceptance Criteria**:
  - [ ] Planned tasks define one error response schema for all expected failures.
  - [ ] Planned tasks map each failure case to deterministic status and payload fields.

  **QA Scenarios**:
  ```
  Scenario: Consistent Problem Details shape
    Tool: Bash
    Steps: Trigger distinct error categories via API tests and collect responses.
    Expected: Same schema fields present across all failure responses.
    Evidence: .sisyphus/evidence/task-6-error-contract.txt

  Scenario: Upstream failure translation
    Tool: Bash
    Steps: Simulate treasury provider failure/timeout in integration tests.
    Expected: Graceful mapped Problem Details response, no raw stack traces.
    Evidence: .sisyphus/evidence/task-6-error-contract-error.txt
  ```

  **Commit**: YES | Message: `feat(errors): unify problem-details contract` | Files: [task artifacts only]

- [x] 7. Architecture Hardening Slice (de-vacify constraints)

  **What to do**: Ensure architecture tests become non-vacuous with real classes in scoped packages, and enforce dependency direction/controller/use-case ownership continuously.
  **Must NOT do**: Do not weaken architecture rules for convenience.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: structural integrity and long-term maintainability.
  - Skills: [`clean-ddd-hexagonal`] - architecture rule fidelity.
  - Omitted: [`tdd`] - this is architecture-compliance hardening.

  **Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [10] | Blocked By: [3,5,6]

  **References**:
  - Existing architecture tests: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/`
  - Architecture rules: `AGENTS.md`
  - Architecture docs: `.specs/codebase/ARCHITECTURE.md`

  **Acceptance Criteria**:
  - [ ] Planned tasks include actions removing vacuous-pass conditions once target packages are populated.
  - [ ] Planned tasks include focused architecture verification commands.

  **QA Scenarios**:
  ```
  Scenario: Architecture suite pass check
    Tool: Bash
    Steps: Run ./gradlew test --tests "com.charlesluxinger.wex_transactions.architecture.*".
    Expected: All architecture tests pass with non-empty rule targets.
    Evidence: .sisyphus/evidence/task-7-architecture-hardening.txt

  Scenario: Intentional boundary violation sentinel
    Tool: Bash
    Steps: Run/verify sentinel strategy demonstrating architecture tests fail on illegal dependency direction.
    Expected: Failure detected when rule is broken; pass when restored.
    Evidence: .sisyphus/evidence/task-7-architecture-hardening-error.txt
  ```

  **Commit**: YES | Message: `test(architecture): harden boundary enforcement` | Files: [task artifacts only]

- [x] 8. Coverage Policy Activation Slice (local + CI)

  **What to do**: Activate `jacocoTestCoverageVerification` in local workflow and CI pipeline, aligning with existing 90% thresholds and domain expectations.
  **Must NOT do**: Do not misrepresent PR changed-files coverage as Gradle-enforced.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: build/CI policy alignment with risk of false signal.
  - Skills: `[]` - current repo files provide full context.
  - Omitted: [`gh-cli`] - no remote GitHub orchestration required in planning.

  **Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [10] | Blocked By: [1]

  **References**:
  - Gradle JaCoCo config: `build.gradle.kts`
  - CI workflow commands: `.github/workflows/ci.yml`
  - Gate order and notes: `AGENTS.md`

  **Acceptance Criteria**:
  - [ ] Planned tasks include local command path with `jacocoTestCoverageVerification`.
  - [ ] Planned tasks include CI step update invoking verification explicitly.
  - [ ] Planned tasks include source-of-truth alignment update for AGENTS/ROADMAP CI gate text.
  - [ ] Planned tasks preserve existing reporter-based changed-files gate behavior.

  **QA Scenarios**:
  ```
  Scenario: Local verification gate check
    Tool: Bash
    Steps: Run ./gradlew --no-daemon test jacocoTestCoverageVerification.
    Expected: Verification task executes and enforces thresholds.
    Evidence: .sisyphus/evidence/task-8-coverage-activation.txt

  Scenario: CI command chain validation
    Tool: Bash
    Steps: Validate workflow includes verification command in correct sequence and AGENTS/ROADMAP gate text is updated to match.
    Expected: CI plan contains explicit coverage verification step and no gate-order policy conflict remains.
    Evidence: .sisyphus/evidence/task-8-coverage-activation-error.txt
  ```

  **Commit**: YES | Message: `chore(ci): enforce jacoco coverage verification` | Files: [task artifacts only]

- [x] 9. Requirement-to-Task Traceability Slice

  **What to do**: Map each requirement from challenge/spec docs to implementation tasks, acceptance checks, and evidence artifacts.
  **Must NOT do**: Do not add new product scope.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: precision mapping and audit-ready structure.
  - Skills: `[]` - mapping from existing docs.
  - Omitted: [`to-issues`] - focus is single execution plan, not issue publication.

  **Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [10] | Blocked By: [3,5,6,8]

  **References**:
  - Primary requirements: `.docs/wex.md`, `.docs/SPEC.md`
  - Roadmap checkpoints: `.specs/features/ROADMAP.md`

  **Acceptance Criteria**:
  - [ ] Every requirement item maps to at least one implementation task and one QA scenario.
  - [ ] Missing mappings are surfaced as explicit blockers.

  **QA Scenarios**:
  ```
  Scenario: Full traceability scan
    Tool: Bash
    Steps: Run mapping consistency checks (requirements list vs task index vs evidence paths).
    Expected: 100% requirements mapped; zero orphan tasks.
    Evidence: .sisyphus/evidence/task-9-traceability.txt

  Scenario: Drift detection
    Tool: Bash
    Steps: Introduce simulated unmatched requirement entry in validation script.
    Expected: Validation fails and reports missing linkage.
    Evidence: .sisyphus/evidence/task-9-traceability-error.txt
  ```

  **Commit**: YES | Message: `docs(plan): add requirement-task traceability matrix` | Files: [task artifacts only]

- [x] 10. Final Execution Readiness Slice

  **What to do**: Consolidate execution order, parallel wave dispatch packets, and verification command playbook for `/start-work` handoff.
  **Must NOT do**: Do not execute implementation itself in this slice.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: operational clarity and unambiguous handoff packaging.
  - Skills: `[]` - plan artifact consolidation only.
  - Omitted: [`review-work`] - that is post-implementation.

  **Parallelization**: Can Parallel: NO | Wave 4 | Blocks: [F1,F2,F3,F4] | Blocked By: [7,8,9]

  **References**:
  - This plan: `.sisyphus/plans/wex-tech-challenge-implementation-tasks.md`
  - Verification order: `AGENTS.md`

  **Acceptance Criteria**:
  - [ ] Execution packet contains wave-by-wave task dispatch order with dependencies.
  - [ ] Verification playbook includes exact local/CI command sequence.

  **QA Scenarios**:
  ```
  Scenario: Handoff completeness check
    Tool: Bash
    Steps: Validate all tasks include agent profile, dependencies, acceptance, QA evidence path.
    Expected: No missing mandatory sections.
    Evidence: .sisyphus/evidence/task-10-readiness.txt

  Scenario: Command playbook dry validation
    Tool: Bash
    Steps: Verify all commands referenced in plan are syntactically valid and present.
    Expected: No invalid/undefined commands.
    Evidence: .sisyphus/evidence/task-10-readiness-error.txt
  ```

  **Commit**: YES | Message: `chore(plan): finalize execution readiness packet` | Files: [task artifacts only]

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Commit per task when task acceptance + QA evidence passes.
- Conventional messages:
  - `chore(challenge): close contract decisions`
  - `feat(purchase): implement store transaction slice`
  - `feat(conversion): implement retrieval conversion slice`
  - `chore(ci): enforce jacoco verification`

## Success Criteria
- Execution agent can run tasks sequentially/parallel per matrix without ambiguity.
- No unresolved architectural boundary decisions remain.
- Coverage policy is explicit and executable in local + CI.
- All feature requirements in `.docs/wex.md`/`.docs/SPEC.md` map to at least one task.
