# WEX FX Remediation — Critical Defects (Revised)

## TL;DR
> **Summary**: Refactor TargetCurrency to accept Treasury format (`country_currency_desc` like `"Canada-Dollar"`), update cache port for date-aware keys, fix 5 blocking FX bugs: wrong cached rate, cross-request contamination, missing validation, no rate limiter, non-protective fallback.
> **Deliverables**: Direct `country_currency_desc` input (no ISO mapping), date-aware cache key, boundary validation, explicit rate limiter (10 req/min), protective circuit breaker fallback.
> **Effort**: Large (7 tasks + signature refactoring across 3 layers)
> **Parallel**: YES — 6 waves
> **Critical Path**: Task 0 (model refactor) → Task 1 (cache key date-aware) → Tasks 2, 3, 4, 5 (parallel) → Task 6 (circuit breaker) → F1–F4 (verification)

## Context

### Original Request
Fix 5 critical defects preventing production deployment:
1. Wrong exchange rate cached (stale/mismatched date)
2. Cache contamination across requests (same pair, different dates)
3. No input validation on currency descriptor
4. No rate limiter on Treasury API calls (10 req/min limit required)
5. Non-protective circuit breaker fallback

### Momus Review Findings (Prior Session)
Momus identified 3 execution blockers:
1. **Scope mismatch**: Plan said "direct `country_currency_desc`" but codebase only accepts ISO-4217 (3-letter codes)
2. **Task 1 incomplete**: Cache key can't be date-aware without updating `ExchangeRateCachePort` signature AND all callers
3. **Task 6 incomplete**: Stale cache fallback requires new cache API method ("find latest by pair") not in original port

**User decision**: Refactor domain models (Task 0) to accept `country_currency_desc` directly throughout stack, eliminating ISO mapping entirely.

### Interview Summary
- **User scope decision**: NO ISO code mapping. User provides `country_currency_desc` directly (e.g., `"Canada-Dollar"`). Treasury API returns exact same format.
- **Cache scope**: KEEP Redis cache, FIX bugs in it (scope: B).
- **Test strategy**: TDD (RED-GREEN-REFACTOR per task). Testcontainers for integration tests (not Mockito).
- **All 5 defects**: Valid in real codebase (verified via explore agent).
- **Breaking change**: `TargetCurrency` refactored from ISO-only to `country_currency_desc`; port signatures updated to include `rateDate`.

### Treasury API Pattern
User provides `country_currency_desc` (exact Treasury field value):
```
GET /v1/accounting/od/rates_of_exchange?fields=country_currency_desc,exchange_rate,record_date&filter=country_currency_desc:in:(Canada-Dollar,Mexico-Peso),record_date:gte:2024-01-01
```
Response: `{"country_currency_desc": "Canada-Dollar", "exchange_rate": "1.426", "record_date": "2020-03-31"}`

## Work Objectives

### Core Objective
Refactor domain models to accept Treasury `country_currency_desc` format directly; fix 5 blocking FX bugs with production-grade resilience (date-aware cache, rate limiting, protective fallback).

### Deliverables
1. ✅ `TargetCurrency` accepts `country_currency_desc` (no ISO validation)
2. ✅ `ExchangeRateCachePort` includes `rateDate` in getRate/saveRate signatures
3. ✅ Cache key includes `record_date` (prevents cross-request contamination)
4. ✅ Input validation on `country_currency_desc` at HTTP boundary (before domain parsing)
5. ✅ Explicit rate limiter: 10 Treasury API calls per minute (Resilience4j)
6. ✅ Circuit breaker fallback: query latest cached rate before failing (protective degradation)
7. ✅ Event-driven cache publish decoupled from retrieval flow

### Definition of Done (verifiable commands)
```bash
# All tests green (including new scenarios)
./gradlew test

# No violations
./gradlew detekt
./gradlew ktlintMainSourceSetCheck

# Coverage maintained
./gradlew test jacocoTestReport
```

### Must Have
- Direct `country_currency_desc` input (no ISO mapping, no conversion)
- Date-aware cache key: `exchangeRate:{sourceCurrency}:{targetCurrency}:{rateDate}`
- `ExchangeRateCachePort` updated: `getRate(source, target, rateDate)` + `getLatestRate(source, target)`
- Rate limiter: **exactly 10 calls/min** (Resilience4j, not Guava)
- Boundary validation: `@NotBlank` on `country_currency_desc` parameter (HTTP 400 if invalid)
- Protective fallback: return most-recent cached rate if Treasury fails, not exception
- TDD per task: write RED test first, implement GREEN, refactor

### Must NOT Have
- ❌ ISO code mappings (eliminate entirely)
- ❌ Hardcoded currency lists
- ❌ Mockito for Treasury adapter tests (use Testcontainers or WireMock)
- ❌ Rate limiter per-call or per-second (must be per-minute)
- ❌ Copy-paste tests (Metis guardrail)
- ❌ Validation deferral (MUST validate at controller boundary)

## Verification Strategy

Test decision: **TDD + Integration (Testcontainers)**
- Unit: JUnit5 + Kotlin test, FakeClock for rate limiter validation
- Integration: RestAssured API tests, Testcontainers (Redis, Treasury mock)
- QA: Agent-executed scenarios (no manual verification)

Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy

