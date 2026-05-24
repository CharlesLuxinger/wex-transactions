# Redis Exchange Rates Migration

## TL;DR
> **Summary**: Replace database-backed exchange rate persistence with Redis cache (6-month TTL) + Treasury API fallback. Delete all JPA/DB code. Add forward-only V3 migration to drop table; keep V1/V2 immutable (Flyway safety). Query pattern: Redis (latest rate) → Treasury API → Redis write.
> **Deliverables**: Redis adapter, cache port, V3 migration (immutable V1/V2), updated use case, docker-compose config, deleted JPA artifacts
> **Effort**: Large
> **Parallel**: YES - 4 waves
> **Critical Path**: Task 1 (dependencies) → Task 2 (V3 migration) → Task 3 (cache port) → Task 4 (Redis adapter) → Task 5 (use case refactor) → Task 6 (tests) → Wave 5 (final verification)

## Context
### Original Request
Replace database-first exchange rate lookup with Redis cache. Delete ExchangeRateJpaEntity, all DB persistence, migrations. Cache key: `sourceCurrency:targetCurrency`. TTL: 6 months. Redis in compose.yml.

### Interview Summary
- **Clarifications confirmed**: Redis-only (no DB fallback), latest rate only, skip cache on Redis down → Treasury directly
- **Scope**: Delete JPA entity, adapter, repository, V1/V2 immutable (Flyway history safety), add V3 forward-only migration
- **Currency types**: TargetCurrency confirmed in Purchase model; cache port uses TargetCurrency (not Currency)
- **Cache key locked**: `exchangeRate:sourceCurrency:targetCurrency` (e.g., `exchangeRate:USD:BRL`)
- **TTL locked**: 6 months = Duration.ofDays(180)
- **Outage**: Redis down → skip directly to Treasury API (no DB fallback)

### Metis Review (gaps addressed)
- ✅ DB-role contradiction: Clarified Redis is single source; Treasury API is fallback on cache miss only
- ✅ Rate freshness: Latest rate only, no date filtering; 6-month cache TTL sufficient
- ✅ Outage matrix: Redis down → Treasury direct (no DB); Treasury down → graceful error
- ✅ Port naming: `ExchangeRateCachePort` (domain/port/outbound) + `ExchangeRateCacheKeyBuilder` utility
- ✅ Acceptance criteria: All verifiable via command-line QA (no human intervention)

### Momus Round 1 Blockers (FIXED)
1. **Migration strategy**: Changed from deleting V1/V2 to immutable approach; added forward-only V3 to drop table (Flyway-safe)
2. **Cache port contract**: Unified method signatures: `fun getRate(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency): ExchangeRate?` + `fun saveRate(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency, rate: ExchangeRate): Unit`
3. **Wave 5 QA scenarios**: Added executable F1-F4 scenarios with Tools, Steps, Expected, Evidence for all verification gates

## Work Objectives
### Core Objective
Eliminate database persistence for exchange rates; replace with Redis cache (6-month TTL) as single source of truth; Treasury API as fallback on cache miss; all JPA code deleted; architecture complies with hexagonal + DDD patterns; V1/V2 migrations immutable (Flyway safety); V3 created to drop table; all QA scenarios executable and passing.

### Deliverables
1. Redis service added to compose.yml (8+ container, health check)
2. V3 migration file (forward-only, drops exchange_rates table)
3. ExchangeRateCachePort interface (domain/port/outbound)
4. ExchangeRateCacheKeyBuilder utility (domain/port/outbound)
5. RedisExchangeRateCacheAdapter (infra/adapter/cache)
6. RetrieveConvertedUseCaseImpl refactored (cache-first flow)
7. ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, ExchangeRateRepositoryPort deleted
8. Unit + integration tests (Testcontainers Redis)
9. All QA scenarios passing (F1-F4 gates)

### Definition of Done (verifiable conditions with commands)
- `rtk gradle test` — all tests pass (JUnit5)
- `rtk gradle detekt` — no violations
- `rtk gradle ktlintMainSourceSetCheck ktlintTestSourceSetCheck` — all formatted
- `docker compose up -d && docker compose ps` — Redis + app healthy
- `docker exec redis redis-cli GET "exchangeRate:USD:BRL"` — cache key present after first API call
- `grep -r "ExchangeRateJpaEntity\|ExchangeRateRepository" src/main/kotlin` — zero matches

