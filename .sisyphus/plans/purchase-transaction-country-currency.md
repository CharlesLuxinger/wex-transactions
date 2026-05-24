# Purchase Retrieval in Treasury Target Currency (OpenFeign)

## TL;DR
> **Summary**: Replace dummy exchange integration with Treasury-backed OpenFeign client and add retrieval fallback that uses persisted rates first, then Treasury historical rates, enforcing <= purchase date and 6-month window.
> **Deliverables**:
> - Real Treasury OpenFeign adapter with retry/timeout
> - Retrieval fallback orchestration
> - Cache-by-write persistence of fetched Treasury rate
> - Error mapping and deterministic tests
> **Effort**: Medium
> **Parallel**: YES - 3 waves
> **Critical Path**: Task 2 → Task 4 → Task 6 → Task 9

## Context
### Original Request
Implement real client for Treasury Reporting Rates of Exchange API, replacing `DummyExchangeRateClientAdapter`, with retry and timeout using OpenFeign, and support retrieval conversion in specified currency.

### Interview Summary
- Keep current input contract: `targetCurrency` ISO-4217.
- Retrieval source strategy: persisted rate first; Treasury fallback on miss.
- Retry policy: 3 attempts total, exponential backoff + jitter.
- Retryable failures: 429/503/504 + IO/connect/read timeout.
- Timeouts: connect 1s, read 3s.
- No eligible rate (`<= purchaseDate` within 6 months): `RateUnavailableException` -> HTTP 422.
- Strategy: TDD red-green-refactor.
- Fallback result must be persisted (`cache-by-write`).

### Metis Review (gaps addressed)
- Guardrail: keep endpoint contract unchanged.
- Guardrail: retrieval-only orchestration change; do not alter store flow behavior beyond replacing dummy client.
- Guardrail: no retry stacking; one retry owner only.
- Guardrail: preserve hexagonal boundaries (`infra -> application -> domain`).
- Guardrail: explicit date-selection tests for boundary and ordering.

## Work Objectives
### Core Objective
Deliver production-grade Treasury exchange-rate integration and retrieval fallback with deterministic historical-rate selection and strict error semantics.

### Deliverables
- New OpenFeign-based Treasury adapter implementing `ExchangeRateClientPort`.
- Feign config for retryer, timeout, error decoder.
- Retrieval use-case fallback flow + cache-by-write.
- Tests across unit/application/adapter/controller levels.
- No contract regressions on retrieve endpoint.

### Definition of Done (verifiable conditions with commands)
- Treasury adapter replaces dummy bean wiring.
- Retrieval succeeds with persisted rate and Treasury fallback.
- 6-month rule enforced for fallback.
- Retry + timeout behavior verified by tests.
- CI-quality gates pass.

Commands:
- `./gradlew test --tests "*RetrieveConverted*"`
- `./gradlew test --tests "*Treasury*"`
- `./gradlew test --tests "com.charlesluxinger.wex_transactions.architecture.*"`
- `./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck`
- `./gradlew detekt`
- `./gradlew test`

### Must Have
- Keep `GET /api/v1/purchases/{purchaseId}/converted?targetCurrency=...` contract.
- Treasury endpoint usage aligned with Fiscal Data API docs.
- Historical selection rule: greatest `record_date` <= purchase date.
- Eligible window: inclusive 6 months.
- Converted amount scale remains 2 decimals.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No controller contract redesign.
- No country-input contract addition.
- No circuit breaker/bulkhead/caching layer beyond cache-by-write persistence.
- No retry duplication (Feign + another retry layer).
- No leakage of Feign/Treasury DTOs into domain model.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: TDD red-green-refactor with JUnit5 + Mockito + RestAssured + Testcontainers.
- QA policy: Every task includes executable happy/failure checks.
- Evidence path: `.sisyphus/evidence/task-{N}-{slug}.{ext}`.

## Execution Strategy
### Parallel Execution Waves
Wave 1: foundation + contract freeze + infra config
- Task 1, 2, 3, 5

Wave 2: orchestration + persistence + adapter integration
- Task 4, 6, 7, 8

Wave 3: scenario hardening + regressions
- Task 9, 10, 11

### Dependency Matrix (full, all tasks)
- 1 blocks 2,4,9
- 2 blocks 4,6,9
- 3 blocks 6,7
- 4 blocks 6,8,9
- 5 blocks 6,9
- 6 blocks 9,10
- 7 blocks 9
- 8 blocks 10
- 9 blocks 11
- 10 blocks 11
- 11 blocks Final Verification

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 4 tasks → quick/unspecified-high
- Wave 2 → 4 tasks → unspecified-high/deep
- Wave 3 → 3 tasks → unspecified-high/quick

