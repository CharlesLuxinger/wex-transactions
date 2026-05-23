# Requirement-to-Task Traceability Matrix

Generated: 2026-05-23
Source: `.docs/wex.md`, `.docs/SPEC.md`, `.specs/features/ROADMAP.md`

## Coverage Summary

| Metric | Count |
|--------|-------|
| Total REQs (SPEC.md) | 25 |
| Mapped to implementation | 25 |
| Mapped to tests | 25 |
| Unmapped | 0 |
| Orphan tasks | 0 |

---

## F-A: Store Purchase Transaction

### REQ-01: description required, max 50 chars
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-A |
| **Plan Task** | 3 (Vertical Slice A — Store Purchase Transaction) |
| **Implementation** | `domain/model/Purchase.kt` (MAX_DESCRIPTION_LENGTH = 50, init validation) |
| | `domain/port/inbound/purchase/model/StorePurchaseRequest.kt` (`@Size(max = 50)`) |
| | `infra/client/purchase/PurchaseControllerV1.kt` (validate()) |
| **Controller** | `POST /api/v1/purchases` — `PurchaseControllerV1` |
| **Use Case** | `application/service/purchase/StorePurchaseUseCaseImpl` |
| **Unit Tests** | `domain/PurchaseTest.kt` |
| | `infra/client/purchase/PurchaseControllerV1ValidationTest.kt` |
| **API Tests** | `infra/client/purchase/PurchaseControllerV1Test.kt` |
| **QA Evidence** | HTTP 400 when description blank or > 50 chars |

### REQ-02: transactionDate required, ISO-8601 input → canonical yyyy-MM-dd'T'HH:mm:ssXXX
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-A |
| **Plan Task** | 1, 3 |
| **Implementation** | `domain/model/TransactionDate.kt` (parsing + canonical format) |
| | `domain/port/inbound/purchase/model/StorePurchaseRequest.kt` |
| **Controller** | `POST /api/v1/purchases` |
| **Use Case** | `StorePurchaseUseCaseImpl` (creates TransactionDate) |
| **Unit Tests** | `domain/TransactionDateTest.kt` |
| **API Tests** | `infra/client/purchase/PurchaseControllerV1Test.kt` |
| **QA Evidence** | HTTP 400 for invalid date format |

### REQ-03: purchaseAmount required, positive, USD, rounded to 2 decimals
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-A |
| **Plan Task** | 3 |
| **Implementation** | `domain/model/Purchase.kt` (init: amount > 0) |
| | `StorePurchaseUseCaseImpl` (validates + sets scale=6 internally) |
| **Controller** | `POST /api/v1/purchases` — validate() |
| **Unit Tests** | `domain/PurchaseTest.kt` |
| | `StorePurchaseUseCaseImplTest.kt` |
| **API Tests** | `infra/client/purchase/PurchaseControllerV1Test.kt` |
| **QA Evidence** | HTTP 400 for zero/negative amount |

### REQ-04: System generates and returns unique Long identifier
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-A |
| **Plan Task** | 3 |
| **Implementation** | `infra/adapter/persistence/PurchaseJpaEntity.kt` (BIGSERIAL → Long) |
| | `infra/client/purchase/PurchaseControllerV1.kt` (returns StorePurchaseResponse with id) |
| **Controller** | `POST /api/v1/purchases` → 201 Created |
| **Unit Tests** | `infra/adapter/persistence/PurchaseJpaEntityTest.kt` |
| **API Tests** | `PurchaseControllerV1Test.kt` (asserts id notNullValue) |
| **QA Evidence** | HTTP 201 with numeric Long id |

---

## F-B: Retrieve Converted Purchase

### REQ-05: Exchange source is Treasury Reporting Rates only
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-B |
| **Plan Task** | 5 |
| **Implementation** | `domain/port/outbound/ExchangeRateClientPort.kt` |
| | `infra/adapter/external/DummyExchangeRateClientAdapter.kt` |
| **Controller** | `GET /api/v1/purchases/{id}/converted` |
| **Use Case** | `RetrieveConvertedUseCaseImpl` |
| **Unit Tests** | `application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt` |
| **QA Evidence** | Conversion uses Treasury-only policy (dummy impl) |

### REQ-06: Selected rate date <= purchase date
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-B |
| **Plan Task** | 5 |
| **Implementation** | `infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt` (query: `e.rateDate <= :rateDate`) |
| **Unit Tests** | `infra/adapter/persistence/ExchangeRateRepositoryAdapterTest.kt` |
| **QA Evidence** | Query constraint enforced at DB level |

