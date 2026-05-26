# Coverage Remediation Plan

## TL;DR
> **Summary**: Fix all 7 identified coverage and test issues: fix misleading test name, add missing branch coverage for 7 domain branches + 5 companion instructions, remove PortContractTest, consolidate overlapping validation tests, add PRA save() test.
> **Deliverables**: Domain reaches true 100% instruction + branch coverage; project stays ≥95%; redundant tests removed; validation consolidated with assertion-preserving merge.
> **Effort**: Short (8-12 tasks, well-bounded)
> **Parallel**: YES — 3 independent waves
> **Critical Path**: Domain coverage fixes → Cleanup → Verification

## Context
### Original Request
"Review all tests if descriptions are compatible with implementations, verify if there are redundant tests, ensure domain has 100% code and branch coverage, ensure overall project coverage >= 90%."

### Interview Summary
- Full codebase audit completed: 18 domain sources, 7 domain tests, all app/infra sources + tests, 6 architecture tests
- `./gradlew test jacocoTestReport jacocoTestCoverageVerification` — BUILD SUCCESSFUL
- Coverage analysis via JaCoCo CSV + HTML identified specific missed branches/instructions
- User selected "Full cleanup" scope (domain + app + infra fixes)

### Metis Review (gaps addressed)
- Scope drift risk: mitigated by explicit "Full cleanup" decision
- False redundancy risk: mitigated by assertion matrix in task before removal
- Synthetic Kotlin branches: verified via JaCoCo HTML per-class reports (all missed branches are real logic paths)
- JaCoCo `includes` pattern quirk: `com.charlesluxinger.wex_transactions.domain.*` does NOT match subpackages (`domain.model.*`, etc.) — rule is a no-op. Fix is included as optional task.
- Branch coverage not enforced in JaCoCo config — plan treats it as manual goal, config fix optional

## Work Objectives
### Core Objective
Achieve true 100% code AND branch coverage in the domain layer, fix test/description mismatches, remove redundant tests, and consolidate overlapping validation.

### Deliverables
1. All domain classes show 0 missed instructions AND 0 missed branches in JaCoCo CSV
2. No test names contradict production constants (SCALE=2, not 6)
3. `PortContractTest` deleted or merged (proven no unique assertions lost)
4. Validation tests consolidated — no 3-way scenario duplication
5. `PurchaseRepositoryJPAAdapter.save()` covered by dedicated unit test

### Definition of Done (verifiable conditions with commands)
```powershell
# CI-equivalent command
./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification

# Verify domain class-level coverage — ALL must show 0 missed instructions AND 0 missed branches
# Check in build/reports/jacoco/test/jacocoTestReport.csv

# Verify test name fix
Select-String -Path "src/test/kotlin/**/ExchangeRateTest.kt" -Pattern "decimal places"
# Must contain "2 decimal places" and NOT contain "6 decimal places"

# Verify PortContractTest removed
Test-Path "src/test/kotlin/**/PortContractTest.kt"
# Must be False

# Verify no 3-way duplication remains
# StorePurchaseUseCaseImplTest: blank, long, zero (3 tests)
# PurchaseControllerV1ValidationTest: deleted (scenarios covered by use case test)
# PurchaseControllerV1Test: keeps integration-level scenarios
```

