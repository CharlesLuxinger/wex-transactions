# WEX Challenge Business Specification and Governance Update

## TL;DR
> **Summary**: Produce a business-level feature specification from `.docs/wex.md` and append mandatory testing constraints to `AGENTS.md` under `Code Quality Gates`, aligned with executable project constraints.
> **Deliverables**:
> - Updated `.docs/wex.md` business specification
> - Updated `AGENTS.md` testing strategy bullets under `## CODE QUALITY GATES`
> - Verification evidence for content and policy assertions
> **Effort**: Short
> **Parallel**: NO
> **Critical Path**: Task 1 → Task 2 → Task 3

## Context
### Original Request
Create a specifications document for WEX features, focused on understanding requirements (not task breakdown), applying project constraints, asking instead of assuming.

### Interview Summary
- Final doc location: `.docs`
- Spec depth: business-only
- Error depth: minimal
- Coverage rule: include domain-100 requirement
- Currency source: treasury-only
- AGENTS placement: append under `Code Quality Gates`
- API tests: no mocks at all
- Infra dependencies in tests: containers mandatory

### Metis Review (gaps addressed)
- Keep two deliverables separated: business spec vs governance policy update.
- Add explicit in-scope/out-of-scope to avoid technical-design drift.
- Convert “minimal errors” into concrete, limited business error classes.
- Use content-assertion acceptance checks (no manual approval checks).

## Work Objectives
### Core Objective
Deliver decision-complete documentation updates that translate challenge requirements into project-constrained business specifications and mandatory test governance.

### Deliverables
1. `.docs/wex.md` rewritten/extended as business specification.
2. `AGENTS.md` appended with mandatory testing strategy bullets under `## CODE QUALITY GATES`.
3. Evidence files proving section presence, forbidden-content absence, and governance phrases.

### Definition of Done (verifiable conditions with commands)
- `.docs/wex.md` contains business sections: Purpose, Scope, Business Rules, Minimal Error Rules, Non-Goals, Acceptance Criteria.
- `.docs/wex.md` states Treasury-only currency source and <=6-month historical lookup rule.
- `.docs/wex.md` contains no API endpoint paths (`/api/`) and no implementation package/class instructions.
- `AGENTS.md` contains appended mandatory testing bullets with: TDD mandatory, RestAssured default, no mocks in API tests, Testcontainers for E2E infra dependencies.
- Project constraints referenced consistently with executable truth (CI + Gradle coverage).

### Must Have
- Business-level language only in `.docs/wex.md`.
- Minimal error taxonomy only:
  - invalid description length
  - invalid date format
  - invalid/negative amount
  - no eligible rate within 6 months on/before purchase date
- Coverage mention includes stricter domain-100 rule.

### Must NOT Have
- No task decomposition in `.docs/wex.md`.
- No endpoint contract details (verbs, paths, schemas).
- No mock-based API testing allowance.
- No contradiction with `build.gradle.kts` or CI workflow.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after (documentation verification)
- QA policy: every task includes binary file-content checks
- Evidence:
  - `.sisyphus/evidence/task-1-business-spec.txt`
  - `.sisyphus/evidence/task-2-governance-append.txt`
  - `.sisyphus/evidence/task-3-final-audit.txt`

## Execution Strategy
### Parallel Execution Waves
Wave 1: Task 1 (business spec authoring)
Wave 2: Task 2 (AGENTS governance append)
Wave 3: Task 3 (automated content audit)

### Dependency Matrix
- Task 1 blocks Task 3
- Task 2 blocks Task 3
- Task 3 depends on Tasks 1 and 2

### Agent Dispatch Summary
- Wave 1: 1 task → writing
- Wave 2: 1 task → quick
- Wave 3: 1 task → quick

## TODOs
- [ ] 1. Rewrite `.docs/wex.md` as business specification

  **What to do**:
  - Keep original requirement intent.
  - Add sections:
    - Purpose
    - Business Scope (In/Out)
    - Business Rules for Store Transaction
    - Business Rules for Retrieve Converted Transaction
    - Minimal Error Rules
    - Non-Goals
    - Acceptance Criteria (business-verifiable)
  - Include policy statements:
    - currency source treasury-only
    - conversion uses rate <= purchase date within last 6 months
    - include domain-100 coverage constraint note in quality constraints subsection

  **Must NOT do**:
  - Add API routes, request/response schemas, class names, package-level design.

  **Recommended Agent Profile**:
  - Category: `writing` - Reason: documentation-first artifact
  - Skills: `[]` - Reason: no external library doc needed
  - Omitted: `[context7]` - Reason: source document already provided

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [3] | Blocked By: []

  **References**:
  - Requirement source: `.docs/wex.md`
  - Project constraints: `AGENTS.md`
  - Executable truth: `build.gradle.kts`, `.github/workflows/ci.yml`

  **Acceptance Criteria**:
  - [ ] `.docs/wex.md` includes section headers: `Purpose`, `Business Scope`, `Business Rules`, `Minimal Error Rules`, `Non-Goals`, `Acceptance Criteria`.
  - [ ] `.docs/wex.md` includes phrase: `treasury-only`.
  - [ ] `.docs/wex.md` includes six-month conversion eligibility rule.
  - [ ] `.docs/wex.md` does not include `/api/`, `@RestController`, or package path `com.charlesluxinger`.

  **QA Scenarios**:
  ```
  Scenario: Business sections present
    Tool: Bash
    Steps: rtk grep "Purpose|Business Scope|Business Rules|Minimal Error Rules|Non-Goals|Acceptance Criteria" .docs/wex.md
    Expected: All required headers found
    Evidence: .sisyphus/evidence/task-1-business-spec.txt

  Scenario: Technical leakage absent
    Tool: Bash
    Steps: rtk grep "/api/|@RestController|com.charlesluxinger" .docs/wex.md
    Expected: No matches
    Evidence: .sisyphus/evidence/task-1-business-spec-error.txt
  ```

  **Commit**: YES | Message: `docs(spec): rewrite wex challenge as business specification` | Files: `.docs/wex.md`