## TODOs
- [ ] 1. Freeze retrieve contract tests

  **What to do**: Add/adjust API tests ensuring existing retrieve endpoint path, query param name, and response fields remain unchanged.
  **Must NOT do**: Do not add new endpoint or rename fields.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: bounded test updates.
  - Skills: `[]` - no special skill required.
  - Omitted: `[playwright-cli]` - non-UI API work.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2,4,9 | Blocked By: none

  **References**:
  - Pattern: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1Test.kt`
  - API: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1.kt`

  **Acceptance Criteria**:
  - [ ] Existing retrieve tests pass with unchanged response contract.
  - [ ] Negative invalid-currency case remains 400.

  **QA Scenarios**:
  ```
  Scenario: Existing retrieve contract
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedControllerV1Test*"
    Expected: Tests green; no contract diff needed.
    Evidence: .sisyphus/evidence/task-1-contract.txt

  Scenario: Invalid target currency
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedControllerV1Test*"
    Expected: 400 mapping still asserted by test.
    Evidence: .sisyphus/evidence/task-1-contract-error.txt
  ```

  **Commit**: YES | Message: `test(retrieve): freeze converted endpoint contract` | Files: [retrieveConverted controller tests]

- [ ] 2. Add Treasury OpenFeign API client and DTOs

  **What to do**: Create Treasury Feign interface and infra DTOs for `rates_of_exchange` endpoint with fields/filter/sort/page parameters required for historical lookup.
  **Must NOT do**: Do not expose Treasury DTOs to domain/application API.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: external API integration details.
  - Skills: `[kotlin-springboot]` - Spring/OpenFeign conventions.
  - Omitted: `[context7-mcp]` - docs already resolved for this plan.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 4,6,9 | Blocked By: 1

  **References**:
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateClientPort.kt`
  - Dummy to replace: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/DummyExchangeRateClientAdapter.kt`
  - Treasury docs: `https://fiscaldata.treasury.gov/api-documentation/`

  **Acceptance Criteria**:
  - [ ] Feign interface compiles and maps endpoint query parameters.
  - [ ] DTO parsing handles string-valued payload fields.

  **QA Scenarios**:
  ```
  Scenario: Build compiles with new Feign client
    Tool: Bash
    Steps: ./gradlew test --tests "*DummyExchangeRateClientAdapter*" --tests "*Treasury*"
    Expected: Compilation success for new client tests.
    Evidence: .sisyphus/evidence/task-2-feign.txt

  Scenario: Treasury error payload compatibility
    Tool: Bash
    Steps: ./gradlew test --tests "*Treasury*Error*"
    Expected: Error DTO/decoder tests pass on malformed/empty body.
    Evidence: .sisyphus/evidence/task-2-feign-error.txt
  ```

  **Commit**: YES | Message: `feat(exchange): add treasury feign endpoint client` | Files: [infra adapter external files]

- [ ] 3. Configure timeout and retry policy

  **What to do**: Add OpenFeign client config for connect/read timeout 1s/3s; retryer with 3 total attempts using exponential backoff + jitter; retry only 429/503/504 + IO/timeout via ErrorDecoder/RetryableException policy.
  **Must NOT do**: Do not add second retry layer.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: resilience correctness.
  - Skills: `[kotlin-springboot]` - Feign config patterns.
  - Omitted: `[clean-ddd-hexagonal]` - infra-only configuration task.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 6,9 | Blocked By: none

  **References**:
  - Config style: `src/main/resources/application.yml` (or current config layout)
  - OpenFeign docs: `https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html`
  - Feign docs: `https://github.com/openfeign/feign/blob/master/README.md`

  **Acceptance Criteria**:
  - [ ] Timeouts applied for Treasury client only.
  - [ ] Retry behavior deterministic in tests.

  **QA Scenarios**:
  ```
  Scenario: Retry then success
    Tool: Bash
    Steps: ./gradlew test --tests "*Treasury*Retry*"
    Expected: 503,503,200 sequence succeeds at attempt 3.
    Evidence: .sisyphus/evidence/task-3-retry.txt

  Scenario: Non-retryable 400
    Tool: Bash
    Steps: ./gradlew test --tests "*Treasury*Retry*" --tests "*ErrorDecoder*"
    Expected: 400 does not retry; mapped failure asserted.
    Evidence: .sisyphus/evidence/task-3-retry-error.txt
  ```

  **Commit**: YES | Message: `feat(exchange): configure feign timeout and retry policy` | Files: [config + feign config classes]

