# Context — Pending Clarifications (Do Not Assume)

These decisions are required before implementation details are finalized.

## Functional Contract
1. Purchase ID format: UUID, numeric sequence, or other?
2. `transactionDate` accepted format: `yyyy-MM-dd` only, or datetime/timezone accepted?
3. `targetCurrency` format: ISO-4217 3-letter code? case-insensitive?
4. Error response contract: RFC7807/9457 Problem Details or custom JSON?
5. Required HTTP status codes per business error category?
6. Is duplicate purchase submission allowed, or idempotency required?

## Treasury Integration Behavior
7. Treasury API endpoint and response format to standardize (JSON dataset API path + query params)?
8. If Treasury is unavailable/timeouts, what behavior is expected (retry, fail-fast, fallback)?
9. Should exchange rates be cached? If yes, cache key + TTL policy?
10. Expected precision/storage format for exchange rate value?

## Data + Environment
11. Runtime DB for challenge delivery: H2, PostgreSQL, or both profiles?
12. Any required migration strategy (Flyway/Liquibase) expected in challenge evaluation?

## Acceptance/Review Preferences
13. Preferred endpoint style/versioning (`/api/v1/...`) for evaluator readability?
14. Any explicit payload examples expected in final documentation?

## Resolved Decisions
1. Purchase ID format: `Long`.
2. `transactionDate` format: `yyyy-MM-dd'T'HH:mm:ssXXX`.
3. `targetCurrency` format: ISO-4217.
4. Error contract: Problem Details.
5. Treasury outage behavior: retry + fail-fast.
6. Runtime DB for challenge: PostgreSQL.

## Remaining Clarifications
- None currently blocking requirement breakdown.

## Status
- Stakeholder answers applied.
- `spec.md` and `tasks.md` must reflect these fixed decisions.