### REQ-07: Eligible historical window is prior 6 months inclusive
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-B |
| **Plan Task** | 5 |
| **Implementation** | `RetrieveConvertedUseCaseImpl` (MAX_WINDOW_MONTHS = 6) |
| | `ExchangeRateRepositoryAdapter` (query: `e.rateDate >= :minDate` where minDate = rateDate - 6mo) |
| **Unit Tests** | `ExchangeRateRepositoryAdapterTest.kt` |
| **QA Evidence** | Exactly 6 months accepted; 6 months + 1 day rejected |

### REQ-08: Business error if no eligible rate in window
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-B |
| **Plan Task** | 5, 6 |
| **Implementation** | `domain/model/DomainException.kt` (RateUnavailableException) |
| | `RetrieveConvertedUseCaseImpl` (throws when rate not found) |
| | `infra/client/error/GlobalExceptionHandler.kt` (422 Problem Detail) |
| **API Tests** | `RetrieveConvertedControllerV1Test.kt` (asserts 422) |
| **QA Evidence** | HTTP 422 with Conversion Unavailable |

### REQ-09: Converted amount rounded to 2 decimals
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-B |
| **Plan Task** | 5 |
| **Implementation** | `RetrieveConvertedUseCaseImpl` (`setScale(2, RoundingMode.HALF_UP)`) |
| **Unit Tests** | `RetrieveConvertedUseCaseImplTest.kt` |
| **API Tests** | `RetrieveConvertedControllerV1Test.kt` (asserts 81.38) |
| **QA Evidence** | 15.50 USD × 5.25 rate = 81.38 (2 decimals, HALF_UP) |

### REQ-10: Response includes id, description, date, original USD, rate, converted amount
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-B |
| **Plan Task** | 5 |
| **Implementation** | `domain/port/inbound/retrieveConverted/model/RetrieveConvertedResponse.kt` |
| **API Tests** | `RetrieveConvertedControllerV1Test.kt` (asserts all fields) |
| **QA Evidence** | Full response projection verified |

---

## F-C: Error Taxonomy

### REQ-11: Error for description > 50
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-C |
| **Plan Task** | 3, 6 |
| **Implementation** | `PurchaseControllerV1.validate()` → IllegalArgumentException |
| | `GlobalExceptionHandler` → 400 Problem Detail |
| **API Tests** | `PurchaseControllerV1Test.kt` (51 chars → 400) |
| **QA Evidence** | HTTP 400, title "Bad Request" |

### REQ-12: Error for invalid date format
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-C |
| **Plan Task** | 3, 6 |
| **Implementation** | `TransactionDate.kt` (throws on invalid) |
| | `PurchaseControllerV1.validate()` catches |
| | `GlobalExceptionHandler` → 400 Problem Detail |
| **API Tests** | `PurchaseControllerV1Test.kt` ("invalid-date" → 400) |
| **QA Evidence** | HTTP 400, detail "Invalid ISO-8601 transaction date" |

### REQ-13: Error for invalid amount (missing/non-numeric/zero/negative)
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-C |
| **Plan Task** | 3, 6 |
| **Implementation** | `PurchaseControllerV1.validate()` (require > ZERO) |
| | `GlobalExceptionHandler` → 400 Problem Detail |
| **API Tests** | `PurchaseControllerV1Test.kt` (-1.00 → 400) |
| **QA Evidence** | HTTP 400 for zero/negative amount |

### REQ-14: Error for conversion unavailable, Problem Details contract
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Feature** | F-C |
| **Plan Task** | 5, 6 |
| **Implementation** | `RateUnavailableException` → `GlobalExceptionHandler` → 422 Problem Detail |
| **API Tests** | `RetrieveConvertedControllerV1Test.kt` (EUR → 422) |
| **QA Evidence** | HTTP 422, title "Conversion Unavailable", Problem Details format |

---

## Quality + Architecture Constraints

### REQ-15: JaCoCo overall threshold >= 90%
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 8 |
| **Implementation** | `build.gradle.kts` (jacocoTestCoverageVerification: minimum = 0.9) |
| | `.github/workflows/ci.yml` (PR reporter: min-coverage-overall: 90) |
| **Verification** | `./gradlew test jacocoTestCoverageVerification` |

### REQ-16: Domain package coverage = 100%
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 8 |
| **Implementation** | `build.gradle.kts` (domain rule: minimum = 1.0) |
| **Verification** | `./gradlew test jacocoTestCoverageVerification` |

### REQ-17: ktlint checks pass
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 2 |
| **Implementation** | `build.gradle.kts` (ktlint plugin) + `.github/workflows/ci.yml` (ktlint steps) |
| **Verification** | `./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck` |