- [ ] 4. Add retrieval fallback orchestration

  **What to do**: In `RetrieveConvertedUseCaseImpl`, keep persisted-rate first path. If miss, call `ExchangeRateClientPort` for target currency historical rates, select best rate <= purchase date within inclusive 6-month window.
  **Must NOT do**: Do not bypass repository-first logic.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: business-rule orchestration.
  - Skills: `[clean-ddd-hexagonal]` - preserve boundaries.
  - Omitted: `[playwright-cli]` - not UI.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 6,8,9 | Blocked By: 1,2

  **References**:
  - Use case: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt`
  - Exceptions: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/DomainException.kt`

  **Acceptance Criteria**:
  - [ ] Persisted hit path unchanged.
  - [ ] Persisted miss calls Treasury path.
  - [ ] Best eligible rate selection deterministic.

  **QA Scenarios**:
  ```
  Scenario: Persisted miss, Treasury success
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedUseCaseImplTest*"
    Expected: Uses Treasury-selected rate <= purchase date.
    Evidence: .sisyphus/evidence/task-4-fallback.txt

  Scenario: Only future rates available
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedUseCaseImplTest*"
    Expected: Returns RateUnavailableException.
    Evidence: .sisyphus/evidence/task-4-fallback-error.txt
  ```

  **Commit**: YES | Message: `feat(retrieve): add treasury fallback on persisted miss` | Files: [retrieveConverted use case + tests]

- [ ] 5. Implement Treasury adapter replacing dummy bean

  **What to do**: Implement new adapter class `ExchangeRateTreasuryAdapter` (name aligned with conventions) that maps Treasury payload to domain `ExchangeRate` values.
  **Must NOT do**: Do not keep ambiguous multiple `ExchangeRateClientPort` beans.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: adapter mapping and DI wiring.
  - Skills: `[kotlin-patterns]` - clean Kotlin mapping.
  - Omitted: `[tdd]` - strategy already covered globally.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 6,9 | Blocked By: none

  **References**:
  - Existing dummy: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/DummyExchangeRateClientAdapter.kt`
  - Domain model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt`

  **Acceptance Criteria**:
  - [ ] Spring injects real adapter for `ExchangeRateClientPort`.
  - [ ] Dummy adapter removed or disabled from runtime wiring.

  **QA Scenarios**:
  ```
  Scenario: Bean wiring resolution
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedControllerV1Test*" --tests "*PurchaseControllerV1Test*"
    Expected: Spring context starts and controller tests pass with resolved ExchangeRateClientPort wiring.
    Evidence: .sisyphus/evidence/task-5-wiring.txt

  Scenario: Mapping invalid rate payload
    Tool: Bash
    Steps: ./gradlew test --tests "*Treasury*Adapter*"
    Expected: Invalid payload mapped to controlled failure.
    Evidence: .sisyphus/evidence/task-5-wiring-error.txt
  ```

  **Commit**: YES | Message: `refactor(exchange): replace dummy adapter with treasury adapter` | Files: [external adapter files]

- [ ] 6. Persist fallback-selected rate (cache-by-write)

  **What to do**: When fallback finds eligible Treasury rate, persist it to `exchange_rates` via repository path (add outbound write capability if missing), then continue response composition.
  **Must NOT do**: Do not persist ineligible (>6 months or >purchase date) rates.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: domain/application + persistence port evolution.
  - Skills: `[clean-ddd-hexagonal]` - safe port extension.
  - Omitted: `[kotlin-springboot]` - primarily architecture/domain orchestration.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 9,10 | Blocked By: 2,3,4,5

  **References**:
  - Repository adapter: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt`
  - JPA repository/entity: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaRepository.kt`, `.../ExchangeRateJpaEntity.kt`

  **Acceptance Criteria**:
  - [ ] Fallback success writes selected rate to DB.
  - [ ] Subsequent retrieval can use persisted-first path.

  **QA Scenarios**:
  ```
  Scenario: Cache-by-write then reuse
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConverted*" --tests "*ExchangeRateRepository*"
    Expected: First retrieval writes; second retrieval uses persisted hit.
    Evidence: .sisyphus/evidence/task-6-cache-write.txt

  Scenario: Ineligible rate not persisted
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConverted*" --tests "*Repository*"
    Expected: No insert when rate violates window/date rule.
    Evidence: .sisyphus/evidence/task-6-cache-write-error.txt
  ```

  **Commit**: YES | Message: `feat(exchange): persist treasury fallback rates` | Files: [ports + adapters + tests]

