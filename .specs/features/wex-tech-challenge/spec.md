# Feature Spec — WEX Tech Challenge

## Source Documents
- Business: `.docs/wex.md`
- Implementation baseline: `.docs/SPEC.md`
- Architecture constraints: `AGENTS.md`

## Feature Set
- F-A: Store Purchase Transaction
- F-B: Retrieve Purchase Converted to Target Currency
- F-C: Error Handling and Business Failures

## Requirements (Traceable IDs)

### F-A Store Purchase
- REQ-01: `description` is required and max 50 chars.
- REQ-02: `transactionDate` is required and must follow `yyyy-MM-dd'T'HH:mm:ssXXX`.
- REQ-03: `purchaseAmount` is required, positive, USD, rounded to 2 decimals.
- REQ-04: System generates and returns unique purchase identifier in `Long` format.

### F-B Retrieve Converted Purchase
- REQ-05: Exchange source is Treasury Reporting Rates only.
- REQ-06: Selected rate date must be <= purchase date.
- REQ-07: Eligible historical window is prior 6 months inclusive.
- REQ-08: If no eligible rate exists, return business error.
- REQ-09: Converted amount rounded to 2 decimals.
- REQ-10: Response includes purchase id (`Long`), description, date, original USD amount, exchange rate used, converted amount.

### F-C Error Taxonomy
- REQ-11: Error for description length > 50.
- REQ-12: Error for invalid date format (must match `yyyy-MM-dd'T'HH:mm:ssXXX`).
- REQ-13: Error for invalid purchase amount (missing/non-numeric/zero/negative).
- REQ-14: Error for conversion unavailable in allowed window, using Problem Details contract.

### Quality + Architecture Constraints
- REQ-15: JaCoCo overall threshold >= 90%.
- REQ-16: Domain package coverage = 100%.
- REQ-17: ktlint checks pass (no failures).
- REQ-18: detekt checks pass (no failures).
- REQ-19: TDD mandatory for feature development.
- REQ-20: API tests use RestAssured and no mocks (error responses validated as Problem Details).
- REQ-21: Integration/E2E uses Testcontainers.
- REQ-22: Layer dependency direction `infra -> application -> domain` only.
- REQ-23: Controllers do not import repositories/adapters directly.
- REQ-24: UseCase implementations live in `application/service/**`.
- REQ-25: Domain stays framework-free (no Spring annotations).

## Dependency Graph
- F-A must exist before full F-B (retrieval depends on persisted purchase).
- F-C is shared by F-A and F-B.
- Infrastructure setup (deps/config) underpins all features.

## Acceptance Slice Mapping
- A1: Create purchase success + returned unique ID. (REQ-01..04)
- A2: Create purchase validation failures. (REQ-11..13)
- B1: Retrieve converted purchase with eligible rate. (REQ-05..07,09,10)
- B2: Conversion unavailable failure path. (REQ-08,14)
- G1: Quality/architecture gates all green. (REQ-15..25)

## Fixed Contract Decisions
- Purchase ID type: `Long`
- Transaction date format: `yyyy-MM-dd'T'HH:mm:ssXXX`
- Target currency format: ISO-4217
- Error contract: Problem Details
- Treasury outage behavior: retry + fail-fast
- Runtime DB: PostgreSQL

## Non-Assumption Policy
Ambiguous behavior is not guessed. Remaining open decisions, if any, stay in `context.md`.