### Must Have
- Domain layer: 100% instruction + 100% branch coverage
- Test names must match actual SCALE constants
- JaCoCo build must pass with 0 violations
- All cleanup removals must prove no assertion loss
- Evidence: JaCoCo CSV + HTML reports showing per-class 0/0

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- NO production code changes (test-only plan)
- NO new features or behavior changes
- NO changing ExchangeRate SCALE (keep 2, fix test name)
- NO removing tests without verifying unique assertions are preserved
- NO modifying JaCoCo verification thresholds (only `includes` pattern if needed)
- NO replacing JUnit5/Mockito/AssertJ patterns with different frameworks
- NO restructuring module packages

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- **Test decision**: TDD — add tests first (RED), verify they fail for right reason (GREEN: compilation + expected exception)
- **Framework**: JUnit5 + kotlin.test (existing project setup)
- **QA policy**: Every task has agent-executed scenarios with specific selectors/data
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}` per task
- **Final gate**: CI-equivalent command + JaCoCo CSV class-level inspection

## Execution Strategy

> **Prerequisite**: Before starting any task, run `./gradlew test jacocoTestReport` to generate the JaCoCo CSV and HTML reports. All task references to `build/reports/jacoco/test/jacocoTestReport.csv` and `build/reports/jacoco/test/html/` depend on these artifacts.

### Parallel Execution Waves

**Wave 1** (Domain coverage fixes — 6 parallel tasks):
- Task 1: Fix ExchangeRateTest name
- Task 2: Add TransactionDate ZonedDateTime test
- Task 3: Add TransactionDate equals different-type test
- Task 4: Add ExchangeRate equals branch tests
- Task 5: Add Purchase equals different-type test
- Task 6: Add TargetCurrency equals different-type test

**Wave 2** (Cleanup — 3 tasks, sequential within group):
- Task 7: Remove PortContractTest
- Task 8: Consolidate validation tests
- Task 9: Add PurchaseRepositoryJPAAdapter.save() test

**Wave 3** (Final verification):
- Tasks F1-F4: review agents

### Dependency Matrix (full, all tasks)
| Task | Blocks | Blocked By |
|------|--------|------------|
| 1-6 | 7-9 | — |
| 7 | 8 | 1-6 (optional, not hard) |
| 8 | F1-F4 | 7 |
| 9 | F1-F4 | 1-6 |
| F1-F4 | — | 1-9 |

## TODOs

- [ ] 1. Fix ExchangeRateTest test name — "6 decimal places" → "2 decimal places"

  **What to do**: Change test name in `ExchangeRateTest.kt` line 48 from `` `should round to 6 decimal places` `` to `` `should round to 2 decimal places` ``. Also update line 102 test name from `` `should handle very large rates with 6 decimals` `` to `` `should handle very large rates` `` (scale-independent description). Do NOT change any production constants — `ExchangeRate` companion `SCALE = 2` stays unchanged.

  **Must NOT do**: Do not change `SCALE` constant. Do not modify assertions or test logic.

  **Recommended Agent Profile**:
  - Category: `quick` — single-line string change in test file

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: —

  **References**:
  - Source: `src/main/kotlin/.../domain/model/ExchangeRate.kt:34` — SCALE = 2
  - Test: `src/test/kotlin/.../domain/ExchangeRateTest.kt:48` — line to change
  - Test: `src/test/kotlin/.../domain/ExchangeRateTest.kt:102` — line to change

  **Acceptance Criteria**:
  - [ ] `Select-String -Path "src/test/kotlin/**/ExchangeRateTest.kt" -Pattern "6 decimal"` returns 0 matches
  - [ ] `Select-String -Path "src/test/kotlin/**/ExchangeRateTest.kt" -Pattern "2 decimal"` returns exactly 1 match

  **QA Scenarios**:
  ```
  Scenario: Test name matches SCALE constant
    Tool: Bash
    Steps: grep for "6 decimal" in ExchangeRateTest.kt
    Expected: 0 matches found
    Evidence: .sisyphus/evidence/task-1-name-check.txt

  Scenario: New name reflects actual scale
    Tool: Bash
    Steps: grep for "2 decimal" in ExchangeRateTest.kt
    Expected: exactly 1 match
    Evidence: .sisyphus/evidence/task-1-name-fixed.txt
  ```

  **Commit**: YES | Message: `test: fix ExchangeRate test names to reflect SCALE=2` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/ExchangeRateTest.kt`]

