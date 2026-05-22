# AGENTS.md Refresh for Pre-Implementation Repo

## TL;DR
> **Summary**: Tighten existing `AGENTS.md` into a compact, verified instruction file aligned to executable config and current `.specs/.docs` intent.
> **Deliverables**:
> - Updated `AGENTS.md` with only high-signal, verified guidance
> - Resolved policy wording for test-layer mock usage
> - Conditionalized or removed stale graphify mandates
> **Effort**: Short
> **Parallel**: YES - 2 waves
> **Critical Path**: Task 1 → Task 2 → Task 4 → Task 6

## Context
### Original Request
Create or update `AGENTS.md` to help future OpenCode sessions avoid mistakes and ramp quickly, keeping only likely-to-be-missed guidance.

### Interview Summary
- Project is not implemented yet.
- Existing `AGENTS.md` must be improved in place, not rewritten blindly.
- Implementation context should come from `./.specs` and `./.docs`.
- Prefer executable truth over prose if conflicts exist.

### Metis Review (gaps addressed)
- Guardrail added: strict precedence ladder for facts.
- Guardrail added: avoid tool/config claims not currently present.
- Guardrail added: resolve test-policy conflict by scope (unit vs API/E2E).
- Default applied: graphify guidance is conditional because `graphify-out/GRAPH_REPORT.md` is absent.

## Work Objectives
### Core Objective
Produce an updated `AGENTS.md` that is concise, executable-truth-aligned, and implementation-ready for future agents.

### Deliverables
- `AGENTS.md` updated in place with compact, verified sections.
- Explicit command order mirroring CI.
- Clear architecture/governance rules preserved where still valid.
- Testing guidance split by test level to remove contradictions.

### Definition of Done (verifiable conditions with commands)
- `AGENTS.md` exists and is modified: `rtk git diff -- AGENTS.md`
- CI order in doc matches workflow file: `rtk grep "ktlintMainSourceSetCheck|ktlintTestSourceSetCheck|detekt|test jacocoTestReport" .github/workflows/ci.yml`
- Coverage thresholds in doc match Gradle config: `rtk grep "minimum = \"1.0\"|minimum = \"0.9\"" build.gradle.kts`
- Tool/version claims in doc are present in executable config: `rtk grep "3.5.14|2.3.21|JavaLanguageVersion.of\(25\)" build.gradle.kts`
- No unconditional graphify-report requirement remains if file absent: `rtk grep "GRAPH_REPORT.md" AGENTS.md`

### Must Have
- Source-of-truth precedence statement.
- Exact local and CI verification commands (repo-specific).
- Architecture boundary rules that affect implementation behavior.
- Test policy scoping (unit vs API/E2E) with no contradiction.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No generic Kotlin/Spring tutorials.
- No unverifiable claims.
- No references to non-existent config files as mandatory.
- No long file-tree dumps.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after (doc change), using command assertions and diff checks.
- QA policy: Every task includes happy and failure/edge scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

Wave 1: factual baseline + conflict resolution
- T1 authoritative source matrix
- T2 CI/Gradle command extraction
- T3 test policy scope resolution
- T4 graphify/config presence validation

Wave 2: document refinement + verification
- T5 rewrite/trim sections in AGENTS.md
- T6 verification sweep against executable files
- T7 final compactness pass

