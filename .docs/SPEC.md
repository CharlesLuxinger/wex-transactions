# WEX Challenge — Implementation Specification

> Generated from `.docs/wex.md` + project audit.
> Date: 2026-05-22

---

## 1. Project Baseline

| Aspect | Status |
|--------|--------|
| Stack | Spring Boot 3.14 / Kotlin 2.3.21 / Java 25 |
| Persistence | spring-boot-starter-data-jpa (DB not yet chosen — no runtime dep) |
| Web | spring-boot-starter-web (REST controllers) |
| JSON | jackson-module-kotlin |
| Architecture scaffold | Hexagonal + DDD (AGENTS.md) — package structure **not yet created** |
| Existing classes | 1 production file (`Application.kt`), 6 architecture test files, 1 context-load test |
| Quality gates | ktlint, detekt, JaCoCo (90% overall, 100% domain) |
| Testing mandate | TDD + RestAssured + Testcontainers |

---

## 2. Feature Mapping

### Feature A: Store Purchase Transaction

**Source:** `.docs/wex.md` §1, §3.1

**Required fields:**
- `description` — required, max 50 chars
- `transactionDate` — required, valid date
- `purchaseAmount` — required, positive, USD, 2 decimal places
- Generated unique identifier (returned on success)

**Existing code:**
- **Zero domain entities** exist.
- **Zero controllers, ports, use cases, or repositories** exist.
- Only `Application.kt` (main class) is present.

**User-visible gap:** Nothing. Full implementation needed.

---

### Feature B: Retrieve Purchase in Target Currency

**Source:** `.docs/wex.md` §3.2

**Conversion rules:**
- Source: Treasury Reporting Rates of Exchange (treasury-only)
- Rate date ≤ purchase date
- Historical fallback: up to 6 months prior (inclusive)
- Fail with business error if no eligible rate found
- Converted amount rounded to 2 decimal places

**Response fields:**
- purchase identifier, description, transaction date
- original USD amount, exchange rate used, converted amount

**Required integration:**
- HTTP client to Treasury Fiscal Data API (`fiscaldata.treasury.gov`)
- Rate lookup logic with date-window filtering
- Caching consideration (not specified but implied by domain)

**Existing code:**
- **No HTTP client adapter** exists.
- **No exchange-rate domain model or port** exists.
- **No external API config** in `application.yaml`.

**User-visible gap:** Nothing. Full implementation needed.

---

### Feature C: Error Handling

**Source:** `.docs/wex.md` §4

**Error taxonomy:**
| Error | When |
|-------|------|
| Invalid description length | >50 characters |
| Invalid date format | Non-parseable `transactionDate` |
| Invalid purchase amount | Missing, non-numeric, zero, or negative |
| Conversion unavailable | No rate ≤ purchase date within prior 6 months |

**Existing code:**
- **No global exception handler** exists.
- **No domain-specific error types** exist.

**User-visible gap:** Nothing. Full implementation needed.

---

## 3. Package Structure Gap Analysis

### Required (per AGENTS.md) vs. Actual

```
com.charlesluxinger.wex_transactions
├── domain/
│   ├── port/
│   │   ├── inbound/         # <Feature>QueryPort, <Feature>CommandPort
│   │   │   └── purchase/model/  # Commands/Queries/DTOs
│   │   └── outbound/        # <Feature>RepositoryPort, <Feature>ClientPort
│   ├── model/               # Entities, value objects
│   └── service/             # Domain services
├── application/
│   └── service/
│       └── purchase/        # PurchaseUseCaseImpl
├── infra/
│   ├── client/              # PurchaseControllerV1
│   ├── persistence/         # PurchaseJpaAdapter, entities
│   └── client/              # TreasuryRateClientAdapter
└── config/                  # Excluded from coverage
```

**Status:** Zero of these packages exist. Only `Application.kt` at root.

---

## 4. Key Architectural Decisions Implied

From `.docs/wex.md` + `AGENTS.md`:

| Decision | Implication |
|----------|-------------|
| **No API contract in spec** | Endpoint design, HTTP methods, URL patterns, status codes — all implementation choice |
| **No DB specified** | JPA + H2 (dev) vs. PostgreSQL (prod) — build config has no runtime DB dep, only starter |
| **Treasury-only currency source** | Single external HTTP client; no provider abstraction needed |
| **Rate date ≤ purchase date** | Query logic: find latest rate with `effective_date <= purchase_date AND effective_date >= purchase_date - 6mo` |
| **Domain-100 coverage** | Every domain class must be 100% line-covered by tests |
| **No mocks in API tests** | RestAssured tests must hit real running app; Testcontainers for external integrations |

---

## 5. Dependencies Not Yet Declared

Agent identified these will be needed:

| Dependency | Reason |
|------------|--------|
| `spring-boot-starter-validation` | `@Valid`, `@NotBlank`, `@Positive`, `@Size` for request validation |
| `RestClient` / `RestTemplate` | HTTP client for Treasury API |
| Testcontainers + PostgreSQL | Test persistence |
| `spring-boot-starter-cache` + caffeine | Rate caching (implied by 6-month window, avoid repeated lookups) |
| RestAssured + Testcontainers | Testing mandate from AGENTS.md |

---

## 6. Summary Table

| Feature | Required                                          | Existing | Gap |
|---------|---------------------------------------------------|----------|-----|
| Purchase entity | `domain/model/Purchase`                           | None | Full creation |
| Purchase repository | `domain/port/outbound/PurchaseRepositoryPort`     | None | Full creation |
| Treasury rate client | `domain/port/outbound/TreasuryRatePort`           | None | Full creation |
| Purchase controller | `infra/client/PurchaseControllerV1`               | None | Full creation |
| Purchase use case | `application/service/purchase/PurchaseUseCaseImpl` | None | Full creation |
| Treasury rate adapter | `infra/client/TreasuryRateClientAdapter`          | None | Full creation |
| JPA adapter | `infra/persistence/PurchaseJpaAdapter`            | None | Full creation |
| Validation | Request validation                                | None | Full creation |
| Exception handling | `@ControllerAdvice`                               | None | Full creation |
| Architecture tests | Package-layer enforcement                         | ✅ ArchitectureTest base + 4 rule tests | None — scaffold passes, will test new packages |
| Build config | HTTP client, validation, DB, cache deps           | Only web+jpa+jackson | Add validation, cache, HTTP client, test deps |

---

## 7. File Inventory

**Production files checked:**
```
src/main/kotlin/com/charlesluxinger/wex_transactions/Application.kt
src/main/resources/application.yaml
build.gradle.kts
settings.gradle.kts
```

**Test files checked:**
```
src/test/kotlin/.../ApplicationTests.kt
src/test/kotlin/.../architecture/ArchitectureTest.kt           (base)
src/test/kotlin/.../architecture/ArchitectureScaffoldTest.kt   (scaffold verification)
src/test/kotlin/.../architecture/DependencyDirectionTest.kt    (Rule 1)
src/test/kotlin/.../architecture/ControllerBoundaryTest.kt     (Rule 2)
src/test/kotlin/.../architecture/UseCaseOwnershipTest.kt       (Rule 3)
src/test/kotlin/.../architecture/VacuousGuardTest.kt           (vacuous-pass prevention)
```

**Config files checked:**
```
.editorconfig
config/detekt/detekt.yml
config/detekt/baseline.xml
.github/workflows/ci.yml
```