### Parallel Execution Waves

**Wave 0** (Domain Model Refactoring — 1 task, foundation for all)
- Task 0: Refactor TargetCurrency to accept `country_currency_desc`; update ExchangeRateCachePort and all callers

**Wave 1** (Cache Infrastructure — 2 tasks, independent after Wave 0)
- Task 1: Fix cache key to include date (prevents cross-request contamination)
- Task 2: Event listener test suite (no defect yet, prep for Task 5 changes)

**Wave 2** (Cache Publish Logic — 1 task, depends on Wave 0, 1)
- Task 3: Fix event publish path (use rate date from Treasury response, not transaction date)

**Wave 3** (Validation + Rate Limiter — 2 tasks, independent after Wave 0)
- Task 4: Add boundary validation (country_currency_desc)
- Task 5: Add explicit rate limiter (10 req/min config)

**Wave 4** (Circuit Breaker Protective Fallback — 1 task)
- Task 6: Implement stale-cache fallback in circuit breaker

**Wave 5** (Final Verification — 4 agents, parallel)
- F1: Plan Compliance Audit (oracle)
- F2: Code Quality Review (unspecified-high)
- F3: Real QA + API Tests (unspecified-high + playwright)
- F4: Scope Fidelity Check (deep)

### Dependency Matrix

| Task | Blocks | Blocked By | Wave |
|------|--------|-----------|------|
| 0    | 1, 2, 3, 4, 5, 6 | —    | 0    |
| 1    | 2, 3   | 0         | 1    |
| 2    | —      | 0         | 1    |
| 3    | —      | 0, 1      | 2    |
| 4    | —      | 0         | 3    |
| 5    | —      | 0         | 3    |
| 6    | —      | 0, 1, 3, 5 | 4    |

### Agent Dispatch Summary

| Wave | Task Count | Categories |
|------|-----------|-----------|
| 0    | 1         | unspecified-high (1) |
| 1    | 2         | unspecified-high (2) |
| 2    | 1         | unspecified-high (1) |
| 3    | 2         | unspecified-high (2) |
| 4    | 1         | unspecified-high (1) |
| 5    | 4 (F1–F4) | oracle, unspecified-high (2), deep |

---

## TODOs

### Wave 0: Domain Model Refactoring

