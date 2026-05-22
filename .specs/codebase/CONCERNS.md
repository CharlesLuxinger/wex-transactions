# CONCERNS

1. Business spec has no API contract details; risk of evaluator mismatch.
2. Treasury API behavior on outages is unspecified.
3. DB runtime choice is unspecified.
4. Domain 100% coverage gate is strict; requires complete unit test discipline.
5. Architecture tests currently scaffolded; new code can introduce boundary violations if not carefully layered.
6. Conversion date-window semantics must be tested for edge boundaries (exactly 6 months).