- [ ] 2. Append mandatory testing policy to `AGENTS.md` under `## CODE QUALITY GATES`

  **What to do**:
  - Add a subsection immediately after `## CODE QUALITY GATES` content:
    - `### Testing Strategy (Mandatory)`
  - Add mandatory bullets:
    - TDD mandatory (RED-GREEN-REFACTOR)
    - RestAssured default for API tests
    - No mocks at all in API tests
    - Testcontainers required for E2E infra dependencies

  **Must NOT do**:
  - Create a new top-level AGENTS section elsewhere.
  - Contradict existing CI/coverage constraints.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: single-file governance edit
  - Skills: `[]` - Reason: direct policy insertion
  - Omitted: `[clean-ddd-hexagonal]` - Reason: not architecture redesign

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [3] | Blocked By: [1 optional]

  **References**:
  - `AGENTS.md`
  - `build.gradle.kts`
  - `.github/workflows/ci.yml`

  **Acceptance Criteria**:
  - [ ] `AGENTS.md` contains header `### Testing Strategy (Mandatory)` under code-quality context.
  - [ ] `AGENTS.md` contains exact concepts: `TDD`, `RestAssured`, `No mocks`, `Testcontainers`.
  - [ ] Existing `CODE QUALITY GATES` table remains intact.

  **QA Scenarios**:
  ```
  Scenario: Governance bullets present
    Tool: Bash
    Steps: rtk grep "Testing Strategy \(Mandatory\)|TDD|RestAssured|No mocks|Testcontainers" AGENTS.md
    Expected: All policy lines present
    Evidence: .sisyphus/evidence/task-2-governance-append.txt

  Scenario: Section placement valid
    Tool: Bash
    Steps: rtk grep "## CODE QUALITY GATES|### Testing Strategy \(Mandatory\)" AGENTS.md
    Expected: Mandatory subsection appears in quality-gates area
    Evidence: .sisyphus/evidence/task-2-governance-append-error.txt
  ```

  **Commit**: YES | Message: `docs(agents): enforce mandatory testing strategy` | Files: `AGENTS.md`

- [ ] 3. Run final documentation compliance audit

  **What to do**:
  - Validate both documents against chosen scope.
  - Confirm minimal error taxonomy only.
  - Confirm treasury-only language and domain-100 coverage mention.

  **Must NOT do**:
  - Introduce new requirements not approved by user.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: deterministic content verification
  - Skills: `[]` - Reason: grep/read assertions sufficient
  - Omitted: `[momus]` - Reason: optional high-accuracy step offered separately

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: [] | Blocked By: [1,2]

  **References**:
  - `.docs/wex.md`
  - `AGENTS.md`

  **Acceptance Criteria**:
  - [ ] All task-1 and task-2 assertions pass.
  - [ ] No technical API contract details leaked into `.docs/wex.md`.
  - [ ] Both files reflect confirmed user decisions.

  **QA Scenarios**:
  ```
  Scenario: Consolidated assertion pass
    Tool: Bash
    Steps: run all grep assertions from tasks 1 and 2 and capture outputs
    Expected: Required matches found, forbidden matches absent
    Evidence: .sisyphus/evidence/task-3-final-audit.txt

  Scenario: Decision drift check
    Tool: Bash
    Steps: verify strings for business-only, minimal errors, treasury-only, domain 100, no mocks
    Expected: All present; no contradictory wording
    Evidence: .sisyphus/evidence/task-3-final-audit-error.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Commit 1: `.docs/wex.md` business specification rewrite
- Commit 2: `AGENTS.md` testing governance append
- Do not squash until final verification wave passes

## Success Criteria
- Documentation fully reflects requirements and confirmed constraints.
- No assumptions beyond user-approved decisions.
- Governance text explicit, enforceable, and colocated under code-quality section.
- All verification assertions produce binary pass/fail outputs with evidence paths.