- [ ] 2. Add TransactionDate ZonedDateTime test case

  **What to do**: Add a new test to `TransactionDateTest.kt` that exercises the second `recoverCatching` block in `TransactionDate.parse()`. Use an input string with ZonedDateTime format including zone ID: `"2026-01-10T15:30:45+02:00[Europe/Paris]"`. Verify the parsed value matches `LocalDateTime.of(2026, 1, 10, 15, 30, 45)`.

  Method signature: `fun `should accept ISO-8601 date with timezone and zone ID`()`

  **Must NOT do**: Do not modify TransactionDate.kt production code. Do not change existing tests.

  **Recommended Agent Profile**:
  - Category: `quick` — single test method addition

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: —

  **References**:
  - Source: `src/main/kotlin/.../domain/model/TransactionDate.kt:31-36` — ZonedDateTime parsing path
  - Test: `src/test/kotlin/.../domain/TransactionDateTest.kt:15-19` — existing OffsetDateTime test (pattern to follow)

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*TransactionDate*"` passes
  - [ ] JaCoCo CSV `TransactionDate.Companion` shows 0 missed instructions (down from 5)

  **QA Scenarios**:
  ```
  Scenario: ZonedDateTime format parses correctly
    Tool: Bash
    Steps: add test with "2026-01-10T15:30:45+02:00[Europe/Paris]", run test
    Expected: test passes, value matches LocalDateTime(2026, 1, 10, 15, 30, 45)
    Evidence: .sisyphus/evidence/task-2-zone-test-pass.txt

  Scenario: Pre-existing tests still pass
    Tool: Bash
    Steps: ./gradlew test --tests "*TransactionDate*"
    Expected: all tests green
    Evidence: .sisyphus/evidence/task-2-all-green.txt
  ```

  **Commit**: YES | Message: `test: add ZonedDateTime branch coverage for TransactionDate.parse()` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/TransactionDateTest.kt`]

- [ ] 3. Add TransactionDate equals different-type branch test

  **What to do**: Add a test to `TransactionDateTest.kt` that covers the missed branch in `TransactionDate.equals()` where `other !is TransactionDate`. The JaCoCo HTML report shows "1 of 6 branches missed" at line 17 (`this === other || (other is TransactionDate && value == other.value)`). Add `assertNotEquals` comparing a `TransactionDate` with a `String` or `null`.

  Add: `fun `should not equal different type`()` — `assertNotEquals(TransactionDate("2026-01-10T15:30:45Z"), "not-a-date")` and `assertNotEquals(TransactionDate("2026-01-10T15:30:45Z"), null)`.

  **Must NOT do**: Do not modify production code. Do not break existing equals/hashCode contracts.

  **Recommended Agent Profile**:
  - Category: `quick` — single test method addition

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: —

  **References**:
  - Source: `src/main/kotlin/.../domain/model/TransactionDate.kt:17` — equals method
  - Test: `src/test/kotlin/.../domain/TransactionDateTest.kt:56-61` — existing equals test pattern

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*TransactionDate*"` passes
  - [ ] JaCoCo CSV `TransactionDate` shows 0 missed branches (down from 1)

  **QA Scenarios**:
  ```
  Scenario: Not equal to different type
    Tool: Bash
    Steps: add assertNotEquals with String arg, run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-3-equals-different-type.txt

  Scenario: Not equal to null
    Tool: Bash
    Steps: add assertNotEquals with null arg, run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-3-equals-null.txt
  ```

  **Commit**: YES | Message: `test: add TransactionDate equals different-type branch test` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/TransactionDateTest.kt`]

- [ ] 4. Add ExchangeRate equals branch coverage tests

  **What to do**: Add 3 test methods to `ExchangeRateTest.kt` covering the 3 missed branches in `ExchangeRate.equals()`:
  1. `should not equal different type` — compare ExchangeRate with a String: `assertNotEquals(sampleRate(), "not-a-rate")`
  2. `should not equal when sourceCurrency differs` — same rate value, same targetCurrency, different sourceCurrency: `assertNotEquals(sampleRate(source = TargetCurrency("United-States-Dollar")), sampleRate(source = TargetCurrency("Brazil-Real")))`
  3. `should not equal when targetCurrency differs` — same rate value, same sourceCurrency, different targetCurrency: `assertNotEquals(sampleRate(target = TargetCurrency("Brazil-Real")), sampleRate(target = TargetCurrency("JPY")))`

  Use existing `sampleRate()` helper for constructing instances.

  **Must NOT do**: Do not modify ExchangeRate.kt. Do not change SCALE. Do not modify existing tests.

  **Recommended Agent Profile**:
  - Category: `quick` — 3 test methods in existing test class

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: —

  **References**:
  - Source: `src/main/kotlin/.../domain/model/ExchangeRate.kt:19-26` — equals method
  - Test: `src/test/kotlin/.../domain/ExchangeRateTest.kt:60-65` — existing equals test pattern
  - JaCoCo HTML: `build/reports/jacoco/test/html/.../ExchangeRate.kt.html` — missed branches at lines 22, 24, 25

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*ExchangeRate*"` passes
  - [ ] JaCoCo CSV `ExchangeRate` shows 0 missed branches (down from 3)

  **QA Scenarios**:
  ```
  Scenario: Not equal to non-ExchangeRate type
    Tool: Bash
    Steps: add assertNotEquals(sampleRate(), "string"), run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-4-exrate-different-type.txt

  Scenario: Not equal when sourceCurrency differs
    Tool: Bash
    Steps: add test with same rate, same target, different source
    Expected: test passes
    Evidence: .sisyphus/evidence/task-4-exrate-source-cur.txt

  Scenario: Not equal when targetCurrency differs
    Tool: Bash
    Steps: add test with same rate, same source, different target
    Expected: test passes
    Evidence: .sisyphus/evidence/task-4-exrate-target-cur.txt
  ```

  **Commit**: YES | Message: `test: add ExchangeRate equals branch coverage for currencies` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/ExchangeRateTest.kt`]

- [ ] 5. Add Purchase equals different-type branch test

  **What to do**: Add test to `PurchaseTest.kt` covering the 2 missed branches in `Purchase.equals()` where `other !is Purchase`. The JaCoCo HTML shows "2 of 6 branches missed" at line 27. Add: `fun `should not equal different type`()` — `assertNotEquals(samplePurchase(), "not-a-purchase")` and `assertNotEquals(samplePurchase(), null)`.

  Use existing helper methods in `PurchaseTest.kt` for constructing instances.

  **Must NOT do**: Do not modify Purchase.kt. Do not change validation logic.

  **Recommended Agent Profile**:
  - Category: `quick` — single test method addition

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: —

  **References**:
  - Source: `src/main/kotlin/.../domain/model/Purchase.kt:27` — equals method
  - Test: `src/test/kotlin/.../domain/PurchaseTest.kt` — existing tests
  - JaCoCo HTML: `build/reports/jacoco/test/html/.../Purchase.kt.html` — missed branches at line 27

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*Purchase*"` passes
  - [ ] JaCoCo CSV `Purchase` shows 0 missed branches (down from 2)

  **QA Scenarios**:
  ```
  Scenario: Not equal to different type
    Tool: Bash
    Steps: add assertNotEquals with String arg, run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-5-purchase-different-type.txt

  Scenario: Not equal to null
    Tool: Bash
    Steps: add assertNotEquals with null arg, run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-5-purchase-null.txt
  ```

  **Commit**: YES | Message: `test: add Purchase equals different-type branch test` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/PurchaseTest.kt`]

- [ ] 6. Add TargetCurrency equals different-type branch test

  **What to do**: Add test to `TargetCurrencyTest.kt` covering the 1 missed branch in `TargetCurrency.equals()` where `other !is TargetCurrency`. The JaCoCo HTML shows "1 of 6 branches missed" at line 16. Add: `fun `should not equal different type`()` — `assertNotEquals(TargetCurrency("United-States-Dollar"), "not-a-currency")` and `assertNotEquals(TargetCurrency("United-States-Dollar"), null)`.

  **Must NOT do**: Do not modify TargetCurrency.kt.

  **Recommended Agent Profile**:
  - Category: `quick` — single test method addition

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: — | Blocked By: —

  **References**:
  - Source: `src/main/kotlin/.../domain/model/TargetCurrency.kt:16` — equals method
  - Test: `src/test/kotlin/.../domain/TargetCurrencyTest.kt` — existing tests
  - JaCoCo HTML: `build/reports/jacoco/test/html/.../TargetCurrency.kt.html` — missed branch at line 16

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*TargetCurrency*"` passes
  - [ ] JaCoCo CSV `TargetCurrency` shows 0 missed branches (down from 1)

  **QA Scenarios**:
  ```
  Scenario: Not equal to different type
    Tool: Bash
    Steps: add assertNotEquals(TargetCurrency("United-States-Dollar"), "string"), run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-6-tc-different-type.txt

  Scenario: Not equal to null
    Tool: Bash
    Steps: add assertNotEquals(TargetCurrency("United-States-Dollar"), null), run test
    Expected: test passes
    Evidence: .sisyphus/evidence/task-6-tc-null.txt
  ```

  **Commit**: YES | Message: `test: add TargetCurrency equals different-type branch test` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/TargetCurrencyTest.kt`]

- [ ] 7. Remove PortContractTest (verify no unique assertions lost)

  **What to do**: Before deleting, verify that `PortContractTest` has zero unique assertions not covered elsewhere:
  1. Read `PortContractTest.kt` — it tests `RetrieveConvertedQueryPort` via an `InMemoryRetrieveConvertedQueryPort` (a copy of the use case orchestration logic)
  2. Compare with `RetrieveConvertedUseCaseImplTest` which tests the actual production `RetrieveConvertedUseCaseImpl` implementing the same port with the same scenarios (found, not-found, rate-unavailable)
  3. Confirm all 3 PortContractTest scenarios are already covered by `RetrieveConvertedUseCaseImplTest`:
     - "retrieve converted query returns response" ↔ `should return converted purchase when found` (or similar)
     - "retrieve converted throws not found" ↔ `should throw PurchaseNotFoundException when not found`
     - "retrieve converted throws unavailable when client has no rate" ↔ `should throw RateUnavailableException when client returns null`
  4. Delete `PortContractTest.kt` file
  5. Also remove any helper classes/functions only used by PortContractTest (the private in-memory port implementations in the same file will be deleted with it)

  **Must NOT do**: Do not delete `RetrieveConvertedUseCaseImplTest` or any other test file. Do not modify port interface contracts.

  **Recommended Agent Profile**:
  - Category: `quick` — file deletion + assertion verification

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 8 | Blocked By: 1-6

  **References**:
  - Test to remove: `src/test/kotlin/.../domain/PortContractTest.kt`
  - Already covers: `src/test/kotlin/.../application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt`

  **Acceptance Criteria**:
  - [ ] `Test-Path -Path "src/test/kotlin/**/PortContractTest.kt"` returns False
  - [ ] `./gradlew test --tests "*RetrieveConverted*"` passes (existing tests validate same scenarios)
  - [ ] JaCoCo coverage for `RetrieveConvertedUseCaseImpl` remains unchanged (0 missed instructions)

  **QA Scenarios**:
  ```
  Scenario: File deleted
    Tool: Bash
    Steps: Check if PortContractTest.kt exists
    Expected: File does not exist
    Evidence: .sisyphus/evidence/task-7-file-deleted.txt

  Scenario: Existing tests still pass
    Tool: Bash
    Steps: ./gradlew test --tests "*RetrieveConvertedUseCaseImpl*"
    Expected: All tests pass
    Evidence: .sisyphus/evidence/task-7-existing-tests-pass.txt
  ```

  **Commit**: YES | Message: `test: remove redundant PortContractTest (covered by RetrieveConvertedUseCaseImplTest)` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/domain/PortContractTest.kt`]

