# ROADMAP — WEX Transactions Tech Challenge

## Milestone M1 — Foundation and Guardrails
Goal: establish architecture skeleton and quality baseline before feature logic.

Deliverables:
1. Layer/package scaffold (`domain`, `application`, `infra`) with naming conventions.
2. Build dependencies and app config baseline (validation, HTTP client strategy, DB/runtime decision, tests).
3. Architecture tests remain green and non-vacuous.

Exit criteria:
- Build compiles
- Architecture tests pass
- No quality gate regressions

---

## Milestone M2 — Feature A: Store Purchase
Goal: vertical slice for purchase creation.

Deliverables:
1. Domain model/value objects + inbound/outbound ports.
2. Application use case implementation.
3. Persistence adapter + DB mapping.
4. REST endpoint for create purchase.
5. TDD coverage for domain/application/infra behavior and validations.

Exit criteria:
- Creation workflow returns generated unique identifier
- Validation/business errors enforced
- Tests and coverage gates pass

---

## Milestone M3 — Feature B: Retrieve Converted Purchase
Goal: retrieval with currency conversion using Treasury-only rates.

Deliverables:
1. Treasury rate outbound port + adapter.
2. Rate lookup policy (`rate_date <= purchase_date`, within prior 6 months inclusive).
3. Conversion use case/query endpoint.
4. Rounding and response fields from business spec.

Exit criteria:
- Converted response contains required fields
- Conversion-unavailable error behavior covered
- Tests and coverage gates pass

---

## Milestone M4 — Cross-Cutting Hardening
Goal: finalize error taxonomy, compliance, documentation traceability.

Deliverables:
1. Consistent exception mapping strategy.
2. Requirement-to-test traceability matrix.
3. Final verification run (ktlint, detekt, tests, JaCoCo).

Exit criteria:
- All required checks green
- No unresolved ambiguity impacting acceptance
- Documentation complete for reviewer handoff