### Dependency Matrix (full, all tasks)
- T1: no deps
- T2: blocked by T1
- T3: blocked by T1
- T4: blocked by T1
- T5: blocked by T2,T3,T4
- T6: blocked by T5
- T7: blocked by T6

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 4 tasks → quick/unspecified-low
- Wave 2 → 3 tasks → writing/quick

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Build authoritative source matrix

  **What to do**: Create a one-section source precedence in `AGENTS.md` based on verified files: `build.gradle.kts`, `.github/workflows/ci.yml`, `.editorconfig`, `config/detekt/detekt.yml`, then `.specs`/`.docs` as supplemental intent.
  **Must NOT do**: Do not claim prose docs override executable config.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: small, deterministic text update.
  - Skills: [`clean-ddd-hexagonal`] - why needed: preserve architecture governance terminology.
  - Omitted: [`context7`] - why not needed: no external API ambiguity.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [2,3,4,5,6,7] | Blocked By: []

  **References**:
  - Pattern: `AGENTS.md` - existing governance/source-of-truth framing to preserve/trim
  - API/Type: `build.gradle.kts` - executable build/test/coverage truth
  - Test: `.github/workflows/ci.yml` - CI order and gates
  - External: `./.specs/codebase/STACK.md` - intended architecture context only

  **Acceptance Criteria**:
  - [ ] `AGENTS.md` includes an explicit precedence ladder with executable sources first.
  - [ ] Wording states `.docs/.specs` are supporting context, not authority over config.

  **QA Scenarios**:
  ```
  Scenario: Happy path precedence present
    Tool: Bash
    Steps: rtk grep "source of truth|executable config|build.gradle.kts|ci.yml" AGENTS.md
    Expected: Matches found for precedence statement and exact files
    Evidence: .sisyphus/evidence/task-1-source-matrix.txt

  Scenario: Failure case prose-over-config contradiction
    Tool: Bash
    Steps: rtk grep "docs.*override|specs.*override" AGENTS.md
    Expected: No matches
    Evidence: .sisyphus/evidence/task-1-source-matrix-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): define source precedence` | Files: [AGENTS.md]

- [ ] 2. Mirror CI and local command workflow exactly

  **What to do**: Update commands section to include exact CI order and compact local equivalents, including format-first local workflow and focused single-test examples.
  **Must NOT do**: Do not invent commands or reorder CI gates.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: command extraction and doc sync.
  - Skills: [] - why needed: direct from config files.
  - Omitted: [`kotlin-springboot`] - why not needed: no framework design decision.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [5,6,7] | Blocked By: [1]

  **References**:
  - Pattern: `AGENTS.md` existing command section
  - API/Type: `.github/workflows/ci.yml:39-46` - canonical CI sequence
  - Test: `build.gradle.kts:68-70` - test finalizedBy jacoco report

  **Acceptance Criteria**:
  - [ ] CI command sequence in `AGENTS.md` matches workflow order exactly.
  - [ ] Local commands include `ktlint*Format`, then checks, `detekt`, `test`.
  - [ ] Includes focused test runs via `--tests` examples.

  **QA Scenarios**:
  ```
  Scenario: Happy path command parity
    Tool: Bash
    Steps: rtk grep "ktlintMainSourceSetCheck|ktlintTestSourceSetCheck|detekt|test jacocoTestReport" .github/workflows/ci.yml && rtk grep "ktlintMainSourceSetCheck|ktlintTestSourceSetCheck|detekt|test" AGENTS.md
    Expected: Both files contain expected gate sequence and task names
    Evidence: .sisyphus/evidence/task-2-commands.txt

  Scenario: Failure case invented commands
    Tool: Bash
    Steps: rtk grep "mvn |npm |pnpm " AGENTS.md
    Expected: No unrelated build-tool commands present
    Evidence: .sisyphus/evidence/task-2-commands-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): align command workflow with CI` | Files: [AGENTS.md]