- [ ] 8. Consolidate overlapping validation tests

  **What to do**: `StorePurchaseUseCaseImplTest` (3 tests), `PurchaseControllerV1ValidationTest` (3 tests), and `PurchaseControllerV1Test` (integration) all test the same 3 validation scenarios (blank description, long description, zero/negative amount). However, they test DIFFERENT code paths:
  - Controller `validate()` method (`PurchaseControllerV1.kt:44-50`) — independent copy of validation logic
  - Use case `storePurchase()` method (`StorePurchaseUseCaseImpl.kt:21-25`) — independent copy of same logic
  - Integration tests cover the full HTTP→controller→use case stack

  **Remove `PurchaseControllerV1ValidationTest`** because:
  - Its 3 scenarios duplicate both the use case test AND the integration test
  - `PurchaseControllerV1Test` (integration via RestAssured) covers the full stack including controller validation with HTTP 400 assertions
  - `StorePurchaseUseCaseImplTest` covers the business layer validation in isolation
  - The controller `validate()` method is a defense-in-depth copy with no unique logic beyond what the use case and integration tests cover
  - Removing it reduces maintenance burden (3 files → 2 files for the same 3 scenarios)

  Steps:
  1. Remove `PurchaseControllerV1ValidationTest.kt`
  2. Verify `StorePurchaseUseCaseImplTest` and `PurchaseControllerV1Test` still pass

  **Must NOT do**: Do not remove `StorePurchaseUseCaseImplTest`. Do not remove `PurchaseControllerV1Test` (it's integration-level and tests HTTP contract). Do not modify `PurchaseControllerV1` or `StorePurchaseRequest`.

  **Recommended Agent Profile**:
  - Category: `quick` — file deletion

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: — | Blocked By: 7

  **References**:
  - Test to remove: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1ValidationTest.kt`
  - Keep: `src/test/kotlin/.../application/service/purchase/StorePurchaseUseCaseImplTest.kt`
  - Keep: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt`

  **Acceptance Criteria**:
  - [ ] `Test-Path -Path "src/test/kotlin/**/PurchaseControllerV1ValidationTest.kt"` returns False
  - [ ] `./gradlew test --tests "*StorePurchaseUseCaseImpl*"` passes (validation coverage preserved)
  - [ ] `./gradlew test --tests "*PurchaseControllerV1*"` passes (integration tests intact)
  - [ ] Coverage for `StorePurchaseUseCaseImpl` and `PurchaseControllerV1` remains unchanged

  **QA Scenarios**:
  ```
  Scenario: File deleted
    Tool: Bash
    Steps: Check if PurchaseControllerV1ValidationTest.kt exists
    Expected: File does not exist
    Evidence: .sisyphus/evidence/task-8-file-deleted.txt

  Scenario: Use-case validation still works
    Tool: Bash
    Steps: ./gradlew test --tests "*StorePurchaseUseCaseImpl*"
    Expected: All tests pass
    Evidence: .sisyphus/evidence/task-8-usecase-tests-pass.txt

  Scenario: Controller integration tests still pass
    Tool: Bash
    Steps: ./gradlew test --tests "*PurchaseControllerV1Test*"
    Expected: All tests pass
    Evidence: .sisyphus/evidence/task-8-controller-tests-pass.txt
  ```

  **Commit**: YES | Message: `test: remove redundant PurchaseControllerV1ValidationTest (scenarios covered by StorePurchaseUseCaseImplTest)` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/infra/client/purchase/PurchaseControllerV1ValidationTest.kt`]

- [ ] 9. Add PurchaseRepositoryJPAAdapter.save() unit test

  **What to do**: Add a test to `PurchaseRepositoryJPAAdapterTest` that verifies the `save()` method correctly converts a domain `Purchase` to a `PurchaseJpaEntity`, delegates to `springDataRepository.save()`, and returns the converted domain object.

  Test: `fun `save should persist and return purchase`()`:
  1. Create a domain `Purchase` instance using existing sample patterns
  2. Mock `springDataRepository.save(entity)` to return a matching entity
  3. Call `adapter.save(purchase)`
  4. Assert the returned `Purchase` matches expected values (id, description, amount, etc.)

  Use Mockito `verify` to confirm `springDataRepository.save()` was called exactly once.

  Reference the existing `findById` tests in the same file for the mocking pattern (`Mockito.mock`, `Mockito.when`, `Mockito.verify`).

  **Must NOT do**: Do not modify `PurchaseRepositoryJPAAdapter.kt`. Do not add Spring Boot test slice annotations (pure unit test with Mockito).

  **Recommended Agent Profile**:
  - Category: `quick` — single test method in existing test class

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: — | Blocked By: 1-6

  **References**:
  - Source: `src/main/kotlin/.../infra/adapter/persistence/PurchaseRepositoryJPAAdapter.kt:13-17` — save() method
  - Test: `src/test/kotlin/.../infra/adapter/persistence/PurchaseRepositoryJPAAdapterTest.kt` — existing tests
  - Entity: `src/main/kotlin/.../infra/adapter/persistence/PurchaseJpaEntity.kt` — fromDomain/toDomain methods

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*PurchaseRepositoryJPAAdapter*"` passes
  - [ ] Test verifies `springDataRepository.save()` was called

  **QA Scenarios**:
  ```
  Scenario: Save delegates to Spring Data repository
    Tool: Bash
    Steps: add test calling adapter.save(), verify springDataRepository.save() called
    Expected: test passes, Mockito verify passes
    Evidence: .sisyphus/evidence/task-9-save-test.txt

  Scenario: Save returns mapped domain object
    Tool: Bash
    Steps: assert returned Purchase has correct id, description, amount
    Expected: all assertions pass
    Evidence: .sisyphus/evidence/task-9-save-return.txt
  ```

  **Commit**: YES | Message: `test: add PurchaseRepositoryJPAAdapter.save() unit test` | Files: [`src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/PurchaseRepositoryJPAAdapterTest.kt`]

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> Run ALL 4 verification tasks. ALL must pass. Present consolidated results to user and get explicit "okay" before completing.

- [ ] F1. Plan Compliance Audit

  **What to do**: Run CI-equivalent command and verify all compliance conditions.

  **Recommended Agent Profile**:
  - Category: `quick` — scripted verification

  **Parallelization**: Can Parallel: YES (F1-F4 run concurrently) | Wave 3 | Blocks: — | Blocked By: 1-9

  **References**:
  - CI config: `.github/workflows/ci.yml:45-46`
  - Coverage report: `build/reports/jacoco/test/jacocoTestReport.csv`

  **Acceptance Criteria**:
  - [ ] `./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification` exits with code 0
  - [ ] JaCoCo CSV shows `INSTRUCTION_MISSED=0` and `BRANCH_MISSED=0` for ALL domain classes
  - [ ] JaCoCo CSV shows overall project coverage ≥90%
  - [ ] `Select-String -Path "src/test/kotlin/**/ExchangeRateTest.kt" -Pattern "6 decimal"` returns 0 matches
  - [ ] `Test-Path -Path "src/test/kotlin/**/PortContractTest.kt"` returns False
  - [ ] `Test-Path -Path "src/test/kotlin/**/PurchaseControllerV1ValidationTest.kt"` returns False

  **QA Scenarios**:
  ```
  Scenario: Full CI-equivalent build passes
    Tool: Bash
    Steps: ./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification
    Expected: exit code 0, build successful
    Evidence: .sisyphus/evidence/F1-build-pass.txt

  Scenario: Domain classes have 0 missed instructions and 0 missed branches
    Tool: Bash
    Steps: Import-Csv build/reports/jacoco/test/jacocoTestReport.csv | Where-Object { $_.GROUP -eq 'wex_transactions' -and $_.PACKAGE -like '*domain*' } | Select-Object CLASS, INSTRUCTION_MISSED, BRANCH_MISSED
    Expected: All domain rows show 0 for both INSTRUCTION_MISSED and BRANCH_MISSED
    Evidence: .sisyphus/evidence/F1-domain-coverage.csv

  Scenario: Test name fix verified
    Tool: Bash
    Steps: Select-String -Path "src/test/kotlin/**/ExchangeRateTest.kt" -Pattern "6 decimal"
    Expected: No matches found
    Evidence: .sisyphus/evidence/F1-no-6-decimal.txt

  Scenario: Redundant tests removed
    Tool: Bash
    Steps: Test-Path "src/test/kotlin/**/PortContractTest.kt" -and Test-Path "src/test/kotlin/**/PurchaseControllerV1ValidationTest.kt"
    Expected: Both return False
    Evidence: .sisyphus/evidence/F1-files-removed.txt
  ```

  **Commit**: NO

- [ ] F2. Code Quality Review

  **What to do**: Inspect all modified test files for quality issues.

  **Recommended Agent Profile**:
  - Category: `quick` — file inspection

  **Parallelization**: Can Parallel: YES (F1-F4 run concurrently) | Wave 3 | Blocks: — | Blocked By: 1-9

  **References**:
  - Modified files: ExchangeRateTest.kt, TransactionDateTest.kt, PurchaseTest.kt, TargetCurrencyTest.kt, PurchaseRepositoryJPAAdapterTest.kt
  - Deleted files: PortContractTest.kt, PurchaseControllerV1ValidationTest.kt

  **Acceptance Criteria**:
  - [ ] No test uses Thread.sleep, flaky assertions, or brittle patterns
  - [ ] New tests follow existing codebase style (same assertions, same helper patterns)
  - [ ] No test has empty or commented bodies
  - [ ] Only planned modifications applied: test additions (tasks 2-6, 9), test name fixes (task 1), test deletions (tasks 7-8)
  - [ ] No test was modified outside of the changes specified in tasks 1-9

  **QA Scenarios**:
  ```
  Scenario: New tests follow codebase patterns
    Tool: Bash
    Steps: Read each modified test file and check assertion style matches existing patterns (kotlin.test assertEquals/assertNotEquals/assertFailsWith, not Hamcrest or AssertJ)
    Expected: All new assertions use kotlin.test.* or org.junit.jupiter.* patterns consistent with existing code
    Evidence: .sisyphus/evidence/F2-patterns.txt

  Scenario: No flaky patterns introduced
    Tool: Bash
    Steps: grep for "Thread.sleep" in all modified test files
    Expected: 0 matches
    Evidence: .sisyphus/evidence/F2-no-flaky.txt
  ```

  **Commit**: NO

- [ ] F3. Real Manual QA

  **What to do**: Run the full test suite and verify all tests pass with no regressions.

  **Recommended Agent Profile**:
  - Category: `quick` — test suite runner

  **Parallelization**: Can Parallel: YES (F1-F4 run concurrently) | Wave 3 | Blocks: — | Blocked By: 1-9

  **References**:
  - `build.gradle.kts` — test configuration

  **Acceptance Criteria**:
  - [ ] `./gradlew test` exits with code 0
  - [ ] All tests pass (0 failures, 0 errors)

  **QA Scenarios**:
  ```
  Scenario: Full test suite passes
    Tool: Bash
    Steps: ./gradlew test
    Expected: BUILD SUCCESSFUL, 0 failures, 0 errors
    Evidence: .sisyphus/evidence/F3-all-tests-pass.txt
  ```

  **Commit**: NO

- [ ] F4. Scope Fidelity Check

  **What to do**: Verify that ONLY test files were changed (no production code modifications).

  **Recommended Agent Profile**:
  - Category: `quick` — git diff inspection

  **Parallelization**: Can Parallel: YES (F1-F4 run concurrently) | Wave 3 | Blocks: — | Blocked By: 1-9

  **References**:
  - Git: use `git diff --name-only` to list changed files

  **Acceptance Criteria**:
  - [ ] `git diff --name-only` lists ONLY test files (paths containing `src/test/`)
  - [ ] No `.kt` files in `src/main/` were modified
  - [ ] No JaCoCo configuration files were modified
  - [ ] No business logic files were altered

  **QA Scenarios**:
  ```
  Scenario: Only test files changed
    Tool: Bash
    Steps: git diff --name-only
    Expected: All changed paths contain "src/test/" — none contain "src/main/"
    Evidence: .sisyphus/evidence/F4-only-test-files.txt

  Scenario: No config or thresholds changed
    Tool: Bash
    Steps: git diff --name-only | Select-String -Pattern "build.gradle|jacoco|\.yml|\.yaml|\.properties"
    Expected: No matches
    Evidence: .sisyphus/evidence/F4-no-config-changes.txt
  ```

  **Commit**: NO

## Commit Strategy
| Task | Commit Message | Files |
|------|---------------|-------|
| 1 | `test: fix ExchangeRate test names to reflect SCALE=2` | ExchangeRateTest.kt |
| 2 | `test: add ZonedDateTime branch coverage for TransactionDate.parse()` | TransactionDateTest.kt |
| 3 | `test: add TransactionDate equals different-type branch test` | TransactionDateTest.kt |
| 4 | `test: add ExchangeRate equals branch coverage for currencies` | ExchangeRateTest.kt |
| 5 | `test: add Purchase equals different-type branch test` | PurchaseTest.kt |
| 6 | `test: add TargetCurrency equals different-type branch test` | TargetCurrencyTest.kt |
| 7 | `test: remove redundant PortContractTest (covered by RetrieveConvertedUseCaseImplTest)` | PortContractTest.kt (delete) |
| 8 | `test: remove redundant PurchaseControllerV1ValidationTest` | PurchaseControllerV1ValidationTest.kt (delete) |
| 9 | `test: add PurchaseRepositoryJPAAdapter.save() unit test` | PurchaseRepositoryJPAAdapterTest.kt |

All commits are independent and can be applied in any order within each wave.

## Success Criteria
1. [ ] `./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification` exits with 0
2. [ ] All domain classes in JaCoCo CSV show `INSTRUCTION_MISSED=0` and `BRANCH_MISSED=0`
3. [ ] Project overall coverage remains ≥90%
4. [ ] No test mentions "6 decimal places" — only "2 decimal places"
5. [ ] PortContractTest.kt deleted
6. [ ] PurchaseControllerV1ValidationTest.kt deleted
7. [ ] PurchaseRepositoryJPAAdapter has save() test
8. [ ] 0 production files modified (only test files)
9. [ ] All 4 final review agents approve