- [ ] 0. Refactor TargetCurrency and ExchangeRateCachePort for direct `country_currency_desc` input

  **What to do**:
  - **Part A: TargetCurrency refactor**
    - Replace ISO-4217 validation with simple non-blank validation: `@NotBlank` check + trim + no format restrictions
    - Remove `isIso4217()` method and `CODE_REGEX` (ISO-specific)
    - Update constructor to accept any non-blank string: `country_currency_desc` format (e.g., `"Canada-Dollar"`)
    - Keep equals/hashCode/toString unchanged
  - **Part B: ExchangeRateCachePort signature update**
    - Add `rateDate: LocalDate` parameter to `getRate(sourceCurrency, targetCurrency, rateDate): ExchangeRate?`
    - Add `rateDate: LocalDate` parameter to `saveRate(sourceCurrency, targetCurrency, rateDate, rate)`
    - **NEW**: Add `getLatestRate(sourceCurrency, targetCurrency): ExchangeRate?` for stale-cache fallback (searches cache ignoring date)
  - **Part C: Update all callers**
    - `RedisExchangeRateCacheAdapter`: update both methods to accept/use `rateDate` in key builder
    - `RetrieveConvertedUseCaseImpl`: pass `rateDate` to all `exchangeRateCachePort` calls (line 38); use `rate.retrievedAt.toLocalDate()`
    - `ExchangeRateFetchedEventListener`: call `cache.saveRate(..., rateDate, ...)` with date from event
  - **Part D: ExchangeRateTreasuryAdapter**
    - Remove `matchRate()` ISO-to-Treasury mapping logic (lines 58-72)
    - Use `country_currency_desc` directly from Treasury response to build ExchangeRate (no conversion needed)
  - Add unit tests: TargetCurrency accepts arbitrary non-blank strings; cache port methods work with date parameter

  **Must NOT do**:
  - ❌ Keep ISO validation
  - ❌ Create ISO-to-descriptor mapping
  - ❌ Defer date parameter to optional (must be required on all cache port methods)
  - ❌ Add ISO code field to TargetCurrency

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: breaking changes across 3 layers, signature refactoring, port contract update
  - Skills: [`kotlin-patterns`] — Why: immutable value objects, refactoring patterns, interface updates
  - Omitted: []

  **Parallelization**: Can Parallel: NO | Wave 0 | Blocks: 1, 2, 3, 4, 5, 6 | Blocked By: —

  **References** (executor has NO interview context — be exhaustive):
  - TargetCurrency model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/TargetCurrency.kt:1-30`
  - ExchangeRateCachePort: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt:1-17`
  - Cache adapter: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt:1-63`
  - Use case: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt:1-106`
  - Event listener: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt:1-70`
  - Treasury adapter: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt:1-99` (remove matchRate mapping)
  - Existing tests: `src/test/kotlin/com/charlesluxinger/wex_transactions/domain/model/TargetCurrencyTest.kt` (refactor validation tests)

  **Acceptance Criteria** (agent-executable only):
  - [ ] TargetCurrency accepts non-blank `country_currency_desc` (e.g., `"Canada-Dollar"`); verified: unit test passes
  - [ ] TargetCurrency rejects blank/null input; verified: unit test throws InvalidCurrencyException
  - [ ] ExchangeRateCachePort.getRate includes `rateDate` parameter; verified: `rtk grep "fun getRate" src/main/kotlin/.../ExchangeRateCachePort.kt | grep "rateDate"`
  - [ ] ExchangeRateCachePort.saveRate includes `rateDate` parameter; verified: `rtk grep "fun saveRate" src/main/kotlin/.../ExchangeRateCachePort.kt | grep "rateDate"`
  - [ ] ExchangeRateCachePort.getLatestRate method exists; verified: method signature found
  - [ ] All callers updated (use case, cache adapter, event listener) pass `rateDate`; verified: grep for calls to port methods
  - [ ] Treasury adapter uses `country_currency_desc` directly (no ISO mapping); verified: `matchRate()` method removed
  - [ ] All existing tests still pass (refactored for new signatures); verified: `./gradlew test --tests "*TargetCurrency*"`

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — TargetCurrency accepts Treasury format
    Tool: Interactive bash (unit test)
    Steps: Create TargetCurrency("Canada-Dollar"). Create TargetCurrency("Mexico-Peso").
    Expected: Both succeed without exception.
    Evidence: .sisyphus/evidence/task-0-targetcurrency-treasury-format.test-report.txt

  Scenario: Validation — blank input rejected
    Tool: Interactive bash (unit test)
    Steps: Try TargetCurrency(""). Try TargetCurrency("   ").
    Expected: Both throw InvalidCurrencyException.
    Evidence: .sisyphus/evidence/task-0-targetcurrency-blank-rejected.test-report.txt

  Scenario: Cache port signature — date parameter included
    Tool: Interactive bash (unit test)
    Steps: Call cache.getRate(source, target, rateDate). Call cache.saveRate(source, target, rateDate, rate).
    Expected: Both methods accept date parameter without compile error.
    Evidence: .sisyphus/evidence/task-0-cache-port-date-param.test-report.txt

  Scenario: Cache port new method — getLatestRate works
    Tool: Interactive bash (unit test)
    Steps: Call cache.getLatestRate(source, target) (ignores date, finds any cached rate for pair).
    Expected: Returns most recent cached rate for pair, or null if none.
    Evidence: .sisyphus/evidence/task-0-cache-latest-rate.test-report.txt

  Scenario: Integration — use case passes rateDate to cache
    Tool: RestAssured API test
    Steps: GET /api/v1/purchases/123/converted?targetCurrency=Canada-Dollar.
    Expected: Use case calls cache.getRate with rateDate parameter. No compile errors.
    Evidence: .sisyphus/evidence/task-0-usecase-cache-date.test-report.txt

  Scenario: Integration — Treasury adapter uses country_currency_desc directly
    Tool: Interactive bash (unit test)
    Steps: Feign client returns rate with country_currency_desc. Adapter builds ExchangeRate.
    Expected: ExchangeRate.targetCurrency == "Canada-Dollar" (no ISO conversion).
    Evidence: .sisyphus/evidence/task-0-treasury-no-iso-mapping.test-report.txt
  ```

  **Commit**: YES | Message: `refactor(domain): accept country_currency_desc directly; update cache port for date-aware keys` | Files: [`src/main/kotlin/.../TargetCurrency.kt`, `src/main/kotlin/.../ExchangeRateCachePort.kt`, `src/main/kotlin/.../RedisExchangeRateCacheAdapter.kt`, `src/main/kotlin/.../RetrieveConvertedUseCaseImpl.kt`, `src/main/kotlin/.../ExchangeRateFetchedEventListener.kt`, `src/main/kotlin/.../ExchangeRateTreasuryAdapter.kt`]

---

### Wave 1: Cache Infrastructure

- [ ] 1. Fix cache key to include date (prevents cross-request contamination)

  **What to do**:
  - Update `ExchangeRateCacheKeyBuilder.buildCacheKey()` to accept `rateDate: LocalDate` parameter (added in Task 0)
  - Generate key: `"exchangeRate:{sourceCurrency}:{targetCurrency}:{rateDate}"`
  - Verify all callers in `RedisExchangeRateCacheAdapter` pass `rateDate` (already updated in Task 0, verify correctness)
  - Add unit tests: same pair, different dates → different cache keys
  - Verify cache key format with specific dates (e.g., `"exchangeRate:USD:Canada-Dollar:2024-01-15"`)

  **Must NOT do**:
  - ❌ Keep old key format without date
  - ❌ Use only month/year (must include full date for precision)
  - ❌ Make date optional in key builder

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: verify cache key logic + atomic refactor across files (already done in Task 0; this is verification + test)
  - Skills: [`kotlin-patterns`] — Why: immutable value objects, cache key design
  - Omitted: []

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 2, 3 | Blocked By: 0

  **References** (executor has NO interview context — be exhaustive):
  - Current key builder: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt:5-9` (now accepts rateDate after Task 0)
  - Cache adapter getRate: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt:18-39` (now passes rateDate after Task 0)
  - Cache adapter saveRate: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt:41-63` (now passes rateDate after Task 0)
  - Existing test pattern: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapterTest.kt`

  **Acceptance Criteria** (agent-executable only):
  - [ ] Key builder accepts `rateDate: LocalDate` parameter; verified: `rtk grep "buildCacheKey.*rateDate" src/main/kotlin/.../ExchangeRateCacheKeyBuilder.kt`
  - [ ] Key format includes date: `exchangeRate:USD:Canada-Dollar:2024-01-15`; verified: unit test assertion
  - [ ] `getRate` and `saveRate` pass `rateDate` to key builder; verified: `rtk grep "buildCacheKey" src/main/kotlin/.../RedisExchangeRateCacheAdapter.kt` shows both calls include date
  - [ ] Same pair, different dates → different cache keys; unit test passes
  - [ ] Keys include full date (YYYY-MM-DD), not month/year; verified: test assertion on key format

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — cache key includes date
    Tool: Interactive bash (unit test)
    Steps: Call buildCacheKey(TargetCurrency("Canada-Dollar"), TargetCurrency("USD"), LocalDate.of(2024, 1, 15)).
    Expected: Returns "exchangeRate:Canada-Dollar:USD:2024-01-15".
    Evidence: .sisyphus/evidence/task-1-cache-key.test-report.txt

  Scenario: Different dates produce different keys
    Tool: Interactive bash (unit test)
    Steps: Call buildCacheKey with same currencies, dates 2024-01-15 and 2024-01-16.
    Expected: Returns two different keys; both contain respective dates.
    Evidence: .sisyphus/evidence/task-1-cache-key-different-dates.test-report.txt

  Scenario: Integration — cache stores by date
    Tool: RestAssured API test (with Redis)
    Steps: (1) GET /api/v1/purchases/123/converted?targetCurrency=Canada-Dollar on 2024-01-15. (2) Fetch same pair on 2024-01-16.
    Expected: Separate cache entries. Rate for 2024-01-16 not overwritten by 2024-01-15 rate.
    Evidence: .sisyphus/evidence/task-1-cache-date-separation.test-report.txt
  ```

  **Commit**: YES | Message: `fix(cache): add date to cache key to prevent cross-request contamination` | Files: [`src/main/kotlin/.../ExchangeRateCacheKeyBuilder.kt`, `src/main/kotlin/.../RedisExchangeRateCacheAdapter.kt`]

