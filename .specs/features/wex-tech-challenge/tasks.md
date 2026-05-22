# Tasks — WEX Tech Challenge (Atomic + Verifiable)

## Conventions
- Status: TODO | IN_PROGRESS | DONE | BLOCKED
- Each task includes: What, Where, Depends on, Done when, Tests, Gate
- All tasks follow TDD (RED -> GREEN -> REFACTOR)

---

## T-001 — Create Package Skeleton
- Status: TODO
- What: Create required package structure for `domain`, `application`, `infra` following AGENTS.md.
- Where:
  - `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/**`
  - `src/main/kotlin/com/charlesluxinger/wex_transactions/application/**`
  - `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/**`
- Depends on: none
- Done when: package scaffold exists with placeholder-safe classes/interfaces only where necessary.
- Tests: architecture vacuous guard still passes.
- Gate: `./gradlew test`.

## T-002 — Define Domain Model and Value Objects (Purchase)
- Status: TODO
- What: Implement domain entity/value objects enforcing REQ-01..03 rules without framework annotations.
- Where: `domain/model/**`
- Depends on: T-001
- Done when: domain constructors/factories enforce invariants and rounding policy.
- Tests: unit tests for all invariants and rounding edge cases.
- Gate: domain coverage remains 100%.

## T-003 — Define Inbound and Outbound Ports
- Status: TODO
- What: Create command/query ports and repository/client outbound ports.
- Where:
  - `domain/port/inbound/**`
  - `domain/port/outbound/**`
- Depends on: T-001, T-002
- Done when: interfaces express use cases for create and converted retrieval.
- Tests: compilation + architecture rules.
- Gate: dependency direction test passes.

## T-004 — Implement Application Use Cases
- Status: TODO
- What: Implement use case orchestration for create purchase and retrieve converted purchase.
- Where: `application/service/purchase/**`
- Depends on: T-003
- Done when: use cases delegate business rules to domain and integrate outbound ports, including retry + fail-fast policy for Treasury outages.
- Tests: unit tests with test doubles at application boundary.
- Gate: `UseCaseOwnershipTest` passes.

## T-005 — Persistence Adapter for Purchase
- Status: TODO
- What: Implement JPA entity + mapper + adapter for repository port.
- Where: `infra/persistence/**`
- Depends on: T-003, T-004
- Done when: create/find purchase workflows persist and retrieve correctly.
- Tests: integration tests with Testcontainers DB.
- Gate: no controller boundary violations.

## T-006 — Treasury Rate Client Adapter
- Status: TODO
- What: Implement outbound client adapter to Treasury source with rate window policy support.
- Where: `infra/client/treasury/**`
- Depends on: T-003, T-004
- Done when: adapter can query/filter rates for `<= purchaseDate` and 6-month window, with retry + fail-fast outage behavior.
- Tests: integration tests with controlled HTTP responses/containerized test infra.
- Gate: deterministic tests, no flaky external dependency.

## T-007 — REST Controller: Create Purchase
- Status: TODO
- What: Expose create purchase endpoint mapped to inbound command port/use case.
- Where: `infra/client/purchase/PurchaseControllerV1.kt`
- Depends on: T-004, T-005
- Done when: success returns generated `Long` identifier and validation errors map correctly.
- Tests: RestAssured API tests.
- Gate: `ControllerBoundaryTest` passes.

## T-008 — REST Controller: Retrieve Converted Purchase
- Status: TODO
- What: Expose retrieval endpoint returning converted response fields per REQ-10.
- Where: `infra/client/purchase/PurchaseControllerV1.kt` (or same feature module)
- Depends on: T-004, T-005, T-006
- Done when: converted response uses ISO-4217 `targetCurrency`, transaction date format `yyyy-MM-dd'T'HH:mm:ssXXX`, and conversion-unavailable path implemented.
- Tests: RestAssured API tests for success + failure scenarios.
- Gate: all architecture tests pass.

## T-009 — Global Error Mapping
- Status: TODO
- What: Implement Problem Details mapping for REQ-11..14 errors.
- Where: `infra/**/ExceptionHandler` or equivalent
- Depends on: T-007, T-008
- Done when: all required business errors return deterministic contract/status.
- Tests: API tests validating error payload and status.
- Gate: detekt + ktlint clean.

## T-010 — Build/Config Wiring
- Status: TODO
- What: Add/confirm dependencies and runtime config required by selected technical decisions (PostgreSQL runtime profile).
- Where:
  - `build.gradle.kts`
  - `src/main/resources/application.yaml`
- Depends on: T-001 (can begin earlier)
- Done when: app boots and tests run with chosen DB/client strategy.
- Tests: smoke test + full test suite.
- Gate: CI-equivalent local pipeline passes.

## T-011 — Final Verification Pack
- Status: TODO
- What: Run full quality pipeline and collect evidence.
- Where: repository root commands
- Depends on: T-001..T-010
- Done when:
  - ktlint pass
  - detekt pass
  - test pass
  - JaCoCo thresholds pass (overall + domain)
- Tests: full suite
- Gate: same as CI.

---

## Parallelization Opportunities
- [P] T-005 and T-006 can run in parallel after ports/use case contracts stabilize.
- [P] Validation/error API scenarios can be split per feature once controller endpoints exist.

## Blockers Before Coding
- Open ambiguity answers in `context.md` required to freeze API contract and integration behavior.