- [ ] 3. Resolve testing policy contradictions by test layer

  **What to do**: Rewrite testing guidance to explicitly separate policy by layer: unit tests may use mocks where needed; API/integration/E2E tests must avoid mocks and use real dependencies/Testcontainers as applicable.
  **Must NOT do**: Do not keep ambiguous blanket “no mocks anywhere” phrasing.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: policy wording precision.
  - Skills: [`clean-ddd-hexagonal`] - why needed: preserve architecture-test intent.
  - Omitted: [`tdd`] - why not needed: policy update only.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [5,6,7] | Blocked By: [1]

  **References**:
  - Pattern: `AGENTS.md` testing strategy section
  - API/Type: `.specs/codebase/TESTING.md` - states unit-test mocking tools
  - Test: `build.gradle.kts` test dependencies include spring-boot-starter-test

  **Acceptance Criteria**:
  - [ ] `AGENTS.md` contains explicit layer split for mocking policy.
  - [ ] No internal contradiction remains in testing section.

  **QA Scenarios**:
  ```
  Scenario: Happy path scoped policy
    Tool: Bash
    Steps: rtk grep "unit.*mock|API.*no mock|integration|Testcontainers" AGENTS.md
    Expected: Scoped statements exist for unit and API/integration levels
    Evidence: .sisyphus/evidence/task-3-test-policy.txt

  Scenario: Failure case contradictory wording
    Tool: Bash
    Steps: rtk grep "no mocks" AGENTS.md
    Expected: If present, text is clearly scoped to API/integration, not global
    Evidence: .sisyphus/evidence/task-3-test-policy-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): scope mock policy by test level` | Files: [AGENTS.md]

- [ ] 4. Make graphify guidance conditional to repository state

  **What to do**: Update graphify section so mandatory language applies only when `graphify-out/GRAPH_REPORT.md` exists; otherwise add fallback exploration path.
  **Must NOT do**: Do not require reading files that are absent.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: small conditional wording fix.
  - Skills: [] - why needed: direct file-state alignment.
  - Omitted: [`graphify`] - why not needed: no graph generation required.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [5,6,7] | Blocked By: [1]

  **References**:
  - Pattern: `AGENTS.md` graphify section
  - API/Type: search result shows no `**/GRAPH_REPORT.md`
  - Test: n/a

  **Acceptance Criteria**:
  - [ ] AGENTS graphify instructions include conditional wording tied to file presence.
  - [ ] No unconditional “ALWAYS read GRAPH_REPORT.md” remains unless presence check is included.

  **QA Scenarios**:
  ```
  Scenario: Happy path conditionalized guidance
    Tool: Bash
    Steps: rtk grep "if present|when available|GRAPH_REPORT.md" AGENTS.md
    Expected: Conditional text exists
    Evidence: .sisyphus/evidence/task-4-graphify.txt

  Scenario: Failure case stale mandatory wording
    Tool: Bash
    Steps: rtk grep "ALWAYS read .*GRAPH_REPORT.md" AGENTS.md
    Expected: No unconditional mandatory match
    Evidence: .sisyphus/evidence/task-4-graphify-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): conditionalize graphify requirements` | Files: [AGENTS.md]

- [ ] 5. Compact AGENTS.md to high-signal sections only

  **What to do**: Prune low-signal text; keep compact sections: overview, architecture boundaries, command workflow, quality gates, testing policy, source precedence, and essential skills mapping.
  **Must NOT do**: Do not remove verified constraints that prevent common agent mistakes.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: structured condensation.
  - Skills: [] - why needed: repo-specific editorial work.
  - Omitted: [`kotlin-patterns`] - why not needed: no code style implementation.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [6,7] | Blocked By: [2,3,4]

  **References**:
  - Pattern: current `AGENTS.md`
  - API/Type: `.editorconfig`, `config/detekt/detekt.yml`, `build.gradle.kts`
  - Test: `.github/workflows/ci.yml`

  **Acceptance Criteria**:
  - [ ] AGENTS content is concise and repo-specific; no generic filler remains.
  - [ ] All retained claims are traceable to verified files.

  **QA Scenarios**:
  ```
  Scenario: Happy path compactness with coverage
    Tool: Bash
    Steps: rtk git diff -- AGENTS.md
    Expected: Diff shows pruning + precise additions; no unrelated files changed
    Evidence: .sisyphus/evidence/task-5-compactness.txt

  Scenario: Failure case unverifiable claims
    Tool: Bash
    Steps: rtk grep "always|must" AGENTS.md
    Expected: Mandatory statements correspond to verifiable repo constraints
    Evidence: .sisyphus/evidence/task-5-compactness-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): compress to high-signal guidance` | Files: [AGENTS.md]