---

- [ ] 2. Prep: Event listener test suite (no defect yet, foundation for Task 5)

  **What to do**:
  - Review existing `ExchangeRateFetchedEventListener` (now expects `rateDate` parameter in event, added in Task 0)
  - Create comprehensive unit test suite covering:
    - Happy path: event published → rate saved to cache with date-aware key
    - Failure path: cache write fails → exception caught, logged, not re-thrown
    - Serialization: ExchangeRate → ExchangeRateCacheValue → JSON roundtrip
  - Tests will use FakeClock / mocked RedisTemplate and cache port
  - No code changes to listener itself in this task (preparation only; Task 0 already updated it)

  **Must NOT do**:
  - ❌ Modify listener logic (Task 0 already did; this is testing only)
  - ❌ Skip failure scenarios (cache failures must not bubble up)
  - ❌ Use real Redis (must mock port or StringRedisTemplate)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: event listener testing, mocking, async flow validation
  - Skills: [`kotlin-springboot`] — Why: Spring event handling, @EventListener patterns
  - Omitted: []

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: 0

  **References** (executor has NO interview context — be exhaustive):
  - Event listener: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt:1-70` (now includes rateDate in saveRate call after Task 0)
  - Event model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt`
  - Cache port (for mocking): `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` (now with rateDate param after Task 0)
  - Existing listener test pattern: Check if `ExchangeRateFetchedEventListenerTest.kt` exists; if not, use `RedisExchangeRateCacheAdapterTest.kt` as mock/spy pattern reference

  **Acceptance Criteria** (agent-executable only):
  - [ ] Unit test suite created: `ExchangeRateFetchedEventListenerTest.kt`; verified: `ls src/test/kotlin/.../ExchangeRateFetchedEventListenerTest.kt`
  - [ ] Happy path test: event published → cache.saveRate called with correct params including rateDate; verified: test passes with mock spy assertion
  - [ ] Failure path test: cache.saveRate throws → exception logged, not re-thrown; verified: test passes, logger.warn invoked
  - [ ] All tests green: `./gradlew test --tests "*ExchangeRateFetchedEventListener*"`

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — event triggers cache save
    Tool: Interactive bash (unit test)
    Steps: Fire ExchangeRateFetchedEvent with sourceCurrency=Canada-Dollar, targetCurrency=USD, rate=1.25, rateDate=2024-01-15.
    Expected: Mock ExchangeRateCachePort.saveRate called once with matching params (including rateDate).
    Evidence: .sisyphus/evidence/task-2-event-cache-save.test-report.txt

  Scenario: Failure path — cache exception handled
    Tool: Interactive bash (unit test)
    Steps: Fire event with mocked saveRate throwing IOException. Check listener response.
    Expected: No exception propagated; logger.warn called with failure details.
    Evidence: .sisyphus/evidence/task-2-event-cache-error.test-report.txt

  Scenario: Serialization round-trip
    Tool: Interactive bash (unit test)
    Steps: Convert ExchangeRate → ExchangeRateCacheValue → JSON string → back to object.
    Expected: All fields preserved; no data loss.
    Evidence: .sisyphus/evidence/task-2-event-serialization.test-report.txt

  Scenario: Date parameter passed correctly
    Tool: Interactive bash (unit test)
    Steps: Fire event with rateDate=2024-01-15. Verify listener calls cache.saveRate with that date.
    Expected: Mock receives saveRate(..., rateDate=2024-01-15).
    Evidence: .sisyphus/evidence/task-2-event-ratedate-param.test-report.txt
  ```

  **Commit**: YES | Message: `test(event-listener): add comprehensive test suite for cache event handler` | Files: [`src/test/kotlin/.../ExchangeRateFetchedEventListenerTest.kt`]

---

### Wave 2: Cache Publish Logic

- [ ] 3. Fix event publish path (remove mismatched date from event)

  **What to do**:
  - Current code (lines 73-86 in `RetrieveConvertedUseCaseImpl`):
    ```kotlin
    private fun publishToCache(rate: ExchangeRate, purchase: Purchase) {
        exchangeRateEventPort.publish(
            ExchangeRateFetchedEvent(
                rateDate = purchase.transactionDate.value.toLocalDate(),  // ← WRONG: transaction date, not rate date
            )
        )
    }
    ```
  - **Fix**: Use `rate.retrievedAt` (Treasury response date) instead of purchase transaction date
  - Update: `rateDate = rate.retrievedAt.toLocalDate()` (rate's own date, not purchase's)
  - Task 0 may have already started this change; verify and complete if needed
  - Add unit test: verify event contains correct rate date, not purchase date

  **Must NOT do**:
  - ❌ Continue using `purchase.transactionDate`
  - ❌ Defer date correction to listener (fix at source)
  - ❌ Use different fields (must use `rate.retrievedAt`)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: event model fix, domain semantics
  - Skills: [`kotlin-patterns`] — Why: proper event construction, temporal correctness
  - Omitted: []

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: — | Blocked By: 0, 1

  **References** (executor has NO interview context — be exhaustive):
  - Current publish method: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt:73-86`
  - ExchangeRate model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt` (check `retrievedAt` field)
  - ExchangeRateFetchedEvent: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt` (check `rateDate` field definition)
  - Event listener (where rate date is used): `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt:50-62` (now uses rateDate from event, thanks to Task 0)
  - Existing use case test: `src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt`

  **Acceptance Criteria** (agent-executable only):
  - [ ] Event publish uses `rate.retrievedAt`, not `purchase.transactionDate`; verified: `rtk grep "rateDate.*rate.retrievedAt" src/main/kotlin/.../RetrieveConvertedUseCaseImpl.kt`
  - [ ] Event rateDate matches Treasury response date; unit test: event.rateDate == rate.retrievedAt.toLocalDate()
  - [ ] Cache key derived from event rateDate (Wave 1 integration): rate saved under correct date-aware key; verified: integration test

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — event rateDate matches rate.retrievedAt
    Tool: Interactive bash (unit test)
    Steps: Create ExchangeRate with retrievedAt=2024-01-20T10:30:00. Call publishToCache. Capture fired event.
    Expected: event.rateDate == LocalDate.of(2024, 1, 20).
    Evidence: .sisyphus/evidence/task-3-event-rate-date.test-report.txt

  Scenario: Difference validation — event date ≠ purchase date
    Tool: Interactive bash (unit test)
    Steps: Create Purchase with transactionDate=2024-01-15. Create Rate with retrievedAt=2024-01-20. Publish event.
    Expected: event.rateDate == 2024-01-20, NOT 2024-01-15.
    Evidence: .sisyphus/evidence/task-3-event-date-separation.test-report.txt

  Scenario: Integration — event triggers cache with correct date key
    Tool: RestAssured API test (with Redis mock)
    Steps: Retrieve rate for purchase on 2024-01-15; Treasury returns rate from 2024-01-20. Event published.
    Expected: Cache stores under key containing 2024-01-20, not 2024-01-15.
    Evidence: .sisyphus/evidence/task-3-event-cache-date-key.test-report.txt
  ```

  **Commit**: YES | Message: `fix(event): use rate date instead of transaction date for cache key` | Files: [`src/main/kotlin/.../RetrieveConvertedUseCaseImpl.kt`]

---

### Wave 3: Validation + Rate Limiter

- [ ] 4. Add boundary validation on country_currency_desc (HTTP 400 for invalid input)

  **What to do**:
  - Add `@NotBlank` constraint annotation to `targetCurrency` parameter in `RetrieveConvertedControllerV1` (line 25)
  - Validation happens at HTTP binding layer (before DTO creation)
  - Invalid input → HTTP 400 with validation error details (RFC 7807 Problem Details format)
  - Add integration test: invalid descriptor → HTTP 400 with error message

  **Must NOT do**:
  - ❌ Validate only inside domain layer (too late; should fail at boundary)
  - ❌ Silently accept blank/null input
  - ❌ Defer to TargetCurrency constructor (controller must reject first; Task 0 handles second level)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: Spring validation, error handling, API contracts
  - Skills: [`kotlin-springboot`] — Why: `@Valid`, `@NotBlank`, constraint annotations, error response formatting
  - Omitted: []

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: — | Blocked By: 0

  **References** (executor has NO interview context — be exhaustive):
  - Controller endpoint: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1.kt:19-30`
  - DTO (no changes needed): `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/inbound/retrieveConverted/model/RetrieveConvertedQuery.kt:3-6`
  - Domain model (validates after boundary): `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/TargetCurrency.kt:5-14` (refactored in Task 0)
  - Error handling spec: `.specs/features/wex-tech-challenge/DECISIONS.md:113-135` (RFC 7807 format)
  - Existing controller test: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt`

  **Acceptance Criteria** (agent-executable only):
  - [ ] `@NotBlank` annotation added to `targetCurrency` parameter; verified: `rtk grep "@NotBlank" src/main/kotlin/.../RetrieveConvertedControllerV1.kt | grep targetCurrency`
  - [ ] Blank targetCurrency → HTTP 400; integration test passes
  - [ ] Error response includes field name and constraint message; API test: response body contains `"targetCurrency"` + `"must not be blank"`
  - [ ] Valid targetCurrency → HTTP 200; happy path test passes

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — valid targetCurrency
    Tool: RestAssured API test
    Steps: GET /api/v1/purchases/123/converted?targetCurrency=Canada-Dollar.
    Expected: HTTP 200. Response includes converted amount.
    Evidence: .sisyphus/evidence/task-4-validation-happy.test-report.txt

  Scenario: Invalid input — blank targetCurrency
    Tool: RestAssured API test
    Steps: GET /api/v1/purchases/123/converted?targetCurrency= (empty string).
    Expected: HTTP 400. Error response in RFC 7807 format with field=targetCurrency, message mentions "blank".
    Evidence: .sisyphus/evidence/task-4-validation-blank.test-report.txt

  Scenario: Invalid input — missing targetCurrency parameter
    Tool: RestAssured API test
    Steps: GET /api/v1/purchases/123/converted (no targetCurrency query param).
    Expected: HTTP 400. Error response indicates required parameter missing.
    Evidence: .sisyphus/evidence/task-4-validation-missing.test-report.txt

  Scenario: Error response format
    Tool: RestAssured API test
    Steps: GET with invalid targetCurrency. Parse response.
    Expected: Response body includes type, title, status, detail, instance per RFC 7807.
    Evidence: .sisyphus/evidence/task-4-validation-error-format.test-report.txt
  ```

  **Commit**: YES | Message: `feat(validation): add boundary validation for targetCurrency parameter` | Files: [`src/main/kotlin/.../RetrieveConvertedControllerV1.kt`]

