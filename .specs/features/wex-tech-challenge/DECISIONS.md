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

**UNRESOLVED: Accepted ISO-8601 subset boundaries**
- The sources do not enumerate the exact accepted subset (e.g., date-only vs offset datetime variants beyond normalizability).
- Constraint present: non-ISO or non-normalizable input must fail.
- Evidence: `.docs/SPEC.md` line 51; `.specs/features/ROADMAP.md` line 124.

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

**UNRESOLVED: Case-sensitivity policy at API boundary**
- Sources define format family, not whether input is normalized (uppercase) or rejected when lowercase.

---

## 4) Exchange Rate Precision

**What**
- Exchange-rate value is treated as **DECIMAL(18,6)**.
- Rounding mode is **HALF_UP** where rounding is required.

**Why**
- Precision/rounding must be deterministic for reproducible currency conversion.

**Evidence**
- `.specs/features/wex-tech-challenge/context.md` line 17 (explicitly user-confirmed)
- `.docs/wex.md` lines 52 and 38 (monetary rounding constraints to 2 decimals for output/purchase amount)

**Rationale**
- Higher internal precision (`18,6`) prevents avoidable conversion drift before final 2-decimal monetary rounding.

**UNRESOLVED: Intermediate rounding sequence when multiple operations occur**
- Sources define precision and output rounding but do not specify operation-by-operation rounding checkpoints.

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

**UNRESOLVED: Exact HTTP status mapping per business error category**
- Explicitly unresolved in source context.
- Evidence: `.specs/features/wex-tech-challenge/context.md` line 10.

---

## 6) Retry Policy (Treasury Integration)

**What**
- Treasury outage behavior is **`retry + fail-fast`**.

**Why**
- Derived spec fixes token and forbids invented operational parameters without source backing.

**Evidence**
- `.docs/SPEC.md` lines 25, 85
- `.specs/features/wex-tech-challenge/context.md` line 16

**Rationale**
- `retry` handles transient failures; `fail-fast` bounds latency/error amplification when conditions are non-transient.

**UNRESOLVED: Retry parameters**
- Max attempts, backoff algorithm, backoff intervals/jitter, and explicit fail-fast trigger conditions are not specified by source documents.
- Anti-assumption policy forbids guessing these values.
- Evidence: `.docs/SPEC.md` lines 16–18, 93–94.

---

## 7) Cache Policy

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

## 8) 6-Month Rule

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

## 9) Business Error Taxonomy

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
- Retry behavior (`retry + fail-fast`) is fixed at strategy level; numeric parameters remain unresolved by policy.
- No-cache policy introduces no contradiction with Treasury-only requirement.
- 6-month inclusive window aligns with required boundary evidence.
- Error taxonomy remains exactly four categories.

---

## Consolidated UNRESOLVED Register

1. **UNRESOLVED: Accepted ISO-8601 subset boundaries** (normalizable variants not fully enumerated).
2. **UNRESOLVED: Target currency case-sensitivity policy** (uppercase normalization vs strict reject).
3. **UNRESOLVED: Intermediate rounding sequence** for rate/amount operation chain.
4. **UNRESOLVED: HTTP status mapping per business error category** under Problem Details.
5. **UNRESOLVED: Retry parameters** (max attempts, backoff, jitter, explicit fail-fast trigger conditions).
6. **UNRESOLVED: Exact 6-month arithmetic algorithm** (calendar-month vs fixed-day-count computation).

No unresolved item above has been implicitly assumed as fixed behavior.
