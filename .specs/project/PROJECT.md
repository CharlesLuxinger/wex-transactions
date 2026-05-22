# PROJECT — WEX Transactions Tech Challenge

## Vision
Build a production-grade backend API that stores purchase transactions in USD and retrieves converted values using Treasury Reporting Rates, fully aligned with this repository’s Hexagonal + DDD constraints.

## Outcome
Deliver an implementation that:
- satisfies `.docs/wex.md` business rules,
- is traceable to requirements in `.docs/SPEC.md`,
- conforms to `AGENTS.md` architecture governance,
- passes quality gates (ktlint, detekt, tests, JaCoCo including domain-100).

## Scope Boundaries
In scope:
- Purchase creation (validated, persisted, uniquely identified)
- Purchase retrieval converted to target currency
- Treasury-only exchange rate source
- Historical fallback rule (<= purchase date, up to 6 months)
- Business error taxonomy from wex.md

Out of scope (unless clarified by reviewer):
- Alternative FX providers
- Performance SLAs and load testing
- Non-essential platform features not required by challenge

## Constraints (Authoritative)
- Layering: `infra -> application -> domain`
- Domain has no Spring/framework annotations
- Controllers call inbound ports/use-cases only
- Use case implementations under `application/service/**`
- TDD required (RED -> GREEN -> REFACTOR)
- API tests with RestAssured, no mocks
- Integration/E2E with Testcontainers
- Coverage gates: overall >= 90%, changed files >= 90%, domain package = 100%

## Requirement Sources
- Business source: `.docs/wex.md`
- Implementation source: `.docs/SPEC.md`
- Architecture source: `AGENTS.md`

## Open Decisions
See `.specs/features/wex-tech-challenge/context.md`.