---

- [ ] 5. Add explicit rate limiter (10 Treasury API calls/min, Resilience4j)

  **What to do**:
  - Add `@RateLimiter` annotation to `fetchNearestPriorRate()` in `ExchangeRateTreasuryAdapter` (line 28)
  - Config in `application.yaml`: `resilience4j.ratelimiter.instances.treasury-rates.limitRefreshPeriod: 1m` and `limitForPeriod: 10`
  - Rate limiter: **exactly 10 calls per minute** (not per second; not per-request threshold)
  - Rate limit exceeded → HTTP 429 (Too Many Requests) with Retry-After header
  - Add unit test: mock time advancement, verify 11th call fails with RateLimiterUnavailableException
  - Add integration test: fire 10 requests, 11th blocked with HTTP 429

  **Must NOT do**:
  - ❌ Use Guava rate limiter (must use Resilience4j)
  - ❌ Set limit per-second (must be per-minute)
  - ❌ Forget to configure in `application.yaml` (YAML config required, not code defaults)
  - ❌ Silently drop excess requests (must throw/reject with 429)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: Resilience4j configuration, rate limiting semantics, HTTP error response
  - Skills: [`kotlin-springboot`] — Why: Resilience4j annotations, application.yaml config, Spring error handling
  - Omitted: []

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: — | Blocked By: 0

  **References** (executor has NO interview context — be exhaustive):
  - Treasury adapter: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt:28-56`
  - Resilience4j rate limiter docs: https://resilience4j.readme.io/docs/ratelimiter (10-call-per-minute config pattern)
  - Application config: `src/main/resources/application.yaml` (add resilience4j.ratelimiter section)
  - Resilience4j existing config (for reference): check if `resilience4j.circuitbreaker` or `resilience4j.bulkhead` sections exist in application.yaml
  - Existing adapter test: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/treasury/ExchangeRateTreasuryAdapterTest.kt`

  **Acceptance Criteria** (agent-executable only):
  - [ ] `@RateLimiter(name = "treasury-rates")` annotation added to `fetchNearestPriorRate`; verified: `rtk grep "@RateLimiter" src/main/kotlin/.../ExchangeRateTreasuryAdapter.kt`
  - [ ] `application.yaml` contains: `resilience4j.ratelimiter.instances.treasury-rates.limitForPeriod: 10` and `limitRefreshPeriod: 1m`; verified: `rtk grep -A2 "treasury-rates" src/main/resources/application.yaml`
  - [ ] Unit test: 11th call within same minute → RateLimiterUnavailableException thrown; test uses FakeClock or time mock
  - [ ] Integration test: HTTP 429 response on rate limit exceeded; response includes Retry-After header

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — 10 calls within limit
    Tool: Interactive bash (unit test with time mock)
    Steps: Simulate 10 Treasury API calls within 1 minute window (FakeClock.advance(1 second) between calls).
    Expected: All 10 calls succeed.
    Evidence: .sisyphus/evidence/task-5-ratelimit-10-calls.test-report.txt

  Scenario: Rate limit exceeded — 11th call rejected
    Tool: Interactive bash (unit test with FakeClock)
    Steps: Simulate 10 calls, then call fetchNearestPriorRate 11th time (still within same 1-min window).
    Expected: 11th call throws RateLimiterUnavailableException. No HTTP call made to Treasury.
    Evidence: .sisyphus/evidence/task-5-ratelimit-exceeded.test-report.txt

  Scenario: Rate limit reset — new window allows 10 more calls
    Tool: Interactive bash (unit test with FakeClock)
    Steps: Simulate 10 calls. Advance FakeClock by 61 seconds (past 1-min window). Call fetchNearestPriorRate again.
    Expected: Call succeeds (new window). Counter reset.
    Evidence: .sisyphus/evidence/task-5-ratelimit-window-reset.test-report.txt

  Scenario: HTTP 429 response on rate limit exceeded
    Tool: RestAssured API test (with rate limiter mocked to fire after 1 call)
    Steps: GET /api/v1/purchases/123/converted?targetCurrency=Canada-Dollar twice in rapid succession.
    Expected: First call HTTP 200. Second call HTTP 429. Response includes Retry-After header.
    Evidence: .sisyphus/evidence/task-5-ratelimit-http-429.test-report.txt
  ```

  **Commit**: YES | Message: `feat(resilience): add rate limiter (10 req/min) to Treasury adapter` | Files: [`src/main/kotlin/.../ExchangeRateTreasuryAdapter.kt`, `src/main/resources/application.yaml`]

---

### Wave 4: Circuit Breaker Protective Fallback

- [ ] 6. Implement protective circuit breaker fallback (return stale cache instead of failing)

  **What to do**:
  - Current fallback (line 74-88 in `ExchangeRateTreasuryAdapter`): immediately throws `RateUnavailableException`
  - **New fallback**: attempt to return most-recent cached rate (stale cache) before throwing
  - Inject `ExchangeRateCachePort` into `ExchangeRateTreasuryAdapter` constructor
  - Logic: query cache using **new `getLatestRate(sourceCurrency, targetCurrency)` method** (added in Task 0); if found, return it; if not found, throw
  - Add unit test: circuit open + cache hit → return stale rate (no exception)
  - Add unit test: circuit open + cache miss → throw exception
  - Add integration test: Treasury timeout → HTTP 200 with stale rate (if cached)

  **Must NOT do**:
  - ❌ Throw immediately without attempting cache lookup
  - ❌ Ignore stale cache (stale is better than failure)
  - ❌ Break circuit breaker configuration (keep `@CircuitBreaker` annotation)
  - ❌ Update cache with stale data (return it as-is, don't re-save)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: resilience patterns, fallback logic, cache integration
  - Skills: [`kotlin-patterns`] — Why: defensive programming, graceful degradation, dependency injection
  - Omitted: []

  **Parallelization**: Can Parallel: NO | Wave 4 | Blocks: — | Blocked By: 0, 1, 3, 5

  **References** (executor has NO interview context — be exhaustive):
  - Current fallback: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt:74-88`
  - Adapter constructor: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt:16-18` (add cache port injection)
  - Cache port interface: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` (review getLatestRate method added in Task 0)
  - Resilience4j fallback docs: https://resilience4j.readme.io/docs/circuitbreaker#create-and-configure-a-circuit-breaker (fallback pattern)
  - Existing adapter test: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/external/treasury/ExchangeRateTreasuryAdapterTest.kt`

  **Acceptance Criteria** (agent-executable only):
  - [ ] Cache port injected into `ExchangeRateTreasuryAdapter`; verified: `rtk grep "private val exchangeRateCachePort" src/main/kotlin/.../ExchangeRateTreasuryAdapter.kt`
  - [ ] Fallback attempts cache lookup using getLatestRate before throwing; verified: `rtk grep "exchangeRateCachePort.getLatestRate" src/main/kotlin/.../ExchangeRateTreasuryAdapter.kt`
  - [ ] Circuit open + cache hit → returns stale rate (no exception); unit test passes
  - [ ] Circuit open + cache miss → throws RateUnavailableException; unit test passes
  - [ ] Integration test: Treasury timeout → HTTP 200 with stale rate (if available)

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Happy path — circuit open, stale cache available
    Tool: Interactive bash (unit test with mocked cache)
    Steps: Set circuit breaker to OPEN state. Mock cache.getLatestRate to return stale rate (old but valid).
    Expected: fallback returns stale rate. No exception thrown.
    Evidence: .sisyphus/evidence/task-6-circuit-fallback-cache-hit.test-report.txt

  Scenario: Failure path — circuit open, no cache available
    Tool: Interactive bash (unit test with mocked cache)
    Steps: Set circuit breaker to OPEN state. Mock cache.getLatestRate to return null.
    Expected: fallback throws RateUnavailableException.
    Evidence: .sisyphus/evidence/task-6-circuit-fallback-cache-miss.test-report.txt

  Scenario: Integration — Treasury timeout, stale cache fallback
    Tool: RestAssured API test (with Testcontainers Treasury mock that times out)
    Steps: GET /api/v1/purchases/123/converted?targetCurrency=Canada-Dollar. Treasury mock times out (circuit opens).
    Expected: HTTP 200 (not 500). Response includes stale rate from cache.
    Evidence: .sisyphus/evidence/task-6-circuit-fallback-integration.test-report.txt

  Scenario: Graceful degradation — stale rate is better than failure
    Tool: RestAssured API test
    Steps: Retrieve rate normally (cache populated). Wait. Retrieve again after Treasury service down.
    Expected: Second retrieval returns stale rate (old but valid exchange rate), not error.
    Evidence: .sisyphus/evidence/task-6-graceful-degradation.test-report.txt
  ```

  **Commit**: YES | Message: `feat(resilience): add protective fallback (stale cache) to circuit breaker` | Files: [`src/main/kotlin/.../ExchangeRateTreasuryAdapter.kt`]

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before marking work complete.

**Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**

- [ ] F1. Plan Compliance Audit — oracle
  - Verify all 5 defects fixed in code
  - Verify Task 0 refactoring complete (TargetCurrency accepts country_currency_desc, cache port has rateDate)
  - Verify cache key includes date (Task 1)
  - Verify validation at boundary (Task 4)
  - Verify rate limiter config is exactly 10/min (Task 5)
  - Verify circuit breaker fallback returns stale cache (Task 6)
  - Check: zero ISO code mappings remain (per scope)

- [ ] F2. Code Quality Review — unspecified-high
  - Static analysis: `./gradlew detekt` passes
  - Formatting: `./gradlew ktlintMainSourceSetCheck` passes
  - No copy-paste tests (Metis guardrail)
  - All error paths logged (no silent failures)

- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
  - Execute all 6 task QA scenarios
  - Capture evidence artifacts
  - Verify all HTTP contracts (200, 400, 429, etc.)
  - Test Treasury API integration (Testcontainers mock)

- [ ] F4. Scope Fidelity Check — deep
  - Verify NO ISO code mappings in code (Task 0 eliminated entirely)
  - Verify user input is `country_currency_desc` (Treasury format, Task 0)
  - Verify cache scope kept (B: fix bugs, not remove, Task 1)
  - Verify all 5 original defects addressed (Tasks 1–6)
  - Verify cache port signature updated with rateDate and getLatestRate (Task 0)