- [ ] 7. Add deterministic Treasury selection algorithm tests

  **What to do**: Add focused tests proving selection of greatest `record_date` <= purchase date, including exact boundary at 6 months inclusive.
  **Must NOT do**: Do not rely on implicit sorting assumptions.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: focused test additions.
  - Skills: `[]` - straightforward.
  - Omitted: `[oracle]` - not design consultation.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 9 | Blocked By: 3

  **References**:
  - Use-case tests: `src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt`

  **Acceptance Criteria**:
  - [ ] Boundary inclusiveness covered (`purchaseDate.minusMonths(6)`).
  - [ ] Future-dated records ignored.

  **QA Scenarios**:
  ```
  Scenario: Boundary inclusive hit
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedUseCaseImplTest*"
    Expected: Rate exactly at minus-6-month date is accepted.
    Evidence: .sisyphus/evidence/task-7-boundary.txt

  Scenario: Boundary miss older than 6 months
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedUseCaseImplTest*"
    Expected: RateUnavailableException raised.
    Evidence: .sisyphus/evidence/task-7-boundary-error.txt
  ```

  **Commit**: YES | Message: `test(retrieve): cover historical selection boundaries` | Files: [retrieveConverted use-case tests]

- [ ] 8. Align error mapping for Treasury unavailability

  **What to do**: Ensure fallback no-eligible-rate path consistently throws `RateUnavailableException`; keep controller mapping to 422 unchanged.
  **Must NOT do**: Do not map transient infrastructure failures to same business error unless policy explicitly requires.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: exception semantics.
  - Skills: `[]` - existing pattern available.
  - Omitted: `[clean-ddd-hexagonal]` - no structural changes.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 10 | Blocked By: 4

  **References**:
  - Handler: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client/error/GlobalExceptionHandler.kt`
  - Domain exceptions: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/DomainException.kt`

  **Acceptance Criteria**:
  - [ ] 422 remains for conversion unavailable.
  - [ ] External transient failures mapped to non-business failure path.

  **QA Scenarios**:
  ```
  Scenario: No eligible rate returns 422
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedControllerV1Test*"
    Expected: 422 with existing problem detail shape.
    Evidence: .sisyphus/evidence/task-8-errors.txt

  Scenario: Treasury transient exhaustion
    Tool: Bash
    Steps: ./gradlew test --tests "*Treasury*Retry*" --tests "*Controller*"
    Expected: Not misclassified as RateUnavailableException.
    Evidence: .sisyphus/evidence/task-8-errors-transient.txt
  ```

  **Commit**: YES | Message: `fix(retrieve): stabilize treasury fallback error mapping` | Files: [handler/tests/use case]

- [ ] 9. Integration/API regression suite for fallback end-to-end

  **What to do**: Add integration/API tests covering persisted hit, persisted miss with Treasury success, and persisted miss with unavailable rate.
  **Must NOT do**: Do not introduce flaky timing dependencies.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: multi-layer verification.
  - Skills: `[kotlin-springboot]` - test wiring.
  - Omitted: `[playwright-cli]` - API-only.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 11 | Blocked By: 1,2,3,4,5,6,7

  **References**:
  - API tests: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1Test.kt`
  - Testcontainer base: `src/test/kotlin/com/charlesluxinger/wex_transactions/config/AbstractRestApiIntegrationTest.kt`

  **Acceptance Criteria**:
  - [ ] End-to-end fallback behavior proven.
  - [ ] Cache-by-write observable via second request path.

  **QA Scenarios**:
  ```
  Scenario: End-to-end fallback then cache hit
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedControllerV1Test*" --tests "*PersistenceRepositoryIntegrationTest*"
    Expected: First fallback succeeds; second call uses persisted path.
    Evidence: .sisyphus/evidence/task-9-e2e.txt

  Scenario: End-to-end unavailable conversion
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedControllerV1Test*"
    Expected: 422 response with expected message.
    Evidence: .sisyphus/evidence/task-9-e2e-error.txt
  ```

  **Commit**: YES | Message: `test(retrieve): add e2e treasury fallback scenarios` | Files: [controller + integration tests]

- [ ] 10. Architecture and dependency safety checks

  **What to do**: Ensure new classes respect package boundaries and architecture tests remain green.
  **Must NOT do**: Do not suppress architecture tests.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: verification-focused.
  - Skills: `[]`.
  - Omitted: `[oracle]` - not needed for execution.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 11 | Blocked By: 6,8

  **References**:
  - Architecture tests: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/*`

  **Acceptance Criteria**:
  - [ ] Dependency direction tests pass.
  - [ ] Controller boundary tests pass.

  **QA Scenarios**:
  ```
  Scenario: Architecture suite
    Tool: Bash
    Steps: ./gradlew test --tests "com.charlesluxinger.wex_transactions.architecture.*"
    Expected: All architecture tests pass.
    Evidence: .sisyphus/evidence/task-10-arch.txt

  Scenario: Use-case ownership
    Tool: Bash
    Steps: ./gradlew test --tests "*UseCaseOwnershipTest*"
    Expected: New/changed use cases stay in application/service.
    Evidence: .sisyphus/evidence/task-10-arch-error.txt
  ```

  **Commit**: YES | Message: `test(arch): verify boundaries after treasury integration` | Files: [architecture tests if needed]

