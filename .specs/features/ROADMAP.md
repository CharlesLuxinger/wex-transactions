# ROADMAP — WEX Transactions Tech Challenge

## Roadmap Policy
- Scope stays milestone-level, not implementation task breakdown.
- Technical guardrails are explicit exit criteria, never implementation steps.
- Challenge business policy is authoritative (`.docs/wex.md`), including 6-month rule.
- Date input accepts ISO-8601 equivalents, with canonical normalization to `yyyy-MM-dd'T'HH:mm:ssXXX` before persistence.
- Purchase identifier remains `Long` end-to-end.
- Runtime acceptance target is PostgreSQL-only.
- Minimal business error taxonomy only (4 errors from `.docs/wex.md`).
- Light endpoint-shape notes are allowed only for reviewer readability.

Mandatory evidence command order (AGENTS-aligned):
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
Local pre-evidence hygiene (recommended):
```bash
./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew detekt
./gradlew test
```
Coverage checks required in every milestone exit:
- overall >= 90%
- changed files >= 90%
- domain package coverage = 100%
- architecture tests remain non-vacuous

---

## Milestone M1 — Foundation and Guardrails
Goal: establish architecture and quality baseline before feature slices.

Exit criteria:
- Architecture checks explicitly pass and are non-vacuous:
  1. dependency direction (`infra -> application -> domain`)
  2. controller boundary (controllers call inbound ports only)
  3. use-case ownership (`application/service/**`, `*UseCaseImpl`)
- Quality gates pass in required order.
- Coverage thresholds hold (overall/changed-files/domain-100).

Evidence block:
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
Risks:
- Empty-pass architecture tests can hide boundary regressions.

Non-goals:
- No purchase creation behavior definition.
- No conversion behavior definition.
- No error-payload contract expansion.

---

## Milestone M2 — Feature A: Store Purchase
Goal: validate and store purchase transaction with generated `Long` identifier.

Exit criteria:
- Purchase storage acceptance rules are satisfied:
  - description required, <= 50
  - transaction date accepted as ISO-8601 equivalent and normalized canonically
  - USD amount required, positive, rounded to 2 decimals
  - unique `Long` ID returned
- Minimal business error taxonomy applied for create path only.
- Guardrails and coverage thresholds remain green.

Evidence block:
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
Risks:
- Date variant acceptance can drift from canonical persistence rule if not explicitly normalized.

Non-goals:
- No conversion retrieval success criteria.
- No Treasury lookup behavior criteria.
- No expansion beyond four business error categories.

---

## Milestone M3 — Feature B: Retrieve Converted Purchase
Goal: retrieve stored purchase with Treasury-only conversion under challenge policy.

Exit criteria:
- Conversion uses Treasury Reporting Rates only.
- Selected rate date is `<= purchase date`.
- Eligible rate must be within prior 6 months inclusive from purchase date.
- If no eligible rate exists, conversion-unavailable business error is returned.
- Returned data includes required fields (id, description, date, original USD amount, exchange rate used, converted amount).
- Converted amount rounded to 2 decimals.
- Guardrails and coverage thresholds remain green.

Evidence block:
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
Risks:
- External Treasury semantics can be interpreted differently than challenge acceptance policy.

Non-goals:
- No alternative FX provider.
- No generic fallback beyond challenge rule.
- No performance SLA objectives.

---

## Milestone M4 — Cross-Cutting Hardening and Reviewer Handoff
Goal: finalize traceability, compliance evidence, and handoff clarity.

Exit criteria:
- Full REQ-to-milestone traceability matrix complete.
- Business error taxonomy remains minimal and stable.
- Reviewer appendix includes light interface-behavior notes (no endpoint/payload contract freezing in roadmap phase).
- Appendix error-path notes must explicitly include non-ISO input rejection examples to clarify accepted date-format variants.
- Appendix conversion-unavailable samples must include boundary evidence: exactly 6 months (accepted) and 6 months + 1 day (rejected).
- All mandated checks pass in order; coverage thresholds satisfied.
- Architecture checks remain non-vacuous and green.

Evidence block:
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```

Risks:
- Missing traceability artifacts can weaken evaluator confidence.

Non-goals:
- No new functional scope.
- No architecture model change.
- No post-challenge optimization backlog.

## Notes for Consistency
- Keep milestones strategic and reviewer-facing.
- If any policy conflicts appear, apply authority order from `AGENTS.md` / `spec.md`.
