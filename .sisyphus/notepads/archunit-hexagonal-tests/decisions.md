# Decisions

## 2026-05-22 Session Init

- ArchUnit version: `1.4.2` (plan mentions 1.4.2; use latest stable from Maven Central — executor should verify)
- Scaffold vacuous-pass strategy: `if (classes.isEmpty()) return` pattern (matches estaparking reference)
- INFRA_CLIENT_PACKAGE pattern: `${INFRA_PACKAGE}client..` = `com.charlesluxinger.wex_transactions.infra..client..`
- No `@ExtendWith(ArchUnitRunner::class)` — plain JUnit5 `@Test` pattern (matches estaparking reference)
- Architecture test package: `src/test/kotlin/com/charlesluxinger/wex_transactions/architecture/`