- [ ] 11. Full quality gate run

  **What to do**: Run required local workflow order and fix issues.
  **Must NOT do**: Do not skip detekt/ktlint/test gates.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: final stabilization.
  - Skills: `[]`.
  - Omitted: `[review-work]` - final verification wave handles review.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: Final Verification | Blocked By: 9,10

  **References**:
  - Workflow rule: `AGENTS.md` local verification section
  - CI order: `.github/workflows/ci.yml`

  **Acceptance Criteria**:
  - [ ] ktlint check passes.
  - [ ] detekt passes.
  - [ ] tests pass.

  **QA Scenarios**:
  ```
  Scenario: Full local gates
    Tool: Bash
    Steps: ./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat && ./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck && ./gradlew detekt && ./gradlew test
    Expected: Exit code 0 all commands.
    Evidence: .sisyphus/evidence/task-11-gates.txt

  Scenario: Focused retrieval safety
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConverted*" --tests "*Treasury*"
    Expected: All retrieval/treasury tests green.
    Evidence: .sisyphus/evidence/task-11-gates-retrieve.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: [none]

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.
> Never mark F1-F4 as checked before getting user's okay.
- [ ] F1. Plan Compliance Audit — oracle
  ```
  Scenario: Plan-to-implementation conformance
    Tool: task (oracle)
    Steps: Run oracle review against executed changes versus this plan; verify each task acceptance criterion mapped to evidence file.
    Expected: Oracle returns APPROVE with no critical drift.
    Evidence: .sisyphus/evidence/f1-plan-compliance.md
  ```
- [ ] F2. Code Quality Review — unspecified-high
  ```
  Scenario: Static and structural quality pass
    Tool: task (unspecified-high)
    Steps: Review changed files for maintainability, error semantics, and retry correctness; validate detekt/ktlint outputs are clean.
    Expected: Reviewer returns APPROVE with no high-severity findings.
    Evidence: .sisyphus/evidence/f2-code-quality.md
  ```
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
  ```
  Scenario: End-to-end API behavior validation
    Tool: task (unspecified-high)
    Steps: Execute retrieval scenarios (persisted hit, treasury fallback hit, no eligible rate) using automated API test run and captured outputs.
    Expected: All scenarios pass; 422 only for true conversion-unavailable case.
    Evidence: .sisyphus/evidence/f3-manual-qa.md
  ```
- [ ] F4. Scope Fidelity Check — deep
  ```
  Scenario: Scope and boundary audit
    Tool: task (deep)
    Steps: Confirm no out-of-scope additions (country input, extra resilience layers, contract changes) and hexagonal boundaries preserved.
    Expected: Deep review returns APPROVE; no scope creep.
    Evidence: .sisyphus/evidence/f4-scope-fidelity.md
  ```

## Commit Strategy
- Small atomic commits per task group.
- Conventional commits:
  - `feat(exchange): add treasury feign client with retry timeout`
  - `feat(retrieve): add treasury fallback with cache-by-write`
  - `test(retrieve): add historical window and retry timeout scenarios`
  - `refactor(exchange): remove dummy adapter wiring`

## Success Criteria
- Real Treasury conversion used when persisted rate missing.
- Persisted-first behavior preserved.
- Cache-by-write works for fallback hits.
- 422 on unavailable eligible historical rate.
- Retry/timeout behavior deterministic and tested.
- Architecture and quality gates pass without waivers.