### REQ-18: detekt checks pass
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 2 |
| **Implementation** | `build.gradle.kts` (detekt plugin) + `.github/workflows/ci.yml` (detekt step) |
| | `config/detekt/detekt.yml` |
| **Verification** | `./gradlew detekt` |

### REQ-19: TDD mandatory for feature development
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 3, 4, 5 |
| **Implementation** | `AGENTS.md` (team policy: TDD RED→GREEN→REFACTOR) |
| **Verification** | Policy documented; tests exist for all features |

### REQ-20: API tests use RestAssured, no mocks, Problem Details validation
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 4 |
| **Implementation** | `build.gradle.kts` (rest-assured dependency) |
| | `config/AbstractRestApiIntegrationTest.kt` |
| | `RestAssuredRequestSupport.kt` |
| **Test Files** | `PurchaseControllerV1Test.kt` |
| | `RetrieveConvertedControllerV1Test.kt` |
| **QA Evidence** | All API tests use RestAssured against real server |

### REQ-21: Integration/E2E uses Testcontainers
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 4 |
| **Implementation** | `config/ContainersConfig.kt` (PostgreSQLContainer) |
| | `config/TestContainersConfig.kt` |
| | `config/TestContainersSupport.kt` |
| **Test Files** | `persistence/PersistenceFlywayIntegrationTest.kt` |
| | `persistence/PersistenceRepositoryIntegrationTest.kt` |
| **QA Evidence** | Testcontainers spin up PostgreSQL for integration tests |

### REQ-22: Layer dependency direction infra → application → domain
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 7 |
| **Implementation** | `architecture/DependencyDirectionTest.kt` |
| **QA Evidence** | ArchUnit enforces: domain !→ application, domain !→ infra, application !→ infra |

### REQ-23: Controllers do not import repositories/adapters directly
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 7 |
| **Implementation** | `architecture/ControllerBoundaryTest.kt` |
| **QA Evidence** | ArchUnit enforces: controllers !→ adapter packages |

### REQ-24: UseCase implementations in application/service/**
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 7 |
| **Implementation** | `architecture/UseCaseOwnershipTest.kt` |
| | `application/service/purchase/StorePurchaseUseCaseImpl.kt` |
| | `application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt` |
| **QA Evidence** | ArchUnit enforces: UseCaseImpl → application layer only |

### REQ-25: Domain stays framework-free (no Spring annotations)
| Field | Value |
|-------|-------|
| **Source** | `.docs/SPEC.md` |
| **Plan Task** | 7 |
| **Implementation** | `architecture/DependencyDirectionTest.kt` |
| | All domain/ classes checked for Spring annotation absence |
| **QA Evidence** | Domain package has zero Spring imports |

---

## Milestone Mapping (ROADMAP)

| Milestone | REQs Covered | Plan Tasks |
|-----------|-------------|------------|
| M1 — Foundation | REQ-15,16,17,18,19,22,23,24,25 | 1, 2, 7, 8 |
| M2 — Store Purchase | REQ-01,02,03,04,11,12,13 | 3 |
| M3 — Retrieve Converted | REQ-05,06,07,08,09,10,14 | 5, 6 |
| M4 — Hardening | REQ-15,16,17,18,20,21 (verification) | 4, 7, 8, 9, 10 |

---

## Implementation Task Index (Plan)

| Task | Description | REQs Covered |
|------|-------------|-------------|
| 1 | Contract Decision Closure | 02 (date normalization), 03 (rounding), 14 (Problem Details) |
| 2 | Foundation Enablement | 17, 18, 21 |
| 3 | Store Purchase Transaction | 01, 02, 03, 04, 11, 12, 13 |
| 4 | API Verification Harness | 20, 21 |
| 5 | Retrieve Converted Purchase | 05, 06, 07, 08, 09, 10, 14 |
| 6 | Unified Error Contract | 11, 12, 13, 14 |
| 7 | Architecture Hardening | 22, 23, 24, 25 |
| 8 | Coverage Policy Activation | 15, 16 |
| 9 | Traceability (this document) | All |
| 10 | Final Execution Readiness | All (verification orchestration) |

---

## Validation Commands

```bash
# REQ-15,16: Coverage thresholds
./gradlew --no-daemon test jacocoTestCoverageVerification

# REQ-17: ktlint
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck

# REQ-18: detekt
./gradlew --no-daemon detekt

# REQ-22,23,24,25: Architecture
./gradlew --no-daemon test --tests "com.charlesluxinger.wex_transactions.architecture.*"

# REQ-20,21: API + Integration tests
./gradlew --no-daemon test --tests "*Api*" --tests "*Integration*"

# Full suite
./gradlew --no-daemon test jacocoTestReport
```
