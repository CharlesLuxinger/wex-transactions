# DECISIONS — WEX Tech Challenge

## Purpose
This document fixes API/business decision tokens needed by implementation tasks.
Source precedence is respected from `AGENTS.md`: `build.gradle.kts` → CI/workflow/config files → `.specs/**` and `.docs/**`.
When source material is ambiguous, the item is marked explicitly with `UNRESOLVED:`.

## Decision Ledger Format
Each section includes:
- **What**: fixed decision token/behavior
- **Why**: business or architectural reason
- **Evidence**: exact source references
- **Rationale**: implementation-facing interpretation constrained by source

---

## 1) Purchase ID Format

**What**
- Purchase identifier type is **`Long`**, represented as a numeric sequence, unique per stored purchase, immutable after persistence.

**Why**
- Business requires a generated unique identifier on successful storage.
- Derived spec defines immutable decision token `Long`.
- Roadmap requires `Long` end-to-end.

**Evidence**
- `.docs/wex.md` lines 39–42 (unique identifier required and returned)
- `.docs/SPEC.md` lines 20–26 (`Long` immutable token), 39, 47, 81
- `.specs/features/ROADMAP.md` line 8 (`Long` end-to-end)
- `.specs/features/wex-tech-challenge/context.md` line 6 (numeric sequence clarification)

**Rationale**
- `Long` aligns with fixed token consistency across create/retrieve flows and avoids contract drift across milestones.
- “Immutable after store” is an interpretation of identifier semantics; no source permits post-persistence mutation.

---

## 2) Transaction Date Format

**What**
- Input accepts ISO-8601 equivalent date/time values.
- Before persistence, value is normalized to canonical format: **`yyyy-MM-dd'T'HH:mm:ssXXX`**.

**Why**
- Business rule requires valid date format.
- Derived spec and roadmap explicitly fix accepted variant handling + canonical normalization.

**Evidence**
- `.docs/wex.md` lines 32–35, 67
- `.docs/SPEC.md` lines 24, 37, 51, 82
- `.specs/features/ROADMAP.md` line 7
- `.specs/features/wex-tech-challenge/context.md` line 7

**Rationale**
- Accepting ISO-8601 equivalents reduces client coupling while preserving deterministic stored representation for comparisons and auditability.

**RESOLVED: Accepted ISO-8601 subset boundaries**
- Accepted variants: ISO-8601 offset datetime (`2026-05-23T12:00:00Z`), zoned datetime, and local datetime.
- All variants are normalized to canonical format: `yyyy-MM-dd'T'HH:mm:ssXXX` (UTC).
- Non-ISO or non-normalizable input fails with `IllegalArgumentException`.
- Evidence: `TransactionDate.parse()` implementation in domain model.

---

## 3) Target Currency Format

**What**
- Target currency uses **ISO-4217 3-letter code** (e.g., `USD`, `EUR`).

**Why**
- Derived spec locks `ISO-4217` as immutable token.
- Business conversion requirement assumes explicit target currency semantics.

**Evidence**
- `.docs/SPEC.md` lines 22, 83
- `.docs/wex.md` lines 43–53 (target currency conversion behavior)
- `.specs/features/wex-tech-challenge/context.md` line 8

**Rationale**
- ISO-4217 ensures predictable interoperability and validation boundaries for Treasury rate lookup.

**RESOLVED: Case-sensitivity policy at API boundary**
- Currency codes are accepted case-insensitively (`^[A-Za-z]{3}$`) and normalized to uppercase via `Currency.getInstance()`.
- The domain value object `TargetCurrency` handles normalization, converting the input code to uppercase for internal representation.
- Evidence: `TargetCurrency` domain model implementation.

---

## 4) Exchange Rate Precision

**What**
- Exchange-rate value is treated as **DECIMAL(18,2)**.
- Rounding mode is **HALF_UP** where rounding is required.
- Schema migration applied: `DECIMAL(18,6)` → `DECIMAL(18,2)` across all monetary columns (`transaction_amount`, `exchange_rate`, `converted_amount`).

**Why**
- Precision/rounding must be deterministic for reproducible currency conversion.
- Business constraint confirmed: final monetary values require 2 decimal places — internal 6-decimal precision introduced unnecessary complexity without business justification.
- Scale 2 is enforced at domain level via `BigDecimal.scale()` validation and `setScale(2, HALF_UP)` in use cases.

