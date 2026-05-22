# WEX Transactions — Agent Guide

## Source of truth (order)
1. `build.gradle.kts`
2. `.github/workflows/ci.yml`
3. `.editorconfig`
4. `config/detekt/detekt.yml`
5. `.specs/**` and `.docs/**` (context only)

## Architecture constraints to keep
- Target: Hexagonal + DDD.
- Dependency direction: `infra -> application -> domain`.
- Domain layer must not depend on `application` or `infra`.
- Controllers call inbound ports/use cases only (no repository/adapter access).
- Use-case implementations belong in `application/service/**` as `*UseCaseImpl`.

### Package Details

| Component | Package | Rule |
|-----------|---------|------|
| Controllers | `infra/client/<feature>/` | Named `<Feature>ControllerV1`. Only calls application ports. |
| Inbound ports | `domain/port/inbound/` | Interfaces: `<Feature>QueryPort`, `<Feature>CommandPort`. |
| Outbound ports | `domain/port/outbound/` | Interfaces: `<Feature>RepositoryPort`, `<Feature>ClientPort`. |
| Adapters | `infra/` | Named `<Feature><Tech>Adapter` (e.g. `ParkingQueryPortJPAAdapter`). **Never** `*PortImpl`. |
| Use case impls | `application/service/<feature>/` | Named `<UseCase>Impl`. Framework-agnostic orchestration. |
| Commands/Queries | `domain/port/inbound/<feature>/model/` | Named `<Action><Domain>Command` / `<Action><Domain>Query`. |
| DTOs / view models | `domain/port/inbound/<feature>/model/` | **Never** inline in service files. |

## Local verification workflow (run in order)
```powershell
./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew detekt
./gradlew test
```
- `test` auto-runs `jacocoTestReport` (`finalizedBy`).
- Do not treat `./gradlew build` as equivalent to the ordered workflow above.

## CI gate order (must match)
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
- PR coverage reporting also enforces: overall `90` and changed-files `90`.

## Focused test commands
```powershell
./gradlew test --tests "com.charlesluxinger.wex_transactions.architecture.DependencyDirectionTest"
./gradlew test --tests "com.charlesluxinger.wex_transactions.architecture.*"
./gradlew test --tests "*DependencyDirection*"
```

## Quality gates and limits
- JaCoCo verification thresholds configured: overall `0.9`, domain package `1.0`.
- Current CI/local flow runs `test` + `jacocoTestReport`; it does not run `jacocoTestCoverageVerification`.
- JaCoCo exclusions: `**/config/**`, `**/health/**`.
- Detekt baseline: `config/detekt/baseline.xml`.
- Update baseline only when intentional: `./gradlew detektBaseline`.
- Formatting: ktlint official, 4 spaces, max line 120, LF.
- Detekt forbids `TODO:`, `FIXME:`, `STOPSHIP:` comments.

## Testing policy by layer
- Team policy target: TDD (`RED -> GREEN -> REFACTOR`).
- Currently declared test libs: `spring-boot-starter-test`, `kotlin-test-junit5`, `archunit-junit5`.
- Planned implementation policy from `.specs/codebase/TESTING.md`:
  - Unit tests: JUnit5 + Mockito allowed.
  - API tests: RestAssured; no mocks/stubs/fakes.
  - Integration/E2E: Testcontainers with real dependencies.

## Graphify

This project has a knowledge graph at `graphify-out/` with god nodes, community structure, and cross-file relationships.

**Rules:**
- ALWAYS read `graphify-out/GRAPH_REPORT.md` before reading source files, running grep, or answering codebase questions.
- For cross-module questions, prefer `graphify query` / `graphify path` / `graphify explain` over grep — graph traverses EXTRACTED + INFERRED edges.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

**code-review-graph MCP tools** (use before grep/read for codebase exploration):
| Tool | Use when |
|------|----------|
| `detect_changes` | Code review — risk-scored change analysis |
| `get_review_context` | Source snippets for review — token-efficient |
| `get_impact_radius` | Blast radius of a change |
| `get_affected_flows` | Execution paths impacted |
| `query_graph` | Trace callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Find functions/classes by name or keyword |
| `get_architecture_overview` | High-level codebase structure |
| `list_communities` | Module communities and relationships |

**Workflow:** `detect_changes` → `get_affected_flows` → `query_graph` (tests_for) → review.

## Useful paths
- App entrypoint: `src/main/kotlin/com/charlesluxinger/wex_transactions/Application.kt`
- Root package: `com.charlesluxinger.wex_transactions`
- Planned implementation details: `.specs/features/wex-tech-challenge/`

## Mandatory Agent Rules
- File: `.agents/RULES.md`.
- Status: mandatory for all agents.
- Apply all constraints in that file.