## Commit Strategy

Each Wave task commits atomically:
- Task 0: `refactor(domain): accept country_currency_desc directly; update cache port for date-aware keys`
- Task 1: `fix(cache): add date to cache key to prevent cross-request contamination`
- Task 2: `test(event-listener): add comprehensive test suite for cache event handler`
- Task 3: `fix(event): use rate date instead of transaction date for cache key`
- Task 4: `feat(validation): add boundary validation for targetCurrency parameter`
- Task 5: `feat(resilience): add rate limiter (10 req/min) to Treasury adapter`
- Task 6: `feat(resilience): add protective fallback (stale cache) to circuit breaker`

## Success Criteria

✅ Task 0 (Breaking Change): Domain models refactored
- TargetCurrency accepts `country_currency_desc` directly (no ISO validation)
- ExchangeRateCachePort includes `rateDate` in getRate/saveRate + new getLatestRate method
- All callers (use case, cache adapter, event listener, Treasury adapter) updated
- Zero ISO code mappings remain in codebase

✅ All 5 defects fixed:
1. Cache key date-aware (no cross-request contamination)
2. Event publish uses rate date (not transaction date)
3. Input validation at HTTP boundary (targetCurrency @NotBlank)
4. Explicit rate limiter: exactly 10 calls/min via Resilience4j
5. Protective circuit breaker fallback (return stale cache before failing)

✅ Verification:
- `./gradlew test` — all tests green (including new scenarios)
- `./gradlew detekt` — no violations
- `./gradlew ktlintMainSourceSetCheck` — formatting correct
- Coverage maintained/improved

✅ No regressions:
- All existing tests pass
- No ISO code mappings introduced
- User provides `country_currency_desc` directly (no conversion)
- Redis cache KEPT (bugs fixed, not removed)