**Evidence**
- `.specs/features/wex-tech-challenge/context.md` line 17 (explicitly user-confirmed)
- `.docs/wex.md` lines 52 and 38 (monetary rounding constraints to 2 decimals for output/purchase amount)
- DECISIONS.md item 4.1 (migration record)

**Rationale**
- Scale 2 aligns DB precision with domain monetary model and eliminates unnecessary scale conversions.
- `HALF_UP` rounding ensures deterministic, audit-friendly results.

---

## 5) Error Response Format

**What**
- Error contract is **RFC 7807 Problem Details** (token: `Problem Details`).

**Why**
- Derived spec fixes this as immutable contract token and explicitly ties conversion-unavailable business error to Problem Details.

**Evidence**
- `.docs/SPEC.md` lines 23, 53, 61, 84
- `.specs/features/wex-tech-challenge/context.md` line 9

**Rationale**
- Standardized error payloads reduce ambiguity for client handling and API test assertions.

**RESOLVED: Exact HTTP status mapping per business error category**
- 400 Bad Request: Validation errors (`IllegalArgumentException` from domain validation, Bean Validation constraint violations).
- 404 Not Found: `PurchaseNotFoundException` (purchase ID not found).
- 422 Unprocessable Entity: `RateUnavailableException` (no eligible exchange rate in 6-month window), `IllegalStateException` (rate fetch failure).
- 500 Internal Server Error: Unhandled infrastructure errors (generic `Exception` handler — no stack trace leaked).
- 503 Service Unavailable: `FeignException` (Treasury API unavailable).
- Evidence: `GlobalExceptionHandler` implementation (updated with FeignException, IllegalStateException, generic Exception handlers).

---

## 6) Treasury API Endpoint

**What**
- Canonical Treasury endpoint: `/rates_of_exchange` via `https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange`.
- Query parameters: `fields`, `filter` (date range), `sort`, `page[size]`.

**Why**
- Treasury Fiscal Data API is the definitive source for exchange rates.
- Filter by date range ensures bounded response and supports the 6-month window rule.

**Evidence**
- `TreasuryFeignClient.fetchRates()` Feign client configuration.
- `ExchangeRateTreasuryAdapter.FILTER_FORMAT = "record_date:lte:%s,record_date:gte:%s"`.

**Rationale**
- Direct Treasury API integration avoids third-party aggregation dependencies.
- Date filtering limits response payload size and ensures deterministic sorting.

---

## 7) Retry Policy (Treasury Integration)

**What**
- Treasury outage behavior is **`retry + fail-fast`**.

**Why**
- Derived spec fixes token and forbids invented operational parameters without source backing.

**Evidence**
- `.docs/SPEC.md` lines 25, 85
- `.specs/features/wex-tech-challenge/context.md` line 16

**Rationale**
- `retry` handles transient failures; `fail-fast` bounds latency/error amplification when conditions are non-transient.

**RESOLVED: Retry parameters**
- Max attempts: 3 (configured in `JitterRetryer.maxAttempts`).
- Backoff algorithm: exponential with jitter — `initialBackoff * 1.5^(attempt-1)` + random jitter.
- Backoff range: 500ms–2000ms (configured in `JitterRetryer.initialBackoff` / `maxBackoff`).
- Fail-fast trigger: non-retryable HTTP status codes (4xx except 429) produce `FeignException` directly.
- Evidence: `TreasuryFeignConfig.JitterRetryer` implementation.

**RESOLVED: Retry + Resilience4j CircuitBreaker coexistence**
- Feign-level retry (`JitterRetryer`) handles transient network failures with exponential backoff + jitter (3 attempts).
- Resilience4j CircuitBreaker (`treasury-rates`) prevents cascading failures when Treasury is degraded:
  - Sliding window: 10 calls, minimum 5 calls before opening.
  - Failure threshold: 50%.
  - Open duration: 30s before half-open.
- Resilience4j Bulkhead (`treasury-rates`) limits concurrent Treasury calls (max 5 concurrent, 500ms max wait).
- Evidence: `application.yaml` resilience4j config, `ExchangeRateTreasuryAdapter` annotations.

---

## 8) Cache Policy

**What**
- **No exchange-rate caching** is permitted.

