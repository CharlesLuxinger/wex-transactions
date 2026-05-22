# ESTAPARKING BACKEND — KNOWLEDGE BASE

## OVERVIEW

Spring Boot 3.5 / Kotlin 2.3 / Java 25 REST API. Target architecture: Hexagonal (Ports & Adapters) + DDD.

**Package root:** `com.charlesluxinger.wex_transactions`
**Entry point:** `src/main/kotlin/com/charlesluxinger/wex_transactions/Application.kt`

## PLANNED ARCHITECTURE

| Layer | Package | Rule |
|-------|---------|------|
| Domain | `domain/` | Core business logic (entities, value objects, domain services). Zero Spring annotations. |
| Application | `application/` | Use case implementations (`UseCaseImpl`). Orchestrates domain logic. No framework dependencies. |
| Infrastructure | `infra/` | Framework adapters (persistence, external clients, web controllers). Depends on application. |

### Governance Rules

1. **Strict Dependency Direction:** `infra/` → `application/` → `domain/`.
2. **Controller Boundary:** Web controllers (`infra/client/`) must invoke inbound ports or use cases only. Direct access to repositories or infrastructure adapters is strictly forbidden.
3. **Use Case Ownership:** Use case implementations belong to the application layer. They coordinate domain objects but do not contain core business rules (which belong in the domain).

### Architecture Governance Checks

Use these commands to verify architectural integrity. A match (output) usually indicates a violation unless specified.

1. **Strict Dependency Rule:** Domain must NOT import Application or Infra.
   - **Bash (CI/Linux only):**
     ```bash
     grep -r "import.*\.application\|\.infra" src/main/kotlin/com/charlesluxinger/wex_transactions/domain
     ```
   - **PowerShell (Windows/local authoritative):**
     ```powershell
     if (Test-Path "src/main/kotlin/com/charlesluxinger/wex_transactions/domain") { Get-ChildItem -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/domain" -Recurse -Filter "*.kt" | Select-String -Pattern "import.*\.application", "import.*\.infra" }
     ```

2. **Controller Boundary:** Controllers must NOT import repositories or adapters.
   - **Bash (CI/Linux only):**
     ```bash
     grep -r "import.*\.repository\|\.adapter" src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client
     ```
   - **PowerShell (Windows/local authoritative):**
     ```powershell
     if (Test-Path "src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client") { Get-ChildItem -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/infra/client" -Recurse -Filter "*.kt" | Select-String -Pattern "import.*\.repository", "import.*\.adapter" }
     ```

3. **Use-Case Ownership:** Use cases must be in the application layer.
   - **Bash (CI/Linux only):**
     ```bash
     # Check if UseCaseImpl exists outside application/service
     find src/main/kotlin -name "*UseCaseImpl.kt" | grep -v "application/service"
     ```
   - **PowerShell (Windows/local authoritative):**
     ```powershell
     Get-ChildItem -Path "src/main/kotlin" -Recurse -Filter "*UseCaseImpl.kt" | Where-Object { $_.FullName -notmatch "application\\service" }
     ```

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

## NAMING

- Test files: `*Test.kt` (unit) | `*IT.kt` (integration)
- Specs: `.spec/<YYYYMMDD-title.md>`

## COMMANDS

Run quality gates locally in this order (matches CI):

```powershell
./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew detekt
./gradlew test
```

`test` auto-triggers JaCoCo report (`finalizedBy`). Shortcut: `./gradlew build` runs all gates.

## CODE QUALITY GATES

| Tool | Config | Hard limit |
|------|--------|-----------|
| ktlint | `.editorconfig` — `ktlint_official` style, 4-space indent, 120-char lines | `ignoreFailures = false` |
| detekt | `config/detekt/detekt.yml` | `ignoreFailures = false` |
| jacoco | `build.gradle.kts` | 90% overall + 90% on changed files (CI enforces both) |

**Detekt limits to know** (non-default values in `detekt.yml`):
- `CyclomaticComplexMethod.allowedComplexity` = 14
- `LongMethod.allowedLines` = 60
- `ReturnCount.max` = 2 (excludes `equals`, excludes return-from-lambda)
- `LongParameterList`: 5 function params, 6 constructor params

**Jacoco exclusions:** `**/config/**`, `**/health/**` are excluded from coverage measurement.

**Detekt baseline:** `config/detekt/baseline.xml` tracks accepted violations. Update it when introducing a justified new violation (`./gradlew detektBaseline`).

## STYLE

Enforced by `.editorconfig` (ktlint reads it automatically):
- `ktlint_official` code style
- `ktlint_standard_package-name` = disabled (package name rule off)
- 120-char max line, 4-space indent, LF line endings

## SKILLS

Load these skills when working on the relevant area:

| Task | Skill to load |
|------|--------------|
| Domain modeling, ports, adapters, use cases, DDD | `clean-ddd-hexagonal` |
| Kotlin idioms, null safety, sealed classes, DSL | `kotlin-patterns` |
| Coroutines, Flow, StateFlow, async patterns | `kotlin-coroutines-flows` |
| Full Kotlin (coroutines + KMP + Compose + Ktor) | `kotlin-specialist` |
| Spring Boot + Kotlin wiring, DI, transactions | `kotlin-springboot` |

Skills live in `.agents/skills/<name>/SKILL.md`. The `clean-ddd-hexagonal` skill includes reference files in `.agents/skills/clean-ddd-hexagonal/references/` covering hexagonal, layers, DDD tactical/strategic, CQRS, and testing patterns.

## CI

Triggers on PR and push to `main`. Steps: `ktlintMainSourceSetCheck ktlintTestSourceSetCheck` → `detekt` → `test jacocoTestReport`. Badges generate on `pull_request` and commit to the PR branch. Coverage posted as PR summary comment.

## graphify

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

## SOURCE OF TRUTH

When documentation conflicts with executable config, follow the executable config. CI workflow (`.github/workflows/ci.yml`), build config (`build.gradle.kts`), and formatter config (`.editorconfig`) are authoritative — they reflect runtime truth.