### Must Have
- Redis container in compose.yml (v8+, health check)
- V3 migration file (immutable V1/V2 preserved)
- Cache port + key builder (domain layer, no Spring deps)
- Redis adapter (infra layer, Spring dependency injection)
- RetrieveConvertedUseCaseImpl refactored (cache → Treasury → cache save)
- All JPA code deleted (entity, adapter, port)
- Test coverage ≥90% (inherited from codebase policy)
- TDD approach (RED → GREEN → REFACTOR)

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- DB fallback on cache miss (Redis down → Treasury direct)
- Serialization logic in domain layer (infra responsibility)
- Date-based rate filtering (latest only, no temporal queries)
- Manual cache invalidation (TTL-based only, 6 months)
- V1/V2 migration modification (Flyway history immutable)
- TODO/FIXME comments (detekt forbids)
- Hardcoded TTL outside adapter (Duration.ofDays(180) encapsulated)

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- **Test decision**: TDD (RED → GREEN → REFACTOR) + framework: JUnit5 + Mockito + Testcontainers (Redis GenericContainer)
- **QA policy**: Every task has agent-executed scenarios (happy path + failure/edge case)
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}` for each scenario
- **Final gates**: 4 parallel review agents (F1-F4) all must APPROVE before handoff

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

**Wave 1: Foundation & Configuration**
- Task 1: Add Redis service to compose.yml (depends: nothing)
- Task 2: Create V3 migration (depends: nothing)

**Wave 2: Domain Layer**
- Task 3: Create ExchangeRateCachePort + ExchangeRateCacheKeyBuilder (depends: Task 1, 2 completed)

**Wave 3: Infra Layer**
- Task 4: Implement RedisExchangeRateCacheAdapter (depends: Task 3)

**Wave 4: Application & Cleanup**
- Task 5: Refactor RetrieveConvertedUseCaseImpl (depends: Task 4)
- Task 6: Delete JPA artifacts (depends: Task 5)
- Task 7: Unit + integration tests (depends: Task 5, 6)

**Wave 5: Final Verification (Parallel, all must PASS)**
- F1. Plan Compliance Audit (oracle)
- F2. Code Quality Review (unspecified-high)
- F3. Real Manual QA (unspecified-high + playwright for UI)
- F4. Scope Fidelity Check (deep)

### Dependency Matrix (full, all tasks)
| Task | Description | Depends On | Blocks | Wave |
|------|-------------|-----------|--------|------|
| 1 | Redis service compose.yml | — | 3, 4, 7 | W1 |
| 2 | V3 migration DROP TABLE | — | 3, 4, 7 | W1 |
| 3 | ExchangeRateCachePort + Builder | 1, 2 | 4, 5 | W2 |
| 4 | RedisExchangeRateCacheAdapter | 3 | 5, 7 | W3 |
| 5 | RetrieveConvertedUseCaseImpl refactor | 4 | 6, 7 | W4 |
| 6 | Delete JPA artifacts | 5 | 7 | W4 |
| 7 | Unit + integration tests | 5, 6 | F1-F4 | W4 |
| F1 | Plan Compliance (oracle) | 7 | — | W5 |
| F2 | Code Quality (unspecified-high) | 7 | — | W5 |
| F3 | Real Manual QA (unspecified-high) | 7 | — | W5 |
| F4 | Scope Fidelity (deep) | 7 | — | W5 |

### Agent Dispatch Summary
- **Wave 1**: 2 tasks, 1 agent (quick category) — both tasks independent
- **Wave 2**: 1 task, 1 agent (unspecified-high) — single domain interface
- **Wave 3**: 1 task, 1 agent (unspecified-high) — Spring + StringRedisTemplate + ObjectMapper
- **Wave 4**: 3 tasks (Tasks 5-7), 2 agents — Task 5 (unspecified-high), Tasks 6-7 combined (unspecified-high)
- **Wave 5**: 4 parallel agents (oracle, unspecified-high×2, deep) — all tasks must PASS

---

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Add Redis service to docker-compose.yml

  **What to do**:
  - Open `compose.yml` (lines 1-31 currently contain postgres + app)
  - Add Redis 8+ service after postgres block:
    ```yaml
    redis:
      image: redis:8-alpine
      ports:
        - "6379:6379"
      healthcheck:
        test: [ "CMD", "redis-cli", "ping" ]
        interval: 5s
        timeout: 3s
        retries: 5
      networks:
        - wex-network
    ```
  - Ensure app service includes `depends_on: [postgres, redis]`
  - Ensure all services use same network: `networks: [wex-network]`

  **Must NOT do**:
  - Create separate redis-compose.yml file
  - Add Redis to infra/docker (keep in root compose.yml only)
  - Modify postgres or app service (only add redis and update depends_on)
  - Remove existing services

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Single YAML file edit, straightforward service addition
  - Skills: [] - No special skills needed
  - Omitted: [clean-ddd-hexagonal, kotlin-specialist] - Not applicable to config

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [3, 4, 7] | Blocked By: []

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `compose.yml:lines 1-31` - Existing postgres + app structure
  - Config: Redis 8-alpine official image (`redis:8-alpine`)
  - Health: `redis-cli ping` standard health check (5s interval, 5 retries)
  - Network: Existing `wex-network` (app uses it; redis must join)

  **Acceptance Criteria** (agent-executable only):
  - [ ] `docker compose config | grep -A 5 "redis:"` returns redis block with image, ports, healthcheck
  - [ ] `docker compose up -d && docker compose ps` shows redis running (STATUS healthy)
  - [ ] `docker exec redis redis-cli ping` returns PONG
  - [ ] `docker compose logs redis | grep -i "ready"` confirms startup

  **QA Scenarios**:
  ```
  Scenario: Redis service definition correct
    Tool: PowerShell
    Steps:
      1. (Get-Content compose.yml) -match "redis:|redis:8-alpine" | Measure-Object | Select-Object -ExpandProperty Count
      2. (Get-Content compose.yml) -match "6379:6379|health" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: redis tag found (count >= 1); redis port + health check found (count >= 2)
    Evidence: .sisyphus/evidence/task-1-redis-config.log

  Scenario: Docker compose up succeeds
    Tool: PowerShell
    Steps:
      1. docker compose up -d
      2. Start-Sleep -Seconds 10
      3. docker compose ps
      4. docker compose logs redis | Select-String "Ready|listening" -Context 1
    Expected: All services running and healthy; redis logs show "Ready" or "listening"
    Evidence: .sisyphus/evidence/task-1-docker-up.log

  Scenario: Redis ping responds
    Tool: PowerShell
    Steps:
      1. docker exec redis redis-cli ping
    Expected: Output is "PONG"
    Evidence: .sisyphus/evidence/task-1-redis-ping.log

  Scenario: docker compose down cleans up
    Tool: PowerShell
    Steps:
      1. docker compose down -v
      2. docker compose ps
    Expected: No containers running; ps output empty or only stopped containers
    Evidence: .sisyphus/evidence/task-1-docker-down.log
  ```

  **Commit**: YES | Message: `config(docker): add Redis 8-alpine service with health check to compose.yml` | Files: [compose.yml]

---

- [ ] 2. Create V3 migration file to drop exchange_rates table

  **What to do**:
  - Create `src/main/resources/db/migration/V3__drop_exchange_rates_table.sql`
  - Content (forward-only, side-effect safe):
    ```sql
    -- Drop exchange_rates table (cached in Redis instead)
    DROP TABLE IF EXISTS exchange_rates CASCADE;
    ```
  - Verify Flyway will execute this after V1 and V2 (numeric ordering: V1, V2, V3)
  - No modifications to V1 or V2 (immutable)

  **Must NOT do**:
  - Modify V1__initial_schema.sql or V2__add_rate_source_to_exchange_rates.sql
  - Delete V1 or V2 files
  - Reference exchange_rates table in any other migration
  - Add complex logic (only DROP TABLE statement)

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Single SQL file creation, straightforward DROP TABLE
  - Skills: [] - No special skills needed
  - Omitted: [clean-ddd-hexagonal, kotlin-specialist] - Not applicable to migrations

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: [3, 4, 7] | Blocked By: []

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `src/main/resources/db/migration/V1__initial_schema.sql:` - Contains `CREATE TABLE exchange_rates`
  - Pattern: `src/main/resources/db/migration/V2__add_rate_source_to_exchange_rates.sql:` - Adds column to exchange_rates
  - Flyway docs: Migrations execute in numeric order (V1, V2, V3); immutable history preserved
  - Safety: `DROP TABLE IF EXISTS ... CASCADE` is idempotent (safe on re-run)

  **Acceptance Criteria** (agent-executable only):
  - [ ] `Test-Path src/main/resources/db/migration/V3__drop_exchange_rates_table.sql` returns True
  - [ ] `(Get-Content src/main/resources/db/migration/V3__drop_exchange_rates_table.sql) -match "DROP TABLE"` returns match
  - [ ] V1 and V2 files unchanged: `git diff src/main/resources/db/migration/V[12]__*` returns no changes
  - [ ] No other .sql files in migration dir reference exchange_rates after V3

  **QA Scenarios**:
  ```
  Scenario: V3 migration created correctly
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/resources/db/migration/V3__drop_exchange_rates_table.sql
      2. (Get-Content src/main/resources/db/migration/V3__drop_exchange_rates_table.sql) -match "DROP TABLE" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: V3 file exists with readable content; DROP TABLE count = 1
    Evidence: .sisyphus/evidence/task-2-v3-created.log

  Scenario: V1 and V2 unchanged (immutable)
    Tool: PowerShell
    Steps:
      1. $v1Hash = Get-FileHash src/main/resources/db/migration/V1__initial_schema.sql | Select-Object -ExpandProperty Hash
      2. $v2Hash = Get-FileHash src/main/resources/db/migration/V2__add_rate_source_to_exchange_rates.sql | Select-Object -ExpandProperty Hash
      3. rtk git diff src/main/resources/db/migration/V1__* src/main/resources/db/migration/V2__*
    Expected: Hashes consistent across multiple checks; git diff shows no changes to V1/V2
    Evidence: .sisyphus/evidence/task-2-v1v2-immutable.log

  Scenario: No exchange_rates code references remain
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src/main/kotlin -Recurse -Include "*.kt" | Select-String "exchange_rates|ExchangeRate" | Where-Object { $_.Path -notmatch ".git" }
    Expected: Zero matches (no table or schema refs in code)
    Evidence: .sisyphus/evidence/task-2-no-code-refs.log
  ```

  **Commit**: YES | Message: `db(migration): add V3 to drop exchange_rates table (forward-only, Flyway-safe)` | Files: [src/main/resources/db/migration/V3__drop_exchange_rates_table.sql]

---

- [ ] 3. Create ExchangeRateCachePort (outbound port) and ExchangeRateCacheKeyBuilder

  **What to do**:
  - Create `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt`
    - Interface with methods:
      - `fun getRate(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency): ExchangeRate?` — return null on miss
      - `fun saveRate(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency, rate: ExchangeRate): Unit` — write with 6-month TTL
  - Create `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt`
    - Utility: `fun buildCacheKey(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency): String`
    - Format: `exchangeRate:{sourceCurrencyCode}:{targetCurrencyCode}`
    - Example: `exchangeRate:USD:BRL`

  **Must NOT do**:
  - Add Java serialization or external libs to domain
  - Reference Spring/Redis in domain layer
  - Assume cache hit rate or TTL in domain

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Domain layer interfaces require careful type alignment (TargetCurrency, ExchangeRate)
  - Skills: [clean-ddd-hexagonal] - Critical: port naming, package structure, no infra deps in domain
  - Omitted: [kotlin-springboot] - Domain is framework-agnostic

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [4, 5] | Blocked By: [1, 2]

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateClientPort.kt` - Existing outbound port structure
  - Type: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/TargetCurrency.kt` - Currency enum used in signatures
  - Type: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt` - Rate value object
  - Example: `.specs/features/wex-tech-challenge/` - Documents cache key format: `exchangeRate:sourceCurrency:targetCurrency`

  **Acceptance Criteria** (agent-executable only):
  - [ ] `rtk gradle compileKotlin` succeeds (both files compile)
  - [ ] `grep "fun getRate\|fun saveRate" src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` finds both methods
  - [ ] `grep "TargetCurrency" src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` returns 4 matches (2 params per method × 2 methods)
  - [ ] `grep "exchangeRate:" src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt` confirms key format

  **QA Scenarios**:
  ```
  Scenario: Cache port interface compiles and has correct method signatures
    Tool: PowerShell
    Steps:
      1. rtk gradle compileKotlin
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt | Select-String "fun getRate|fun saveRate" -Context 2
    Expected: Compilation succeeds; both getRate and saveRate methods visible with TargetCurrency parameters
    Evidence: .sisyphus/evidence/task-3-cache-port-compile.log

  Scenario: Cache key builder produces correct format
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt | Select-String "exchangeRate:" -Context 1
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt | Select-String "buildCacheKey" -Context 3
    Expected: Key format matches "exchangeRate:{code}:{code}" pattern; buildCacheKey method present
    Evidence: .sisyphus/evidence/task-3-cache-key-format.log

  Scenario: Domain layer has no Spring dependencies
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt | Select-String "import.*spring|import.*redis"
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt | Select-String "import.*spring|import.*redis"
    Expected: Zero matches in both files (domain layer is framework-agnostic)
    Evidence: .sisyphus/evidence/task-3-no-spring-deps.log

  Scenario: TargetCurrency type used consistently
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt | Select-String "TargetCurrency" | Measure-Object).Count
    Expected: Exactly 4 occurrences (getRate: 2 params, saveRate: 2 params)
    Evidence: .sisyphus/evidence/task-3-target-currency-count.log
  ```

  **Commit**: YES | Message: `domain(port): add ExchangeRateCachePort + ExchangeRateCacheKeyBuilder outbound interfaces` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt, src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt]