**Why**
- Explicit user clarification and challenge scope simplicity; avoids stale-rate behavior ambiguity.

**Evidence**
- `.specs/features/wex-tech-challenge/context.md` line 17 (`No`)
- `.docs/wex.md` lines 14–17, 47–51 (Treasury-only source and date-window rules)

**Rationale**
- No-cache keeps behavior directly tied to Treasury source and avoids unsourced TTL/invalidation assumptions.

---

## 9) 6-Month Rule

**What**
- Eligible rate must satisfy both:
  1. rate date `<=` purchase date
  2. rate date within **prior 6 months inclusive** from purchase date
- Boundary examples required:
  - exactly 6 months difference: accepted
  - 6 months + 1 day: rejected

**Why**
- This is a core business acceptance criterion for conversion eligibility.

**Evidence**
- `.docs/wex.md` lines 48–51, 68
- `.docs/SPEC.md` lines 44–45
- `.specs/features/ROADMAP.md` lines 95–97, 125

**Rationale**
- Combines historical fallback with bounded staleness.
- Inclusive boundary language must be enforced consistently in conversion-unavailable logic.

**UNRESOLVED: Exact calendar arithmetic rule for “6 months”**
- Sources require “prior 6 months inclusive” but do not define algorithm (calendar-month subtraction vs fixed-day-count).
- Must be finalized before implementation to prevent boundary inconsistency.

---

## 10) API Contract Details

**What**
- `targetCurrency` is a **required** query parameter acting as the conversion currency selector.
- List/collection endpoints and DELETE operations are explicitly **out of scope** for the current implementation.

**Why**
- Required parameter guarantees explicit user intent for currency conversion, preventing ambiguous default behavior.
- Scope limitation avoids unbounded CRUD expectations that the business requirement does not mandate.

**Evidence**
- `RetrieveConvertedQueryPort` interface — `targetCurrency` is a required parameter.
- Store-only scope documented in `AGENTS.md` and current controller surface.

**Rationale**
- Explicit required parameter reduces magical behavior and documents the API contract clearly at the boundary.

---

## 11) Idempotency

**What**
- **Idempotency is NOT implemented.** Duplicate POST submissions create duplicate purchase records.

**Why**
- No business requirement for idempotency key or deduplication.
- Current behavior is intentional: each POST creates a new purchase.
- Future idempotency support would require an idempotency key header.

**Evidence**
- `PurchaseControllerV1` — no idempotency check, no deduplication logic.
- `StorePurchaseUseCaseImpl` — always creates a new purchase.

**Rationale**
- Adding idempotency without business requirement introduces unnecessary complexity.
- Documented to prevent incorrect assumptions about idempotent behavior.

---

## 12) Business Error Taxonomy

**What**
Minimal taxonomy stays fixed to four business categories:
1. invalid description length (>50)
2. invalid transaction date format
3. invalid purchase amount (missing/non-numeric/zero/negative)
4. conversion unavailable (no eligible rate in window)

**Why**
- Business and roadmap explicitly require minimal, non-expanded taxonomy.

**Evidence**
- `.docs/wex.md` lines 63–69, 80
- `.docs/SPEC.md` lines 50–54
- `.specs/features/ROADMAP.md` lines 10, 70, 85, 122

**Rationale**
- Fixed taxonomy avoids scope creep and preserves stable acceptance behavior across milestones.

---

## Cross-Decision Consistency Check

- `Long` ID token is consistent across create/retrieve/roadmap.
- Date acceptance + canonical normalization is consistent across business/derived/roadmap docs.
- Currency format token (`ISO-4217`) does not conflict with treasury-only source policy.
- Retry behavior (`retry + fail-fast`) is fixed with Resilience4j CircuitBreaker + Bulkhead governance.
- No-cache policy introduces no contradiction with Treasury-only requirement.
- 6-month inclusive window uses calendar-month subtraction (`minusMonths(6)`).
- Error taxonomy remains exactly four categories.
- HTTP status mapping is fully resolved: 400 (validation), 404 (not-found), 422 (conversion), 503 (infra unavailable), 500 (unhandled).
- Idempotency: explicitly not implemented; duplicate POST creates new record.

---

## Consolidated UNRESOLVED Register

1. **UNRESOLVED: Intermediate rounding sequence** for rate/amount operation chain.

No unresolved item above has been implicitly assumed as fixed behavior.
