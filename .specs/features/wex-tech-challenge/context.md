# Context — Pending Clarifications (Do Not Assume)

These decisions are required before implementation details are finalized.

## Functional Contract
1. Purchase ID format: UUID, numeric sequence, or other? numeric sequence
2. `transactionDate` accepted format: `yyyy-MM-dd` only, or datetime/timezone accepted? `yyyy-MM-dd'T'HH:mm:ssXXX`
3. `targetCurrency` format: ISO-4217 3-letter code? case-insensitive? ISO-4217 3-letter code
4. Error response contract: RFC7807/9457 Problem Details or custom JSON? Problem Details
5. Required HTTP status codes per business error category? unresolved (must be documented in final spec).
6. Is duplicate purchase submission allowed, or idempotency required? unresolved

## Treasury Integration Behavior
7. Treasury API endpoint and response format to standardize (JSON dataset API path + query params)? unresolved
8. If Treasury is unavailable/timeouts, what behavior is expected (retry, fail-fast, fallback)? retry, fail-fast
9. Should exchange rates be cached? If yes, cache key + TTL policy? No
10. Expected precision/storage format for exchange rate value? DECIMAL(18,6) with HALF_UP when rounding is required. (user-confirmed)

## Data + Environment
11. Runtime DB for challenge delivery: H2, PostgreSQL, or both profiles? PostgreSQL
12. Any required migration strategy (Flyway/Liquibase) expected in challenge evaluation? Flyway

## Acceptance/Review Preferences
13. Preferred endpoint style/versioning (`/api/v1/...`) for evaluator readability? deferred until implementation contract phase
14. Any explicit payload examples expected in final documentation? deferred until final spec