---

- [ ] 4. Implement RedisExchangeRateCacheAdapter (infra layer)

  **What to do**:
  - Create `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt`
  - Implement ExchangeRateCachePort:
    - Constructor: `StringRedisTemplate`, `ObjectMapper` (Spring injection)
    - `getRate(sourceCurrency, targetCurrency)`:
      1. Use ExchangeRateCacheKeyBuilder to build key (e.g., `exchangeRate:USD:BRL`)
      2. `stringRedisTemplate.opsForValue().get(key)` to fetch
      3. If null: return null
      4. If present: deserialize JSON string to ExchangeRate via ObjectMapper
      5. **On deserialization error**: log error, return null (no rethrow)
    - `saveRate(sourceCurrency, targetCurrency, rate)`:
      1. Build key via ExchangeRateCacheKeyBuilder
      2. Serialize rate to JSON via ObjectMapper
      3. `stringRedisTemplate.opsForValue().set(key, json, Duration.ofDays(180))`
      4. **On serialization error**: log error, skip save (no rethrow)
  - Annotations: `@Component`, package: `com.charlesluxinger.wex_transactions.infra.adapter.cache`
  - Error handling: catch JsonMappingException + Exception, log, return null or skip (graceful degradation)

  **Must NOT do**:
  - Use BytesRedisTemplate (use StringRedisTemplate only)
  - Implement automatic cache invalidation
  - Add date-based rate filtering
  - Hardcode TTL outside this method (Duration.ofDays(180) only here)
  - Rethrow exceptions (null on error, let use case handle)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Infra adapter with Spring DI, StringRedisTemplate, serialization logic
  - Skills: [kotlin-springboot] - Critical: Spring Component annotation, StringRedisTemplate best practices
  - Omitted: [clean-ddd-hexagonal] - Architecture already defined; focus on implementation

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: [5, 7] | Blocked By: [3]

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt` - Data mapping pattern (now being deleted)
  - Pattern: betrader-backend codebase (researched earlier) - Redis adapter structure with StringRedisTemplate
  - API: `org.springframework.data.redis.core.StringRedisTemplate` - get/set operations
  - Type: `com.fasterxml.jackson.databind.ObjectMapper` - JSON serialization (auto-wired via Spring)
  - Duration: `java.time.Duration.ofDays(180)` - 6-month TTL

  **Acceptance Criteria** (agent-executable only):
  - [ ] `rtk gradle compileKotlin` succeeds
  - [ ] `grep "@Component" src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt` finds annotation
  - [ ] `grep "Duration.ofDays(180)" src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt` finds TTL
  - [ ] `grep -c "Duration.ofDays(180)" src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt` returns exactly 1

  **QA Scenarios**:
  ```
  Scenario: Redis adapter compiles and is Spring-injectable
    Tool: PowerShell
    Steps:
      1. rtk gradle compileKotlin
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt | Select-String "@Component|StringRedisTemplate|ObjectMapper" -Context 1
    Expected: Compilation succeeds; all three Spring/Redis annotations/types present
    Evidence: .sisyphus/evidence/task-4-adapter-compile.log

  Scenario: TTL is 6 months (180 days) and appears only once
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt | Select-String "Duration.ofDays\(180\)" | Measure-Object).Count
    Expected: Count = 1 (TTL hardcoded in saveRate method only)
    Evidence: .sisyphus/evidence/task-4-ttl-once.log

  Scenario: Error handling is present (no rethrow)
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt | Select-String "catch|try" -Context 2
    Expected: Try-catch blocks visible; no rethrow of exceptions visible
    Evidence: .sisyphus/evidence/task-4-error-handling.log

  Scenario: Cache key builder is used correctly
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt | Select-String "ExchangeRateCacheKeyBuilder|buildCacheKey" -Context 1
    Expected: Key builder injected and used in both getRate/saveRate
    Evidence: .sisyphus/evidence/task-4-key-builder-usage.log
  ```

  **Commit**: YES | Message: `infra(cache): implement RedisExchangeRateCacheAdapter with 6-month TTL and graceful error handling` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt]

---

- [ ] 5. Refactor RetrieveConvertedUseCaseImpl to use ExchangeRateCachePort

  **What to do**:
  - Modify `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`
    - Remove dependency: `exchangeRateRepositoryPort: ExchangeRateRepositoryPort`
    - Add dependency: `exchangeRateCachePort: ExchangeRateCachePort`
    - Update flow:
      1. Extract source and target currencies from Purchase: `sourceCurrency = purchase.transactionCurrency`, `targetCurrency = purchase.targetCurrency`
      2. Check cache: `exchangeRateCachePort.getRate(sourceCurrency, targetCurrency)`
      3. If cache miss (null): call `exchangeRateClientPort.fetchNearestPriorRate(...)`
      4. If fetch succeeds: save to cache: `exchangeRateCachePort.saveRate(sourceCurrency, targetCurrency, fetchedRate)`
      5. If fetch fails: throw `RateUnavailableException`
    - Remove `rateDate` parameter from cache lookup (latest rate only, no date filtering)
    - Simplify: no DB lookup window, no `findNearestPriorRate` call

  **Must NOT do**:
  - Keep `ExchangeRateRepositoryPort` reference
  - Call database in any path
  - Filter cache by date
  - Persist to database on Treasury fetch

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Use case refactoring; dependency replacement; flow rewrite
  - Skills: [clean-ddd-hexagonal] - Critical: use case orchestration, port composition, error handling
  - Omitted: [kotlin-springboot] - Not Spring-specific, domain/app layer logic

  **Parallelization**: Can Parallel: NO | Wave 4 | Blocks: [6, 7] | Blocked By: [4]

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt:lines 1-50` - Current flow with DB lookup
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` - New cache port (just created in Task 3)
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateClientPort.kt` - Existing Treasury API port (unchanged)
  - Exception: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/exception/RateUnavailableException.kt` - Thrown on Treasury fetch failure

  **Acceptance Criteria** (agent-executable only):
  - [ ] `rtk gradle compileKotlin` succeeds
  - [ ] `grep "exchangeRateCachePort" src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt` finds usage
  - [ ] `grep "exchangeRateRepositoryPort" src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt` returns zero matches
  - [ ] `grep "exchangeRateClientPort.fetchNearestPriorRate" src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt` finds call

  **QA Scenarios**:
  ```
  Scenario: Use case refactored, repo port removed
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt | Select-String "exchangeRateCachePort" | Measure-Object | Select-Object -ExpandProperty Count
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt | Select-String "exchangeRateRepositoryPort" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: Cache port usage found (count >= 2); repository port count = 0
    Evidence: .sisyphus/evidence/task-5-port-swap.log

  Scenario: Cache → Treasury → save flow implemented
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt | Select-String "getRate|fetchNearestPriorRate|saveRate" -Context 1
    Expected: All three methods visible in order (getRate, fetch on miss, saveRate)
    Evidence: .sisyphus/evidence/task-5-flow-order.log

  Scenario: No database lookups remain
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt | Select-String "findNearestPriorRate|findByRate|query|select|from" -Context 1
    Expected: Zero matches (no DB method calls)
    Evidence: .sisyphus/evidence/task-5-no-db-calls.log

  Scenario: Compilation succeeds
    Tool: PowerShell
    Steps:
      1. rtk gradle compileKotlin
    Expected: BUILD SUCCESS
    Evidence: .sisyphus/evidence/task-5-compile.log
  ```

  **Commit**: YES | Message: `app(service): refactor RetrieveConvertedUseCaseImpl to use Redis cache (cache-first, no DB)` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt]

---

- [ ] 6. Delete JPA exchange-rate artifacts

  **What to do**:
  - Delete `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt`
  - Delete `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt` (or `ExchangeRateJpaRepository.kt` if separate)
  - Delete `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt`
  - Verify: `grep -r "ExchangeRateJpaEntity\|ExchangeRateRepositoryPort" src/` returns zero matches

  **Must NOT do**:
  - Delete Purchase model or related artifacts
  - Delete ExchangeRate domain model
  - Delete test files yet (will clean in Task 7)
  - Modify any other adapters

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Straightforward file deletion (no code rewrites)
  - Skills: [] - No special skills needed
  - Omitted: [clean-ddd-hexagonal] - Architecture cleanup, not design

  **Parallelization**: Can Parallel: YES (with Task 7) | Wave 4 | Blocks: [7] | Blocked By: [5]

  **References** (executor has NO interview context - be exhaustive):
  - Files to delete: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt`, `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt`, `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt`
  - Verification: No references should remain in src/main/kotlin after deletion

  **Acceptance Criteria** (agent-executable only):
  - [ ] Three files deleted (Test-Path returns False for all)
  - [ ] `grep -r "ExchangeRateJpaEntity" src/main/kotlin` returns zero matches
  - [ ] `grep -r "ExchangeRateRepositoryPort" src/main/kotlin` returns zero matches
  - [ ] `rtk gradle compileKotlin` succeeds (no broken references)

  **QA Scenarios**:
  ```
  Scenario: JPA artifacts deleted
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt
      2. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt
      3. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt
    Expected: All three return False (files deleted)
    Evidence: .sisyphus/evidence/task-6-files-deleted.log

  Scenario: No broken references remain
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src/main/kotlin -Recurse -Include "*.kt" | Select-String "ExchangeRateJpaEntity|ExchangeRateRepositoryPort|ExchangeRateRepositoryAdapter" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: Count = 0
    Evidence: .sisyphus/evidence/task-6-no-references.log

  Scenario: Compilation succeeds
    Tool: PowerShell
    Steps:
      1. rtk gradle compileKotlin
    Expected: BUILD SUCCESS
    Evidence: .sisyphus/evidence/task-6-compile.log
  ```

  **Commit**: YES | Message: `infra(persistence): delete ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, and ExchangeRateRepositoryPort (replaced by cache)` | Files: [DELETE src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt, DELETE src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt, DELETE src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt]

