# TESTING

Mandatory strategy:
- TDD (RED -> GREEN -> REFACTOR)
- API tests: RestAssured
- No mocks in API tests
- Integration/E2E: Testcontainers
- Unit tests: JUnit5, Mockito

Quality gates:
- ktlint checks pass
- detekt checks pass
- tests pass
- JaCoCo overall >= 90%
- JaCoCo domain package = 100%
