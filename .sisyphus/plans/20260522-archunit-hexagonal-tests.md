# ArchUnit Hexagonal Governance Tests Plan

## TL;DR
> **Summary**: Add ArchUnit JUnit 5 architecture tests that codify the hexagonal governance already defined in this repository, using strict TDD and non-vacuous rule enforcement.
> **Deliverables**:
> - ArchUnit dependency wired into test scope
> - Architecture test suite for the 3 governance rules from `AGENTS.md`
> - Non-vacuous test behavior and CI-compatible execution evidence
>
> **Effort**: Short
>
> **Parallel**: YES - 2 waves
>
> **Critical Path**: Task 1 → Task 2 → Task 3/4/5 → Task 6 → Task 7

## Context
### Original Request
- Based on hexagonal best practice, implement tests using `com.tngtech.archunit:archunit-junit5:1.4.2` and ArchUnit documentation.

### Interview Summary
- User selected **TDD strict** strategy.
- Repository is currently scaffold-only (single app class + smoke test), so architecture tests must be designed to avoid vacuous green outcomes.

### Metis Review (gaps addressed)
- Added explicit guardrails against vacuous passing in early-scaffold state.
- Locked scope to dependency + architecture tests (no production refactor work).
- Converted AGENTS governance text into explicit executable rule targets.
- Added policy defaults where user preference was implicit:
  - First rollout enforces core 3 governance rules only.
  - Controller dependencies allowed to inbound ports/use cases, disallowed to repositories/adapters.

## Work Objectives
### Core Objective
Enforce hexagonal architecture boundaries through executable ArchUnit tests that fail fast on boundary violations and run in current JUnit5/CI flow.

### Deliverables
1. `archunit-junit5` added in test dependencies.
2. Architecture test class(es) under `src/test/kotlin` enforcing:
   - strict dependency direction (`infra -> application -> domain`)
   - controller boundary in `infra/client`
   - use-case ownership for `*UseCaseImpl`
3. Non-vacuous behavior policy encoded in tests.
4. Execution evidence proving pass/fail behavior and CI compatibility.

### Definition of Done (verifiable conditions with commands)
- `./gradlew test --tests "*Architecture*"` passes on compliant code.
- Injecting a temporary violating class/import causes targeted ArchUnit test failure, then removal restores green.
- CI-equivalent command `./gradlew --no-daemon test jacocoTestReport` remains green.
- Rule intent traceable to `AGENTS.md` governance section and referenced in test comments/docs.

### Must Have
- Strict TDD sequence (RED → GREEN → REFACTOR) evidenced per rule group.
- Rule package scopes anchored to `com.charlesluxinger.wex_transactions`.
- Production classes only in import scope (exclude test classes).
- Architecture tests deterministic and binary (no flaky conditions).

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No production feature/domain behavior changes.
- No broad package renames or architecture refactor under this task.
- No vague wildcard rules that can silently pass due to empty selection.
- No replacement of existing CI flow.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: **TDD strict** using JUnit5 + ArchUnit.
- QA policy: Every task includes executable happy and failure scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

Wave 1: Task 1, Task 2 (foundation)
Wave 2: Task 3, Task 4, Task 5, Task 6, Task 7 (rules + hardening + alignment)