- [ ] 6. Run executable-truth verification sweep

  **What to do**: Validate each mandatory statement in `AGENTS.md` against authoritative files; fix mismatches immediately.
  **Must NOT do**: Do not accept “close enough” wording for thresholds/orders/versions.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: deterministic consistency checks.
  - Skills: [] - why needed: shell assertions only.
  - Omitted: [`oracle`] - why not needed: no architecture uncertainty left.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [7] | Blocked By: [5]

  **References**:
  - Pattern: `AGENTS.md`
  - API/Type: `build.gradle.kts`, `.github/workflows/ci.yml`, `.editorconfig`, `config/detekt/detekt.yml`
  - Test: `.specs/codebase/TESTING.md`

  **Acceptance Criteria**:
  - [ ] All command/order/version/threshold claims match source files exactly.
  - [ ] No mention of missing local configs (`.cursor`, `opencode.json`, copilot instructions) as required setup.

  **QA Scenarios**:
  ```
  Scenario: Happy path full assertion sweep
    Tool: Bash
    Steps: rtk grep "JavaLanguageVersion.of\(25\)|3.5.14|2.3.21|minim.*0.9|minim.*1.0" build.gradle.kts && rtk grep "ktlintMainSourceSetCheck|detekt|test jacocoTestReport" .github/workflows/ci.yml && rtk grep "ktlint_style|max_line_length|indent_size" .editorconfig
    Expected: All assertions return expected matches
    Evidence: .sisyphus/evidence/task-6-verification.txt

  Scenario: Failure case stale version drift retained
    Tool: Bash
    Steps: rtk grep "3\.14" AGENTS.md
    Expected: No stale Spring Boot version reference
    Evidence: .sisyphus/evidence/task-6-verification-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): verify against executable truth` | Files: [AGENTS.md]

- [ ] 7. Final readability + anti-slop pass

  **What to do**: Ensure each retained line answers “likely missed without help”; remove residual obvious/default statements.
  **Must NOT do**: Do not drop critical operational constraints.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: final signal/noise optimization.
  - Skills: [] - why needed: editorial refinement.
  - Omitted: [`improve-codebase-architecture`] - why not needed: not architecture redesign.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [] | Blocked By: [6]

  **References**:
  - Pattern: `AGENTS.md`
  - API/Type: user request criteria in current session
  - Test: n/a

  **Acceptance Criteria**:
  - [ ] AGENTS.md is compact, high-signal, and free of generic advice.
  - [ ] Final diff touches only `AGENTS.md`.

  **QA Scenarios**:
  ```
  Scenario: Happy path final file-only diff
    Tool: Bash
    Steps: rtk git diff --name-only
    Expected: Output contains only AGENTS.md (and optional evidence files if created)
    Evidence: .sisyphus/evidence/task-7-final-pass.txt

  Scenario: Failure case scope creep
    Tool: Bash
    Steps: rtk git diff --name-only | rtk grep -v "AGENTS.md|.sisyphus/evidence"
    Expected: No unexpected repository file changes
    Evidence: .sisyphus/evidence/task-7-final-pass-error.txt
  ```

  **Commit**: NO | Message: `docs(agents): finalize concise operational guidance` | Files: [AGENTS.md]

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Single commit after all verification checks pass.
- Message: `docs(agents): refresh high-signal repository instructions`
- Include only `AGENTS.md` (+ optional `.sisyphus/evidence/*` if policy requires retaining evidence).

## Success Criteria
- Future agent can execute correct quality workflow without guessing.
- Architecture/testing guardrails are unambiguous.
- All statements are evidence-backed and conflict-free.
- File remains compact and implementation-focused.