---

- [ ] 7. Unit + integration tests (TDD: RED → GREEN → REFACTOR)

  **What to do**:
  - **Unit tests** (Mockito):
    - `ExchangeRateCacheKeyBuilderTest.kt`: Test key format for USD→BRL, USD→EUR, etc.
    - `RedisExchangeRateCacheAdapterTest.kt`: Mock StringRedisTemplate; test getRate (hit/miss), saveRate, error handling
  - **Integration tests** (Testcontainers):
    - `RedisExchangeRateCacheAdapterIntegrationTest.kt`: Real Redis container (GenericContainer); test end-to-end caching
    - `RetrieveConvertedUseCaseImplIntegrationTest.kt`: Mock Treasury API; test cache → fetch → save flow; verify no DB calls
  - **Test coverage** target: ≥90% (inherited from codebase policy)
  - **TDD approach**:
    1. RED: Write test (initially failing)
    2. GREEN: Implement code to pass test
    3. REFACTOR: Optimize, extract helpers, reduce duplication
  - **Cleanup**: Delete old tests for ExchangeRateRepositoryAdapter (no longer exists)

  **Must NOT do**:
  - Skip integration tests (Testcontainers mandatory)
  - Test database layer (no DB in scope)
  - Mock external Treasury API (test with actual endpoint behavior)
  - Leave TODO/FIXME comments (detekt forbids)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: TDD cycle, Testcontainers setup, coverage enforcement
  - Skills: [tdd] - Critical: RED-GREEN-REFACTOR discipline, test-first mindset
  - Omitted: [kotlin-springboot] - Tests are mostly domain/logic, not Spring config

  **Parallelization**: Can Parallel: YES (with Task 6) | Wave 4 | Blocks: [F1-F4] | Blocked By: [5, 6]

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/` - Existing use case tests
  - Framework: `org.junit.jupiter:junit-jupiter` - JUnit5 (declared in build.gradle.kts)
  - Mock: `org.mockito.kotlin:mockito-kotlin` - Mockito for StringRedisTemplate mocking
  - Container: `org.testcontainers:testcontainers` - Testcontainers library (check build.gradle.kts)
  - Coverage: JaCoCo configured in build.gradle.kts; overall threshold 90%

  **Acceptance Criteria** (agent-executable only):
  - [ ] `rtk gradle test` succeeds (all tests pass)
  - [ ] `rtk gradle test --tests "*RedisExchangeRateCache*"` passes (adapter tests)
  - [ ] `rtk gradle test --tests "*RetrieveConverted*"` passes (use case tests)
  - [ ] Coverage report generated: `build/reports/jacoco/test/html/index.html`
  - [ ] No TODO/FIXME comments in test files: `grep -r "TODO\|FIXME" src/test/kotlin/` returns zero matches

  **QA Scenarios**:
  ```
  Scenario: All tests pass
    Tool: PowerShell
    Steps:
      1. rtk gradle test
    Expected: BUILD SUCCESS; all tests PASSED; no failures
    Evidence: .sisyphus/evidence/task-7-all-tests-pass.log

  Scenario: Cache key builder tests pass
    Tool: PowerShell
    Steps:
      1. rtk gradle test --tests "*ExchangeRateCacheKeyBuilder*"
    Expected: BUILD SUCCESS; tests PASSED
    Evidence: .sisyphus/evidence/task-7-key-builder-tests.log

  Scenario: Redis cache adapter integration tests pass
    Tool: PowerShell
    Steps:
      1. rtk gradle test --tests "*RedisExchangeRateCache*Integration*"
    Expected: BUILD SUCCESS; tests PASSED; Testcontainers container created and cleaned up
    Evidence: .sisyphus/evidence/task-7-redis-integration-tests.log

  Scenario: Use case integration tests pass (cache → fetch → save)
    Tool: PowerShell
    Steps:
      1. rtk gradle test --tests "*RetrieveConverted*Integration*"
    Expected: BUILD SUCCESS; tests PASSED; cache hit, miss, and save flows verified
    Evidence: .sisyphus/evidence/task-7-use-case-integration-tests.log

  Scenario: No TODO/FIXME in tests
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src/test/kotlin -Recurse -Include "*.kt" | Select-String "TODO|FIXME" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: Count = 0
    Evidence: .sisyphus/evidence/task-7-no-todo-fixme.log

  Scenario: Coverage report generated and ≥90%
    Tool: PowerShell
    Steps:
      1. Test-Path build/reports/jacoco/test/html/index.html
      2. (Get-Content build/reports/jacoco/test/html/index.html) -match "90%|9[1-9]%|100%" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: Report file exists; overall coverage >= 90%
    Evidence: .sisyphus/evidence/task-7-coverage-report.log
  ```

  **Commit**: YES | Message: `test: add unit + integration tests for Redis cache (TDD: RED-GREEN-REFACTOR)` | Files: [src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapterTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapterIntegrationTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilderTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImplIntegrationTest.kt]

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback → fix → re-run → present again → wait for okay.

- [ ] F1. Plan Compliance Audit — oracle

  **What to do**: Review implementation against plan requirements. Verify all tasks completed per specification, no scope creep, all decisions honored.

  **References**:
  - Plan: `.sisyphus/plans/redis-exchange-rates.md` (this file)
  - Decisions: V1/V2 immutable, V3 forward-only, TargetCurrency throughout, no DB fallback, 6-month TTL, all JPA deleted

  **Acceptance Criteria** (agent-executable only):
  - [ ] All 7 tasks completed (T1-T7 marked done)
  - [ ] No JPA code remains: `grep -r "ExchangeRateJpaEntity\|ExchangeRateRepositoryPort" src/main/kotlin` returns zero
  - [ ] V1/V2 unchanged, V3 created: `ls src/main/resources/db/migration/V[123]__*` shows 3 files
  - [ ] All tests pass: `rtk gradle test` succeeds
  - [ ] Detekt + ktlint pass: `rtk gradle detekt ktlintMainSourceSetCheck` succeeds

  **QA Scenarios**:
  ```
  Scenario: No JPA exchange-rate code remains
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src/main/kotlin -Recurse -Include "*.kt" | Select-String "ExchangeRateJpaEntity|ExchangeRateJpaRepository|@Entity.*[Ee]xchange"
      2. Get-ChildItem -Path src/main/kotlin -Recurse -Include "*.kt" | Select-String "exchange_rates"
    Expected: Zero matches in both searches
    Evidence: .sisyphus/evidence/F1-jpa-removal.log

  Scenario: ExchangeRateCachePort correctly defined
    Tool: PowerShell
    Steps:
      1. rtk gradle compileKotlin
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt | Select-String "fun getRate|fun saveRate" -Context 2
    Expected: Both methods present with correct signatures (TargetCurrency params), compilation succeeds
    Evidence: .sisyphus/evidence/F1-cache-port-contract.log

  Scenario: All tests pass
    Tool: PowerShell
    Steps:
      1. rtk gradle test
    Expected: BUILD SUCCESS, all tests passed, test report generated
    Evidence: .sisyphus/evidence/F1-all-tests-pass.junit.xml
  ```

---

- [ ] F2. Code Quality Review — unspecified-high

  **What to do**: Static analysis, style compliance, error handling, security, maintainability. All code passes ktlint, detekt, no violations.

  **References**:
  - Style: `.editorconfig`, ktlint 4 spaces, max line 120
  - Quality: `config/detekt/detekt.yml`, no TODO/FIXME
  - Dependencies: Verify spring-data-redis in build.gradle.kts

  **Acceptance Criteria** (agent-executable only):
  - [ ] `rtk gradle detekt` returns zero violations
  - [ ] `rtk gradle ktlintMainSourceSetCheck ktlintTestSourceSetCheck` returns zero violations
  - [ ] `grep -r "TODO\|FIXME\|STOPSHIP" src/main/kotlin src/test/kotlin` returns zero matches

  **QA Scenarios**:
  ```
  Scenario: Detekt passes with no violations
    Tool: PowerShell
    Steps:
      1. rtk gradle detekt
    Expected: BUILD SUCCESS, no violations reported
    Evidence: .sisyphus/evidence/F2-detekt-pass.log

  Scenario: Ktlint passes on all source files
    Tool: PowerShell
    Steps:
      1. rtk gradle ktlintMainSourceSetCheck ktlintTestSourceSetCheck
    Expected: BUILD SUCCESS, all files formatted correctly
    Evidence: .sisyphus/evidence/F2-ktlint-pass.log

  Scenario: TTL hardcoded only in adapter
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src/main/kotlin -Recurse -Include "*.kt" | Select-String "Duration.ofDays\(180\)|15552000"
      2. Get-ChildItem -Path src/main/kotlin/com/charlesluxinger/wex_transactions -Recurse -Exclude { $_.FullName -match "infra/adapter/cache" } -Include "*.kt" | Select-String "Duration.ofDays\(180\)|15552000"
    Expected: First search matches in adapter; second search returns zero matches
    Evidence: .sisyphus/evidence/F2-ttl-encapsulation.log

  Scenario: Error handling in adapter (JSON deserialization)
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt | Select-String "catch.*JsonMappingException|catch.*Exception" -Context 3
    Expected: Error caught, logged, null returned (no rethrow visible in context)
    Evidence: .sisyphus/evidence/F2-error-handling.log
  ```

---

- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)

  **What to do**: End-to-end API testing. Create real purchase, retrieve with cache, verify Redis key present, repeat to confirm cache hit.

  **References**:
  - Endpoints: `POST /api/v1/purchases`, `GET /api/v1/purchases/{id}/converted`
  - Docker: `docker compose up -d` (Redis + app)
  - Redis CLI: `docker exec redis redis-cli GET "exchangeRate:USD:BRL"`

  **Acceptance Criteria** (agent-executable only):
  - [ ] App starts without errors: `docker compose up -d && docker compose logs app | grep -i "started\|ready"`
  - [ ] Redis responds: `docker exec redis redis-cli ping` returns PONG
  - [ ] API purchase create succeeds: `curl -X POST ... | grep -q '"id"'`
  - [ ] API retrieve converted succeeds: `curl -X GET ... | grep -q '"convertedAmount"'`
  - [ ] Cache key present after first call: `docker exec redis redis-cli GET "exchangeRate:USD:BRL"` not null

  **QA Scenarios**:
  ```
  Scenario: Docker compose up with Redis and app both healthy
    Tool: PowerShell
    Steps:
      1. docker compose up -d
      2. Start-Sleep -Seconds 15
      3. docker compose ps
      4. docker compose logs app | Select-Object -Last 20
    Expected: All services running and healthy; app logs show Redis connection established
    Evidence: .sisyphus/evidence/F3-docker-up.log

  Scenario: Redis container responds to ping
    Tool: PowerShell
    Steps:
      1. docker exec redis redis-cli ping
    Expected: Output is "PONG"
    Evidence: .sisyphus/evidence/F3-redis-ping.log

  Scenario: App connects to Redis on startup
    Tool: PowerShell
    Steps:
      1. docker compose logs app | Select-String "redis|connect|cache" -Context 1
    Expected: Logs mention Redis connection or cache initialization
    Evidence: .sisyphus/evidence/F3-app-redis-init.log

  Scenario: API create purchase, then retrieve with cache (uses real endpoints)
    Tool: PowerShell (using Invoke-RestMethod)
    Steps:
      1. Create a purchase:
         $body = @{
           description = "Test"
           transactionAmount = 100.00
           transactionCurrency = "USD"
           transactionDate = "2025-05-24T10:00:00Z"
           targetCurrency = "BRL"
         } | ConvertTo-Json
         $purchase = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/purchases" -Method Post -Body $body -ContentType "application/json"
         $purchaseId = $purchase.id
      2. Retrieve converted amount (first call, cache miss → Treasury API):
         $converted1 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/purchases/$purchaseId/converted" -Method Get
      3. Repeat step 2 two more times for cache hits:
         $converted2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/purchases/$purchaseId/converted" -Method Get
         $converted3 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/purchases/$purchaseId/converted" -Method Get
      4. Verify cache key in Redis:
         docker exec redis redis-cli GET "exchangeRate:USD:BRL"
    Expected: All requests HTTP 200; Redis key present after first call; second/third calls faster (cached)
    Evidence: .sisyphus/evidence/F3-api-cache-hits.log

  Scenario: Docker compose down cleans up without error
    Tool: PowerShell
    Steps:
      1. docker compose down -v
      2. docker compose ps
    Expected: All containers stopped and removed; no errors; ps output is empty
    Evidence: .sisyphus/evidence/F3-docker-down.log
  ```

---

- [ ] F4. Scope Fidelity Check — deep

  **What to do**: Verify all original requirements met. Cache key format correct, TTL is 6 months, no DB fallback, V1/V2 immutable, all JPA deleted, architecture hexagonal.

  **References**:
  - Scope: Original request + interview clarifications
  - Architecture: AGENTS.md hexagonal constraints
  - Files: Plan decisions (this file)

  **Acceptance Criteria** (agent-executable only):
  - [ ] All required files created: `test -f compose.yml docker/redis-* domain/port/outbound/ExchangeRateCachePort.kt ...`
  - [ ] All JPA files deleted: `grep -r "ExchangeRateJpaEntity" src/` returns zero
  - [ ] Cache key format verified: `grep "exchangeRate:" src/.../ExchangeRateCacheKeyBuilder.kt`
  - [ ] TTL is 180 days: `grep "Duration.ofDays(180)" src/.../RedisExchangeRateCacheAdapter.kt` finds exactly 1

  **QA Scenarios**:
  ```
  Scenario: All required files created
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt
      2. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt
      3. Test-Path compose.yml
      4. (Get-Content compose.yml) -match "redis:" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: All paths exist (True); Redis found in compose.yml (count >= 1)
    Evidence: .sisyphus/evidence/F4-files-present.log

  Scenario: All JPA entity/repository/migrations deleted
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src -Recurse -Include "*ExchangeRateJpaEntity*", "*ExchangeRateRepository*Adapter*" 2>/dev/null | Measure-Object | Select-Object -ExpandProperty Count
      2. Get-ChildItem -Path src/main/resources/db/migration -Filter "V[12]__*exchange*" 2>/dev/null | Measure-Object | Select-Object -ExpandProperty Count
    Expected: Both counts = 0 (no matching files)
    Evidence: .sisyphus/evidence/F4-files-deleted.log

  Scenario: Cache key format is exchangeRate:sourceCurrency:targetCurrency
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt | Select-String "buildCacheKey|exchangeRate:" -Context 1
      2. Get-ChildItem -Path src/test/kotlin -Recurse -Include "*ExchangeRateCacheKeyBuilder*" | Get-Content | Select-String "exchangeRate:" -Context 1
    Expected: Key format matches pattern "exchangeRate:{code}:{code}" (e.g., "exchangeRate:USD:BRL") in both implementation and tests
    Evidence: .sisyphus/evidence/F4-cache-key-format.log

  Scenario: TTL is 6 months (180 days)
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt | Select-String "Duration.ofDays\(180\)"
    Expected: "Duration.ofDays(180)" found exactly once in the saveRate method
    Evidence: .sisyphus/evidence/F4-ttl-6-months.log

  Scenario: No DB fallback on cache miss (use case flow)
    Tool: PowerShell
    Steps:
      1. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt | Select-String "fun retrieveConverted" -Context 10
      2. Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt | Select-String "exchangeRateRepositoryPort|ExchangeRateRepository" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: No repository port in use case; second search returns 0 matches
    Evidence: .sisyphus/evidence/F4-no-db-fallback.log

  Scenario: Redis-down fallback tested and passing
    Tool: PowerShell
    Steps:
      1. Get-ChildItem -Path src/test/kotlin -Recurse -Include "*.kt" | Select-String "redis.*down|Redis.*down" | Measure-Object | Select-Object -ExpandProperty Count
      2. rtk gradle test --tests "*Redis*Down*" -v 2>&1 | Select-String "PASSED|FAILED"
    Expected: Test file found (count >= 1); test runs and shows PASSED status
    Evidence: .sisyphus/evidence/F4-redis-down-test-pass.junit.xml
  ```

---

## Commit Strategy
- T1: `config(docker): add Redis 8-alpine service with health check to compose.yml`
- T2: `db(migration): add V3 to drop exchange_rates table (forward-only, Flyway-safe)`
- T3: `domain(port): add ExchangeRateCachePort + ExchangeRateCacheKeyBuilder outbound interfaces`
- T4: `infra(cache): implement RedisExchangeRateCacheAdapter with 6-month TTL and graceful error handling`
- T5: `app(service): refactor RetrieveConvertedUseCaseImpl to use Redis cache (cache-first, no DB)`
- T6: `infra(persistence): delete ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, and ExchangeRateRepositoryPort (replaced by cache)`
- T7: `test: add unit + integration tests for Redis cache (TDD: RED-GREEN-REFACTOR)`
- F1-F4: `docs(plan): final verification wave passed (oracle, quality, manual QA, scope)`

---

## Success Criteria

**Execution Complete when**:
1. ✅ All 7 tasks executed and committed
2. ✅ All F1-F4 final verification gates APPROVED
3. ✅ User explicitly approves verification results
4. ✅ Plan deleted (cleanup)
5. ✅ Evidence captured (.sisyphus/evidence/*)

**Definition of Done**:
- Zero JPA exchange-rate code remains
- Redis cache integrated (6-month TTL)
- Treasury API fallback on cache miss (no DB)
- V1/V2 immutable, V3 forward-only migration deployed
- All tests passing (≥90% coverage)
- All code formatted (ktlint), analyzed (detekt), quality checked
- API end-to-end tested (real purchase → retrieve → cache verified)
- Hexagonal architecture maintained (domain/app/infra separation)