### Dependency Matrix (full, all tasks)
- Task 1 blocks Tasks 2-7
- Task 2 blocks Tasks 3-6
- Task 3 independent of Task 4 and Task 5 after Task 2
- Task 6 depends on Tasks 3-5
- Task 7 depends on Tasks 1-6
- Final Verification Wave depends on Tasks 1-7

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 2 tasks → `quick`, `unspecified-low`
- Wave 2 → 5 tasks → `quick`, `unspecified-high`
- Final verification → 4 tasks → `oracle`, `unspecified-high`, `deep`

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [x] 1. Add ArchUnit JUnit5 dependency and test entrypoint

  **What to do**:
  - Add `testImplementation("com.tngtech.archunit:archunit-junit5:<version>")` to Gradle test dependencies.
  - Keep existing JUnit5 engine usage unchanged.
  - Ensure new tests are discovered by existing `useJUnitPlatform()` config.

  **Must NOT do**:
  - Do not alter runtime dependencies.
  - Do not change CI workflow steps.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: small build-file change with immediate test verification.
  - Skills: [`kotlin-patterns`] - Gradle Kotlin DSL consistency.
  - Omitted: [`clean-ddd-hexagonal`] - not needed for dependency declaration itself.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [2,3,4,5,6,7] | Blocked By: []

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `build.gradle.kts:35-45` - current test dependency block.
  - Pattern: `build.gradle.kts:67-70` - JUnit platform activation.
  - External: `https://www.archunit.org/getting-started` - dependency + setup orientation.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `./gradlew test --tests "*ApplicationTests"` succeeds after dependency addition.
  - [ ] `./gradlew dependencies --configuration testRuntimeClasspath` shows ArchUnit artifact.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Dependency available in test classpath
    Tool: Bash
    Steps: Run `./gradlew dependencies --configuration testRuntimeClasspath`; capture output.
    Expected: Output contains `com.tngtech.archunit:archunit-junit5` resolved version.
    Evidence: .sisyphus/evidence/task-1-archunit-dependency.log

  Scenario: Broken coordinate fails build
    Tool: Bash
    Steps: Temporarily set invalid ArchUnit coordinate; run `./gradlew test`; revert to valid coordinate.
    Expected: Build fails with dependency resolution error, then returns green after revert.
    Evidence: .sisyphus/evidence/task-1-archunit-dependency-error.log
  ```

  **Commit**: YES | Message: `test(architecture): add archunit junit5 dependency` | Files: `build.gradle.kts`

- [x] 2. Create architecture test scaffold with deterministic class import policy

  **What to do**:
  - Create dedicated architecture test package under `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/`.
  - Add base ArchUnit test class/utility that imports production classes from base package only.
  - Exclude test classes from import and centralize base package constant.

  **Must NOT do**:
  - Do not implement governance rules yet in this task.
  - Do not import classes outside `com.charlesluxinger.wex_transactions`.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` - Reason: test scaffolding + import-policy setup.
  - Skills: [`kotlin-patterns`] - idiomatic Kotlin test structure.
  - Omitted: [`kotlin-springboot`] - no Spring wiring needed.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [3,4,5,6] | Blocked By: [1]

  **References**:
  - Pattern: `src/test/kotlin/com/charlesluxinger/wex_transactions/ApplicationTests.kt:1-13` - existing JUnit style and package root.
  - API/Type: `AGENTS.md:7-8` - package root and entrypoint.
  - External: `https://www.archunit.org/userguide/html/000_Index.html` - class import + JUnit integration.

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*Architecture*"` executes scaffold tests successfully.
  - [ ] Scaffold import excludes `src/test` classes (validated by intentional assertion/check in scaffold).

  **QA Scenarios**:
  ```
  Scenario: Architecture scaffold test executes
    Tool: Bash
    Steps: Run `./gradlew test --tests "*Architecture*"`.
    Expected: Test task discovers and executes architecture test scaffold.
    Evidence: .sisyphus/evidence/task-2-architecture-scaffold.log

  Scenario: Import policy catches wrong scope
    Tool: Bash
    Steps: Temporarily broaden import to include test package; run architecture test; restore production-only import.
    Expected: Architecture test fails due to violated import-scope assertion, then passes after restore.
    Evidence: .sisyphus/evidence/task-2-architecture-scaffold-error.log
  ```

  **Commit**: YES | Message: `test(architecture): scaffold archunit test base` | Files: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/**`

- [x] 3. Implement rule set A: strict dependency direction

  **What to do**:
  - Implement ArchUnit rule(s) for layer direction based on AGENTS governance:
    - `domain..` must not depend on `application..` or `infra..`.
    - `application..` must not depend on `infra..`.
  - Keep rule messages explicit and mapped to governance text.

  **Must NOT do**:
  - Do not relax rule to best-effort warnings.
  - Do not include third-party package checks beyond project base package.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: core architecture contract design.
  - Skills: [`clean-ddd-hexagonal`] - dependency-rule fidelity to hexagonal principles.
  - Omitted: [`kotlin-springboot`] - framework specifics not central.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [6,7] | Blocked By: [2]

  **References**:
  - Pattern: `AGENTS.md:22-24` - strict dependency direction definition.
  - Pattern: `AGENTS.md:30-38` - current regex-based dependency check intent.
  - External: `https://www.archunit.org/use-cases` - package/layer dependency constraints.

  **Acceptance Criteria**:
  - [ ] Red phase: introduce temporary violating import (domain→infra) and observe failing rule.
  - [ ] Green phase: remove violation and observe rule pass.

  **QA Scenarios**:
  ```
  Scenario: Dependency direction passes on compliant structure
    Tool: Bash
    Steps: Run `./gradlew test --tests "*Architecture*dependency*"` after compliant setup.
    Expected: All dependency-direction ArchUnit tests pass.
    Evidence: .sisyphus/evidence/task-3-dependency-direction.log

  Scenario: Domain imports infra triggers failure
    Tool: Bash
    Steps: Add temporary test fixture or temporary class violating domain->infra dependency; run targeted test; remove fixture.
    Expected: ArchUnit reports violation referencing domain/infra boundary.
    Evidence: .sisyphus/evidence/task-3-dependency-direction-error.log
  ```

  **Commit**: YES | Message: `test(architecture): enforce inward dependency direction` | Files: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/**`

- [x] 4. Implement rule set B: controller boundary enforcement

  **What to do**:
  - Implement rule: classes under `infra.client..` must not depend on repository/adapter packages directly.
  - Allow dependencies to inbound ports/use case abstractions only.
  - Ensure failure output names offending class and import.

  **Must NOT do**:
  - Do not ban legitimate DTO/model classes used at API boundary.
  - Do not enforce service-layer naming in this task.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: nuanced allowed/forbidden dependency targeting.
  - Skills: [`clean-ddd-hexagonal`] - controller boundary semantics.
  - Omitted: [`kotlin-coroutines-flows`] - not relevant.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [6,7] | Blocked By: [2]

  **References**:
  - Pattern: `AGENTS.md:23` - controller boundary policy.
  - Pattern: `AGENTS.md:40-48` - repository/adapter forbidden dependency checks.
  - Pattern: `AGENTS.md:65-67` - controller and inbound port package conventions.

  **Acceptance Criteria**:
  - [ ] Red phase: temporary controller dependency on forbidden repository/adapter fails.
  - [ ] Green phase: dependency shifted to allowed port/use-case passes.

  **QA Scenarios**:
  ```
  Scenario: Controller boundary remains clean
    Tool: Bash
    Steps: Run `./gradlew test --tests "*Architecture*controller*"`.
    Expected: No controller->repository/adapter direct dependency violations.
    Evidence: .sisyphus/evidence/task-4-controller-boundary.log

  Scenario: Controller direct repository dependency fails
    Tool: Bash
    Steps: Introduce temporary forbidden import from infra.client class to repository/adapter package; run targeted test; revert.
    Expected: ArchUnit failure indicates forbidden dependency path.
    Evidence: .sisyphus/evidence/task-4-controller-boundary-error.log
  ```

  **Commit**: YES | Message: `test(architecture): enforce controller dependency boundary` | Files: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/**`

- [x] 5. Implement rule set C: use-case ownership and location policy

  **What to do**:
  - Implement rule: any class matching `*UseCaseImpl` must reside under `application.service..`.
  - Ensure rule catches both misplaced location and naming misuse.

  **Must NOT do**:
  - Do not enforce unrelated naming patterns (`*Adapter`, `*Port`) in this rollout.
  - Do not move production classes in this task.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: focused naming/location rule.
  - Skills: [`clean-ddd-hexagonal`] - use-case placement policy.
  - Omitted: [`kotlin-springboot`] - no runtime wiring.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [6,7] | Blocked By: [2]

  **References**:
  - Pattern: `AGENTS.md:24` - use-case ownership statement.
  - Pattern: `AGENTS.md:50-59` - existing filesystem check intent.
  - Pattern: `AGENTS.md:69` - canonical package for use-case implementations.

  **Acceptance Criteria**:
  - [ ] Red phase: temporary `*UseCaseImpl` outside `application.service..` fails rule.
  - [ ] Green phase: compliant placement passes.

  **QA Scenarios**:
  ```
  Scenario: UseCaseImpl ownership rule passes
    Tool: Bash
    Steps: Run `./gradlew test --tests "*Architecture*usecase*"`.
    Expected: No ownership violations for `*UseCaseImpl` classes.
    Evidence: .sisyphus/evidence/task-5-usecase-ownership.log

  Scenario: Misplaced UseCaseImpl fails
    Tool: Bash
    Steps: Add temporary `*UseCaseImpl` class outside application.service package; run targeted tests; delete temporary class.
    Expected: ArchUnit reports incorrect package location.
    Evidence: .sisyphus/evidence/task-5-usecase-ownership-error.log
  ```

  **Commit**: YES | Message: `test(architecture): enforce use-case ownership location` | Files: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/**`

- [x] 6. Harden suite against vacuous green outcomes

  **What to do**:
  - Configure architecture rules/tests so empty target sets do not silently pass.
  - Add explicit test assertions/messages for current scaffold phase behavior.
  - Document temporary expected behavior until full package tree exists.

  **Must NOT do**:
  - Do not disable rules to keep pipeline green.
  - Do not hide failures behind `@Disabled`.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: correctness guardrail for early codebase maturity.
  - Skills: [`clean-ddd-hexagonal`] - architectural integrity over time.
  - Omitted: [`kotlin-specialist`] - unnecessary breadth.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [7] | Blocked By: [3,4,5]

  **References**:
  - Pattern: `AGENTS.md:10` - early scaffold status.
  - Pattern: `AGENTS.md:12-19` - planned target layer packages.
  - External: `https://www.archunit.org/userguide/html/000_Index.html` - rule behavior/customization guidance.

  **Acceptance Criteria**:
  - [ ] Rules fail meaningfully when evaluating empty/undefined layer scopes (unless explicitly waived by policy comment).
  - [ ] Test output contains actionable guidance for expected package creation paths.

  **QA Scenarios**:
  ```
  Scenario: Non-vacuous behavior enforced
    Tool: Bash
    Steps: Run full architecture test suite on current scaffold.
    Expected: Suite behavior matches defined policy (explicit fail/explicitly documented pending), never silent pass by accident.
    Evidence: .sisyphus/evidence/task-6-non-vacuous.log

  Scenario: Silent pass prevention
    Tool: Bash
    Steps: Temporarily configure a rule to allow empty scope; run suite; restore strict configuration.
    Expected: Guard test fails when empty-scope allowance is introduced.
    Evidence: .sisyphus/evidence/task-6-non-vacuous-error.log
  ```

  **Commit**: YES | Message: `test(architecture): prevent vacuous archunit passes` | Files: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/**`

- [x] 7. Final alignment with tech-challenge implementation plan and CI run

  **What to do**:
  - Cross-check architecture tests against boundaries expected by `.sisyphus/plans/tech-challenge-backend.md`.
  - Execute CI-equivalent local command and capture logs.
  - Ensure evidence artifacts are saved for evaluator review.

  **Must NOT do**:
  - Do not add new architecture rules beyond the core 3 in this task.
  - Do not modify unrelated tasks in `tech-challenge-backend.md`.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: final consistency + command verification.
  - Skills: [`clean-ddd-hexagonal`] - verify scope fidelity against architecture intent.
  - Omitted: [`kotlin-coroutines-flows`] - not relevant.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [Final Verification Wave] | Blocked By: [1,2,3,4,5,6]

  **References**:
  - Pattern: `.sisyphus/plans/tech-challenge-backend.md:4,48-54,66-69` - hexagonal + domain boundary expectations.
  - Pattern: `.github/workflows/ci.yml:44-46` - CI test + coverage step.
  - Pattern: `build.gradle.kts:67-70,72-78` - local test/coverage behavior.

  **Acceptance Criteria**:
  - [ ] `./gradlew --no-daemon test jacocoTestReport` succeeds with architecture tests enabled.
  - [ ] Evidence files exist for all tasks 1-7.

  **QA Scenarios**:
  ```
  Scenario: CI-equivalent command succeeds
    Tool: Bash
    Steps: Run `./gradlew --no-daemon test jacocoTestReport`.
    Expected: Test and jacoco tasks finish successfully with architecture tests included.
    Evidence: .sisyphus/evidence/task-7-ci-alignment.log

  Scenario: Boundary regression is detected
    Tool: Bash
    Steps: Introduce one temporary boundary-violating class; run full test command; revert violation.
    Expected: Build fails in architecture tests with clear violation report, then returns green post-revert.
    Evidence: .sisyphus/evidence/task-7-ci-alignment-error.log
  ```

  **Commit**: YES | Message: `test(architecture): align arch rules with tech challenge plan` | Files: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/**`, `build.gradle.kts` (if final pin/version adjustment needed)

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- One commit per task (feature-sliced) to preserve atomic traceability.
- Commit order must follow dependency chain: 1 → 2 → (3/4/5) → 6 → 7.
- No squashing until user explicitly requests history rewrite.

## Success Criteria
- Core hexagonal governance is executable as architecture tests, not only documented text.
- Any boundary regression is caught by automated tests before merge.
- Test execution stays compatible with existing CI and coverage workflow.
- Plan remains scoped to architecture-test implementation only.
