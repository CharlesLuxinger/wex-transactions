# Redis Exchange Rate Cache Migration

## TL;DR
> **Summary**: Migrate exchange-rate resolution from database-first to Redis cache (6-month TTL) + Treasury API fallback. Delete all JPA/DB persistence code. Edit V1 migration to remove exchange_rates table (API not in production). Mock Treasury API in tests.
> **Deliverables**: Redis service in compose.yml + spring-data-redis dependency + connection properties + documentation updates | V1 migration edited | ExchangeRateCachePort + ExchangeRateCacheKeyBuilder created | RedisExchangeRateCacheAdapter implemented | RetrieveConvertedUseCaseImpl refactored to cache-first flow | JPA artifacts deleted | comprehensive tests with mocked Treasury API | final verification (4 parallel agents)
> **Effort**: Large
> **Parallel**: YES - 2 waves (Wave 1: Tasks 1-2 foundation; Wave 2: Tasks 3-7 dependent on Wave 1 completion)
> **Critical Path**: Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Wave 5 (F1-F4 final verification)

## Context
### Original Request
User requested: Delete ExchangeRateJpaEntity and all related JPA code. Implement exchange-rate resolution via Redis cache (6-month TTL) + Treasury API fallback. Cache key: `exchangeRate:sourceCurrency:targetCurrency`. Edit V1 migration to remove exchange_rates table (API not in production). Mock Treasury API in integration tests. Update `.docs/run-and-test-guide.md` to reflect Redis addition.

### Interview Summary
- **Environment**: Windows (PowerShell for QA execution)
- **Cache strategy**: Redis-only (no DB fallback on cache miss); miss → Treasury API → save to cache
- **TTL**: 6 months (180 days)
- **Outage behavior**: Redis down → skip to Treasury API directly (no database fallback)
- **Currency types**: Request parameter `targetCurrency` (caller decides conversion target, not purchase-stored)
- **Migration approach**: Edit V1 directly to remove exchange_rates table (only file that exists; API not in production; no Flyway risk)
- **Testing strategy**: Mock/stub Treasury API (WireMock or Mockito) — NO external dependencies in integration tests
- **Architecture**: Hexagonal + DDD; adapters in `infra/`, outbound ports in `domain/port/outbound/`, use cases in `application/service/`
- **Task 1 consolidation**: Single task with 4 parts (A: compose.yml, B: build.gradle.kts, C: application.yaml, D: documentation)

### Metis Review (gaps addressed)
Metis identified: (1) DB-role contradiction (addressed: no DB fallback, Redis-only + Treasury), (2) rate freshness (addressed: 6-month TTL locked), (3) outage matrix (addressed: Redis down → Treasury directly). All addressed.

### Momus Review (Round 2 Final)
Momus confirmed: (1) V1 edit strategy valid (only file exists), (2) Treasury mock strategy locked (WireMock/Mockito, no external calls), (3) TargetCurrency from request (not purchase-stored). All blockers resolved. Plan ready for execution.

## Work Objectives
### Core Objective
Eliminate database persistence for exchange rates; replace with Redis cache as single source of truth; Treasury API as fallback on cache miss; delete all JPA code; architecture complies with hexagonal + DDD; V1 migration edited (not V3 created); documentation updated; all integration tests mock Treasury API; zero human intervention required for verification.

### Deliverables
1. Redis service (compose.yml) with health check
2. spring-boot-starter-data-redis dependency (build.gradle.kts)
3. Redis connection properties (application.yaml)
4. Documentation updates (.docs/run-and-test-guide.md)
5. V1 migration edited (exchange_rates table removed, purchases table intact)
6. ExchangeRateCachePort + ExchangeRateCacheKeyBuilder interfaces
7. RedisExchangeRateCacheAdapter implementation
8. RetrieveConvertedUseCaseImpl refactored (cache-first flow)
9. JPA artifacts deleted (entity, adapter, port)
10. Unit + integration tests (mocked Treasury API via WireMock/Mockito)
11. Final verification results (F1-F4 parallel agents)

### Definition of Done (verifiable conditions with commands)
```powershell
# Task 1 (config): Redis service + dependency + properties + docs
docker compose up -d
docker compose ps | grep redis  # Must be "healthy"
docker compose logs redis | Select-String "Ready to accept"  # Expected
./gradlew compileKotlin  # Must succeed
Get-Content src/main/resources/application.yaml | Select-String "spring.data.redis"  # Expected
Get-Content .docs/run-and-test-guide.md | Select-String "Redis"  # Expected (2+ mentions)

# Task 2 (migration): V1 edited
Get-Content src/main/resources/db/migration/V1__initial_schema.sql | Select-String "exchange_rates"  # Expected: 0 matches
Get-Content src/main/resources/db/migration/V1__initial_schema.sql | Select-String "purchases"  # Expected: ≥1 match
./gradlew compileKotlin  # Must succeed

# Task 3 (ports): Cache port created
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt  # Must be $true
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt  # Must be $true
./gradlew compileKotlin  # Must succeed

# Task 4 (adapter): Redis adapter created
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt  # Must be $true
./gradlew compileKotlin  # Must succeed
grep -r "Duration.ofDays(180)" src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/  # TTL enforcement

# Task 5 (use case): Refactored
./gradlew compileKotlin  # Must succeed
./gradlew test --tests "*RetrieveConverted*"  # All pass

# Task 6 (delete JPA): Artifacts removed
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt  # Must be $false
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt  # Must be $false
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt  # Must be $false
./gradlew compileKotlin  # Must succeed

# Task 7 (tests): Treasury mocked
./gradlew test  # All pass
grep -r "WireMock\|Mockito" src/test/kotlin/  # Treasury mocking confirmed

# Final Verification (F1-F4): All agents pass
# (See Final Verification Wave section)
```

### Must Have
- Redis 8-alpine service in compose.yml with health check
- spring-boot-starter-data-redis dependency in build.gradle.kts
- Redis connection properties (spring.data.redis.host: redis, spring.data.redis.port: 6379) in application.yaml
- Cache key format: `exchangeRate:sourceCurrency:targetCurrency` (enforced in ExchangeRateCacheKeyBuilder)
- TTL: Duration.ofDays(180) hardcoded in RedisExchangeRateCacheAdapter
- ExchangeRateCachePort outbound port in domain/port/outbound/
- ExchangeRateCacheKeyBuilder utility (domain/port/outbound/)
- RedisExchangeRateCacheAdapter in infra/adapter/cache/
- RetrieveConvertedUseCaseImpl refactored: cache-first (hit → return; miss → Treasury → save → return)
- Treasury API mocked in integration tests (WireMock or Mockito — NO external HTTP calls)
- V1 migration edited to remove exchange_rates table (purchases table unchanged)
- All JPA exchange-rate artifacts deleted (entity, adapter, port)
- Documentation updated (.docs/run-and-test-guide.md: Redis service mention, health check verification, env vars, failure playbook)
- Test coverage ≥90% (existing JaCoCo gate)
- Zero detekt/ktlint violations

### Must NOT Have
- Database persistence for exchange rates (DB fallback on cache miss)
- V3 migration file (only V1 edit, no new migration)
- Actual Treasury API calls in integration tests (mock/stub only)
- Purchase-stored targetCurrency (request parameter drives conversion)
- Adapter classes named `*PortImpl` (use `*Adapter` pattern per AGENTS.md)
- ExchangeRateRepositoryPort, ExchangeRateRepositoryAdapter, ExchangeRateJpaEntity (delete all 3)
- Manual verification steps in final gates (all F1-F4 agent-executed)
- Cache key variations (lock to `exchangeRate:sourceCurrency:targetCurrency`)
- TTL variations (lock to 6 months = 180 days)

### Guardrails (from Metis)
1. **Outage behavior**: If Redis is down, bypass to Treasury API directly — do NOT attempt database fallback
2. **Rate freshness**: 6-month cache TTL is acceptable; no date-based filtering; latest rate only
3. **Currency type consistency**: All cache port signatures must use `TargetCurrency` type (matches Purchase domain model)
4. **Test isolation**: Treasury API mocking is mandatory; no external service dependencies in CI/tests

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- **Test decision**: TDD (RED-GREEN-REFACTOR) with JUnit5 + Testcontainers (Redis) + WireMock/Mockito (Treasury)
- **QA policy**: Every task has agent-executed scenarios with specific PowerShell commands, expected outputs, evidence files
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}` (logs, screenshots, junit.xml)

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. Wave 1 (foundation tasks) serialized; Wave 2 (dependent tasks) parallel-safe after Wave 1 completion.

**Wave 1** (serialized: Task 1 → Task 2):
- Task 1: Configure Redis service + dependency + properties + docs (4 parts A/B/C/D)
- Task 2: Edit V1 migration (remove exchange_rates table)

**Wave 2** (parallel after Wave 1):
- Task 3: Create ExchangeRateCachePort + ExchangeRateCacheKeyBuilder (can start after Task 1)
- Task 4: Implement RedisExchangeRateCacheAdapter (can start after Task 1 + Task 3)
- Task 5: Refactor RetrieveConvertedUseCaseImpl (can start after Task 4)
- Task 6: Delete JPA artifacts (can start after Task 5)
- Task 7: Write unit + integration tests (can start after Task 4, runs after Task 6 completion)

**Wave 5** (final verification, parallel):
- F1: Plan Compliance Audit (oracle)
- F2: Code Quality Review (unspecified-high)
- F3: Real Manual QA (unspecified-high + playwright for API)
- F4: Scope Fidelity Check (deep)
- **CRITICAL**: Do NOT auto-proceed. Wait for user's explicit approval before marking work complete.

### Dependency Matrix (full, all tasks)
| Task | Depends On | Blocks |
|------|-----------|---------|
| Task 1 (config) | None | Task 2, Task 3, Task 4 |
| Task 2 (migration) | Task 1 | Task 7 |
| Task 3 (ports) | Task 1 | Task 4 |
| Task 4 (adapter) | Task 1, Task 3 | Task 5 |
| Task 5 (use case) | Task 4 | Task 6 |
| Task 6 (delete JPA) | Task 5 | Task 7 |
| Task 7 (tests) | Task 2, Task 4, Task 6 | F1-F4 |
| F1-F4 (verify) | Task 7 | (none — final gate) |

### Agent Dispatch Summary
| Wave | Tasks | Category | Parallelization |
|------|-------|----------|-----------------|
| Wave 1 | 1-2 | quick (config + schema edit) | Serial: 1 → 2 |
| Wave 2 | 3-7 | unspecified-high (domain + infra implementation + testing) | Parallel safe after Wave 1; Task 7 waits for Task 6 |
| Wave 5 | F1-F4 | oracle + unspecified-high + deep | All 4 in parallel; user approval before completion |

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Configure Redis service (compose.yml), add dependency (build.gradle.kts), set connection properties (application.yaml), update run-and-test-guide.md

  **What to do**:

  **Part A: compose.yml**
  - Open `compose.yml`
  - Add Redis 8-alpine service after postgres block:
    ```yaml
    redis:
      image: redis:8-alpine
      ports:
        - "6379:6379"
      healthcheck:
        test: ["CMD", "redis-cli", "ping"]
        interval: 5s
        timeout: 3s
        retries: 5
      networks:
        - wex-network
    ```
  - Ensure `networks:` section includes `wex-network: {}` (create if missing)

  **Part B: build.gradle.kts**
  - Open `build.gradle.kts`
  - Locate `dependencies { ... }` block
  - Add after spring-boot-starter-data-jpa:
    ```kotlin
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    ```

  **Part C: application.yaml**
  - Open `src/main/resources/application.yaml`
  - Locate `spring:` root block
  - Under `spring:`, add (at same indentation level as existing `datasource:`, `jpa:`, etc.):
    ```yaml
    spring:
      data:
        redis:
          host: redis
          port: 6379
    ```

  **Part D: Update .docs/run-and-test-guide.md**
  - Open `.docs/run-and-test-guide.md`
  - Line 31: Change "application and PostgreSQL services start." to "application, PostgreSQL, and Redis services start."
  - After "PostgreSQL logs section" (around line 50), add new verification step:
    ```
    3. Verify Redis readiness logs:

    docker compose logs redis

    Look for readiness markers indicating Redis is accepting connections.
    ```
  - After environment variables table, add:
    ```
    | `REDIS_HOST` | `redis` | Redis service hostname (Docker network name). |
    | `REDIS_PORT` | `6379` | Redis service port. |
    ```
  - Before "## 9) Reference", add new failure playbook subsection:
    ```
    ### Redis connection failure

    Symptoms: app cannot connect to Redis, cache operations fail.

    Actions:
    1. Verify Redis container running: `docker compose ps | grep redis`
    2. Check Redis logs: `docker compose logs redis`
    3. Verify port: `docker compose port redis 6379` returns 0.0.0.0:6379
    4. Test ping: `docker exec redis redis-cli ping` returns PONG
    5. Restart: `docker compose restart redis`
    ```

  **Must NOT do**:
  - Change API examples or other unrelated sections
  - Modify Docker build instructions
  - Remove existing PostgreSQL documentation
  - Add multiple root `spring:` blocks (consolidate under one)

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Configuration-only task; no logic, no tests; straightforward file edits
  - Skills: [] - Not needed
  - Omitted: [tdd, kotlin-specialist] - Configuration task, no code logic

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: Task 2, Task 3, Task 4 | Blocked By: (none)

  **References**:
  - Pattern: `docker-compose.yml` (betrader-backend style) - Redis 8-alpine with health check, standard ports, network config
  - Config: `build.gradle.kts` - Existing Spring Boot dependency structure
  - Config: `src/main/resources/application.yaml` - Existing Spring property structure
  - External: [Spring Data Redis Docs](https://spring.io/projects/spring-data-redis) - Property reference

  **Acceptance Criteria** (agent-executable only):
  - [ ] docker compose up -d succeeds; redis container healthy (docker compose ps | grep redis shows "healthy")
  - [ ] docker compose logs redis contains "Ready to accept connections"
  - [ ] ./gradlew compileKotlin succeeds
  - [ ] Gradle compileKotlin succeeds after adding spring-boot-starter-data-redis dependency
  - [ ] Redis service defined in docker-compose.yml with health check
  - [ ] Redis connection properties (host, port) in application.yaml (nested YAML structure is valid)
  - [ ] Documentation (.docs/run-and-test-guide.md) mentions Redis service, connection, and env vars

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Docker Compose with Redis service up and healthy
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. docker compose down -v  # Clean state
      3. Start-Sleep -Seconds 2
      4. docker compose up -d
      5. Start-Sleep -Seconds 5
      6. docker compose ps
    Expected: redis container exists with status "running" or "healthy"
    Evidence: .sisyphus/evidence/task-1-docker-healthy.log

  Scenario: Redis health check via redis-cli (using service name from compose.yml)
    Tool: PowerShell
    Steps:
      1. docker compose exec -it redis redis-cli ping
    Expected: Output is PONG
    Evidence: .sisyphus/evidence/task-1-redis-ping.log

  Scenario: Gradle compilation with Redis dependency
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL" (or "compileKotlin UP-TO-DATE")
    Evidence: .sisyphus/evidence/task-1-gradle-compile.log

  Scenario: Redis connection properties in application.yaml (nested YAML structure)
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/resources/application.yaml) -join "`n" | Select-String -Pattern "redis:\s*$" -A 2
    Expected: Found redis section with nested host/port properties
    Evidence: .sisyphus/evidence/task-1-yaml-properties.log

  Scenario: Documentation updated for Redis
    Tool: PowerShell
    Steps:
      1. (Get-Content .docs/run-and-test-guide.md) -match "Redis" | Measure-Object | Select-Object -ExpandProperty Count
      2. (Get-Content .docs/run-and-test-guide.md) -match "docker compose logs redis"
      3. (Get-Content .docs/run-and-test-guide.md) -match "REDIS_HOST|REDIS_PORT" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: First: 2+ matches; Second: matched; Third: 2+ matches
    Evidence: .sisyphus/evidence/task-1-docs-updated.log

  Scenario: Docker compose down cleanup
    Tool: PowerShell
    Steps:
      1. docker compose down -v
      2. Start-Sleep -Seconds 3
      3. docker compose ps
    Expected: No containers running (ps output empty or only stopped containers)
    Evidence: .sisyphus/evidence/task-1-docker-down.log
  ```

  **Commit**: YES | Message: `config(infra): add Redis 8-alpine service, spring-data-redis dependency, and connection properties; update run-and-test-guide.md` | Files: [compose.yml, build.gradle.kts, src/main/resources/application.yaml, .docs/run-and-test-guide.md]

---

- [ ] 2. Edit V1 migration to remove exchange_rates table

  **What to do**:
  - Open `src/main/resources/db/migration/V1__initial_schema.sql`
  - Locate and DELETE the entire `CREATE TABLE exchange_rates (...)` block (including semicolon)
  - KEEP the `CREATE TABLE purchases (...)` block intact
  - Verify no dangling commas or SQL syntax errors after deletion
  - Save file

  **Must NOT do**:
  - Delete purchases table
  - Modify V2 migration (leave unchanged; Flyway handles gracefully if table doesn't exist)
  - Create V3 migration (V1 is only edit target)
  - Leave dangling commas or incomplete SQL

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Single file edit; schema removal only; straightforward SQL surgery
  - Skills: [] - Not needed
  - Omitted: [tdd, kotlin-specialist] - Not coding; schema-only change

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: Task 7 | Blocked By: Task 1

  **References**:
  - File: `src/main/resources/db/migration/V1__initial_schema.sql` - Target file for edit

  **Acceptance Criteria** (agent-executable only):
  - [ ] Get-Content src/main/resources/db/migration/V1__initial_schema.sql | Select-String "exchange_rates" returns 0 matches
  - [ ] Get-Content src/main/resources/db/migration/V1__initial_schema.sql | Select-String "purchases" returns ≥1 match
  - [ ] ./gradlew compileKotlin succeeds (syntax check via Kotlin compiler)

  **QA Scenarios** (MANDATORY):
  ```
  Scenario: exchange_rates table removed from V1 migration
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/resources/db/migration/V1__initial_schema.sql) -match "exchange_rates" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: 0 (no mention of exchange_rates table)
    Evidence: .sisyphus/evidence/task-2-no-exchange-rates.log

  Scenario: purchases table remains intact in V1 migration
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/resources/db/migration/V1__initial_schema.sql) -match "purchases" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: ≥1 (purchases table definition present)
    Evidence: .sisyphus/evidence/task-2-purchases-intact.log

  Scenario: SQL syntax valid after edit (Flyway migration test)
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. Start PostgreSQL if needed (docker compose up -d postgres)
      3. ./gradlew test --tests "*FlywayMigration*" 2>&1 (or run specific Flyway validation test if available)
      4. If no specific test exists, verify app starts with edited V1: docker compose up app (should not fail on migration)
    Expected: Migration executes without SQL syntax errors; app startup succeeds
    Evidence: .sisyphus/evidence/task-2-sql-valid.log
  ```

  **Commit**: YES | Message: `db(migration): remove exchange_rates table from V1 (no longer persisted; cache + API only)` | Files: [src/main/resources/db/migration/V1__initial_schema.sql]

---

- [ ] 3. Create ExchangeRateCachePort and ExchangeRateCacheKeyBuilder

  **What to do**:
  - Create `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt`:
    ```kotlin
    package com.charlesluxinger.wex_transactions.domain.port.outbound

    import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
    import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency

    interface ExchangeRateCachePort {
        fun getRate(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency): ExchangeRate?
        fun saveRate(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency, rate: ExchangeRate)
    }
    ```
  - Create `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt`:
    ```kotlin
    package com.charlesluxinger.wex_transactions.domain.port.outbound

    import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency

    object ExchangeRateCacheKeyBuilder {
        fun buildCacheKey(sourceCurrency: TargetCurrency, targetCurrency: TargetCurrency): String =
            "exchangeRate:${sourceCurrency.code}:${targetCurrency.code}"
    }
    ```
  - Verify types match domain model (use TargetCurrency enum/class as defined in codebase)

  **Must NOT do**:
  - Use String types instead of TargetCurrency
  - Vary cache key format (lock to `exchangeRate:sourceCurrency:targetCurrency`)
  - Name adapter class `*PortImpl` (ports are interfaces, adapters are separate)
  - Add implementation logic (ports are contracts only)

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Interface definitions only; no logic; straightforward contract creation
  - Skills: [kotlin-specialist] - TargetCurrency type verification
  - Omitted: [tdd] - Interfaces alone don't require tests; tests come in Task 7

  **Parallelization**: Can Parallel: YES | Wave 2 (start after Task 1) | Blocks: Task 4 | Blocked By: Task 1

  **References**:
  - Type: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/TargetCurrency.kt` - Currency type definition
  - Type: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt` - Rate model
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/` - Outbound port location
  - AGENTS.md: "Inbound ports in domain/port/inbound/; outbound ports in domain/port/outbound/; never *PortImpl"

  **Acceptance Criteria** (agent-executable only):
  - [ ] Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt returns $true
  - [ ] Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt returns $true
  - [ ] ./gradlew compileKotlin succeeds
  - [ ] ExchangeRateCachePort.getRate signature: (TargetCurrency, TargetCurrency) → ExchangeRate?
  - [ ] ExchangeRateCachePort.saveRate signature: (TargetCurrency, TargetCurrency, ExchangeRate) → Unit
  - [ ] ExchangeRateCacheKeyBuilder.buildCacheKey returns format "exchangeRate:CODE1:CODE2"

  **QA Scenarios** (MANDATORY):
  ```
  Scenario: ExchangeRateCachePort interface created
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt) -match "interface ExchangeRateCachePort|fun getRate|fun saveRate"
    Expected: $true; 3 matches
    Evidence: .sisyphus/evidence/task-3-port-interface.log

  Scenario: ExchangeRateCacheKeyBuilder utility created
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt) -match "object ExchangeRateCacheKeyBuilder|fun buildCacheKey|exchangeRate:"
    Expected: $true; 3 matches
    Evidence: .sisyphus/evidence/task-3-key-builder.log

  Scenario: Kotlin compilation succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL"
    Evidence: .sisyphus/evidence/task-3-compile.log

  Scenario: Cache key format verification
    Tool: PowerShell (manual inspection)
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt) | Select-String 'exchangeRate:'
    Expected: Found; format matches "exchangeRate:${sourceCurrency.code}:${targetCurrency.code}"
    Evidence: .sisyphus/evidence/task-3-key-format.log
  ```

  **Commit**: YES | Message: `domain(port): add ExchangeRateCachePort and ExchangeRateCacheKeyBuilder for Redis cache interface` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt, src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt]

---

- [ ] 4. Implement RedisExchangeRateCacheAdapter

  **What to do**:
  - Create `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt`
  - Implement ExchangeRateCachePort using Spring's StringRedisTemplate
  - Constructor inject: StringRedisTemplate, ObjectMapper
  - Hardcode TTL: Duration.ofDays(180)
  - `getRate()`: redis GET with cache key; deserialize JSON to ExchangeRate; return null on miss/error
  - `saveRate()`: serialize ExchangeRate to JSON; redis SET with TTL; silently fail on error
  - Cache key format: ExchangeRateCacheKeyBuilder.buildCacheKey(sourceCurrency, targetCurrency)

  **Must NOT do**:
  - Use RedisTemplate<Object, Object> (use StringRedisTemplate for JSON serialization)
  - Vary TTL (lock to 180 days)
  - Throw exceptions on cache error (null/silent fail on miss or serialization error)
  - Expose cache key builder logic (delegate to ExchangeRateCacheKeyBuilder)
  - Name class `*PortImpl` (use `*Adapter` pattern per AGENTS.md)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Infrastructure adapter implementation; Spring integration; requires careful error handling
  - Skills: [kotlin-specialist] - Spring injection, StringRedisTemplate API
  - Omitted: [tdd] - Tests written in Task 7; adapter implementation first

  **Parallelization**: Can Parallel: YES | Wave 2 (start after Task 1 + Task 3) | Blocks: Task 5 | Blocked By: Task 1, Task 3

  **References**:
  - Interface: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` - Contract to implement
  - Utility: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt` - Cache key builder
  - Model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt` - Serialization target
  - Pattern: betrader-backend `infra/cache/` - StringRedisTemplate + ObjectMapper + Duration.ofDays() pattern
  - Spring API: [StringRedisTemplate](https://docs.spring.io/spring-data-redis/docs/current/api/org/springframework/data/redis/core/StringRedisTemplate.html) - Redis operations
  - External: [Spring Data Redis Docs](https://spring.io/projects/spring-data-redis) - Best practices

  **Acceptance Criteria** (agent-executable only):
  - [ ] Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt returns $true
  - [ ] ./gradlew compileKotlin succeeds
  - [ ] Adapter implements ExchangeRateCachePort (check signature match)
  - [ ] TTL hardcoded as Duration.ofDays(180) in adapter
  - [ ] No exceptions thrown on cache miss/error (null or silent fail)
  - [ ] StringRedisTemplate injected and used (not RedisTemplate<Object, Object>)

  **QA Scenarios** (MANDATORY):
  ```
  Scenario: RedisExchangeRateCacheAdapter class created
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt) -match "class RedisExchangeRateCacheAdapter|: ExchangeRateCachePort|StringRedisTemplate"
    Expected: $true; 3 matches
    Evidence: .sisyphus/evidence/task-4-adapter-class.log

  Scenario: TTL hardcoded as 180 days
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt) -match "Duration.ofDays\(180\)"
    Expected: ≥1 match
    Evidence: .sisyphus/evidence/task-4-ttl-180days.log

  Scenario: Kotlin compilation succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL"
    Evidence: .sisyphus/evidence/task-4-compile.log

  Scenario: getRate and saveRate methods present
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt) -match "fun getRate|fun saveRate|override"
    Expected: ≥2 matches (both methods, override keyword)
    Evidence: .sisyphus/evidence/task-4-methods.log

  Scenario: Error handling (null return on miss)
    Tool: PowerShell (code inspection)
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt) | Select-String "?: ExchangeRate\?|null|?.let|catch"
    Expected: Defensive null returns or exception catching visible
    Evidence: .sisyphus/evidence/task-4-error-handling.log
  ```

  **Commit**: YES | Message: `infra(cache): implement RedisExchangeRateCacheAdapter with 180-day TTL` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt]

---

- [ ] 5. Refactor RetrieveConvertedUseCaseImpl to use Redis cache

  **What to do**:
  - Open `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`
  - Inject ExchangeRateCachePort (constructor dependency)
  - Refactor execute() logic:
    1. Retrieve purchase (existing logic)
    2. Query cache: cachePort.getRate(sourceCurrency: purchase.transactionCurrency, targetCurrency: query.targetCurrency)
    3. If hit: return cached rate
    4. If miss: call Treasury API (existing logic) → save to cache: cachePort.saveRate(...) → return rate
  - Maintain same public contract (no API change)
  - Remove any calls to deleted ExchangeRateRepositoryPort (Task 6 cleanup)

  **Must NOT do**:
  - Change RetrieveConvertedQuery or RetrieveConvertedResponse signatures
  - Fallback to database on cache miss (skip directly to Treasury API)
  - Vary cache hit logic (fetch → return; no post-fetch validation)
  - Break existing API contract
  - Access ExchangeRateRepositoryPort (will be deleted in Task 6)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Application service refactor; cache integration; flow logic change
  - Skills: [kotlin-specialist] - Use case implementation patterns, dependency injection
  - Omitted: [tdd] - Tests written in Task 7; refactor first, then test

  **Parallelization**: Can Parallel: YES | Wave 2 (start after Task 4) | Blocks: Task 6 | Blocked By: Task 4

  **References**:
  - File: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt` - Target for refactor
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCachePort.kt` - Cache dependency
  - Model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/Purchase.kt` - transactionCurrency field
  - Model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/inbound/retrieveConverted/model/RetrieveConvertedQuery.kt` - targetCurrency field
  - AGENTS.md: "Use cases in application/service/**; orchestration only; no direct adapter access"

  **Acceptance Criteria** (agent-executable only):
  - [ ] ./gradlew compileKotlin succeeds
  - [ ] ExchangeRateCachePort injected in constructor
  - [ ] execute() logic: cache-first (hit → return; miss → Treasury → save → return)
  - [ ] No calls to ExchangeRateRepositoryPort (deleted in Task 6)
  - [ ] RetrieveConvertedQuery and RetrieveConvertedResponse signatures unchanged
  - [ ] ./gradlew test --tests "*RetrieveConverted*" succeeds (existing tests still pass)

  **QA Scenarios** (MANDATORY):
  ```
  Scenario: ExchangeRateCachePort injected
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt) -match "cachePort|ExchangeRateCachePort"
    Expected: ≥2 matches (import + injection)
    Evidence: .sisyphus/evidence/task-5-cache-injection.log

  Scenario: Cache-first logic in execute()
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt) | Select-String "cachePort.getRate|?.let|null"
    Expected: Matches found; cache hit/miss handling visible
    Evidence: .sisyphus/evidence/task-5-cache-logic.log

  Scenario: No calls to deleted ExchangeRateRepositoryPort
    Tool: PowerShell
    Steps:
      1. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt) -match "ExchangeRateRepositoryPort" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: 0 (no references to old repository port)
    Evidence: .sisyphus/evidence/task-5-no-old-repo.log

  Scenario: Kotlin compilation succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL"
    Evidence: .sisyphus/evidence/task-5-compile.log

  Scenario: Existing tests still pass
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. rtk gradle test --tests "*RetrieveConverted*" 2>&1
    Expected: All tests pass (0 failures)
    Evidence: .sisyphus/evidence/task-5-tests-pass.log
  ```

  **Commit**: YES | Message: `feat(app): refactor RetrieveConvertedUseCaseImpl to use Redis cache for exchange rates (cache-first, fallback to Treasury API)` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt]

---

- [ ] 6. Delete ExchangeRate JPA artifacts

  **What to do**:
  - Delete `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt`
  - Delete `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt`
  - Delete `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt`
  - Search for any remaining imports/references to these classes; remove or redirect to ExchangeRateCachePort
  - Verify no dangling dependencies on deleted classes

  **Must NOT do**:
  - Delete Purchase entity or other JPA classes
  - Delete database connection or JPA configuration
  - Delete purchase-related migrations (V1 purchases table stays)
  - Leave orphaned imports or references

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Cleanup task; file deletion only; no logic
  - Skills: [] - Not needed
  - Omitted: [tdd, kotlin-specialist] - Cleanup only, no code changes

  **Parallelization**: Can Parallel: NO | Wave 2 (must wait for Task 5) | Blocks: Task 7 | Blocked By: Task 5

  **References**:
  - File (delete): `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt`
  - File (delete): `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt`
  - File (delete): `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt`

  **Acceptance Criteria** (agent-executable only):
  - [ ] Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt returns $false
  - [ ] Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt returns $false
  - [ ] Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt returns $false
  - [ ] ./gradlew compileKotlin succeeds (no orphaned imports)
  - [ ] No remaining references to ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, or ExchangeRateRepositoryPort

  **QA Scenarios** (MANDATORY):
  ```
  Scenario: ExchangeRateJpaEntity deleted
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateJpaEntity.kt
    Expected: $false (file does not exist)
    Evidence: .sisyphus/evidence/task-6-entity-deleted.log

  Scenario: ExchangeRateRepositoryAdapter deleted
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/persistence/ExchangeRateRepositoryAdapter.kt
    Expected: $false (file does not exist)
    Evidence: .sisyphus/evidence/task-6-adapter-deleted.log

  Scenario: ExchangeRateRepositoryPort deleted
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateRepositoryPort.kt
    Expected: $false (file does not exist)
    Evidence: .sisyphus/evidence/task-6-port-deleted.log

  Scenario: No orphaned imports
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1 | Select-String "ExchangeRateJpaEntity|ExchangeRateRepositoryAdapter|ExchangeRateRepositoryPort"
    Expected: 0 matches (no unresolved references)
    Evidence: .sisyphus/evidence/task-6-compile.log

  Scenario: Verify no remaining references in codebase
    Tool: PowerShell
    Steps:
      1. grep -r "ExchangeRateJpaEntity|ExchangeRateRepositoryAdapter|ExchangeRateRepositoryPort" src/main/kotlin/ 2>$null | Measure-Object | Select-Object -ExpandProperty Count
    Expected: 0 (no matches)
    Evidence: .sisyphus/evidence/task-6-grep.log
  ```

  **Commit**: YES | Message: `refactor: delete ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, and ExchangeRateRepositoryPort (replaced by Redis cache)` | Files: (deletions only)

---

- [ ] 7. Write unit and integration tests with mocked Treasury API

  **What to do**:
  - Write unit tests for ExchangeRateCacheKeyBuilder:
    - Test cache key format: "exchangeRate:USD:BRL" (given USD and BRL currencies)
  - Write unit tests for RedisExchangeRateCacheAdapter:
    - Mock StringRedisTemplate
    - Test getRate() hit scenario: JSON deserialization to ExchangeRate
    - Test getRate() miss scenario: null return
    - Test saveRate(): SET operation with Duration.ofDays(180)
    - Test error handling: silent fail on serialization error
  - Write integration tests at HTTP/controller layer for `RetrieveConvertedControllerV1`:
    - Use existing integration-test pattern under `src/test/kotlin/.../infra/client/retrieveConverted/`
    - Keep Redis real via Testcontainers (when required by scenario)
    - Mock Treasury API via WireMock or Mockito (NO external HTTP calls)
    - Scenario 1: Cache hit → return cached rate (skip Treasury call)
    - Scenario 2: Cache miss → call Treasury (mocked) → save to cache → return rate
    - Scenario 3: Redis error → skip to Treasury (mocked) → return rate
    - Verify Treasury mock was called/not-called as expected
  - All tests must achieve ≥90% coverage (JaCoCo gate)

  **Must NOT do**:
  - Call actual Treasury API endpoint in tests (mock/stub only)
    - Treasury API mocking strategy: **WireMock** (HTTP-level mocking for integration tests) or **Mockito** (object-level mocking for unit tests)
  - Use real external services (Redis must be Testcontainers; Treasury must be mocked)
  - Skip TDD approach (RED-GREEN-REFACTOR for all test cases)
  - Reduce coverage below 90% (JaCoCo gate enforces 0.9 overall, 1.0 domain package)
  - Test business logic in cache adapter (adapter is infrastructure; test integration only)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Comprehensive test suite; integration testing; multiple test frameworks (Testcontainers, WireMock/Mockito, JUnit5)
  - Skills: [tdd, kotlin-specialist] - TDD approach; Kotlin test patterns; mocking
  - Omitted: [] - TDD mandatory; kotlin-specialist for framework knowledge

  **Parallelization**: Can Parallel: NO | Wave 2 (must wait for Task 6 before running tests) | Blocks: F1-F4 (final verification) | Blocked By: Task 6

  **References**:
  - Test framework: JUnit5 + Testcontainers (existing spring-boot-starter-test)
  - Mocking: [WireMock](https://wiremock.org/) (HTTP-level) or Mockito (object-level)
  - Integration test pattern: `src/test/kotlin/com/charlesluxinger/wex_transactions/` - Existing test structure
  - Policy: `.specs/codebase/TESTING.md` - Unit (Mockito OK), API (RestAssured), Integration (Testcontainers + real dependencies)
  - JaCoCo gate: `build.gradle.kts` - 0.9 overall, 1.0 domain package
  - External: [Testcontainers Docs](https://www.testcontainers.org/), [WireMock Docs](https://wiremock.org/docs/), [Mockito Docs](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

  **Acceptance Criteria** (agent-executable only):
  - [ ] All unit tests pass: ./gradlew test --tests "*ExchangeRateCache*" (key builder + adapter unit tests)
  - [ ] All integration tests pass: ./gradlew test --tests "*RetrieveConvertedControllerV1IntegrationTest"
  - [ ] Overall JaCoCo coverage ≥90%: ./gradlew jacocoTestReport (check build/reports/jacoco/test/html/index.html)
  - [ ] Domain package coverage = 100%: JaCoCo report shows domain/ ≥ 1.0
  - [ ] Treasury API mocked (no external HTTP calls): WireMock stubs or Mockito mock confirms
  - [ ] ./gradlew detekt succeeds (no violations)
  - [ ] ./gradlew ktlint succeeds (formatting)

  **QA Scenarios** (MANDATORY):
  ```
  Scenario: Unit tests for ExchangeRateCacheKeyBuilder
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test --tests "*CacheKeyBuilder*" 2>&1
    Expected: All tests pass (0 failures); output includes "ExchangeRateCacheKeyBuilder"
    Evidence: .sisyphus/evidence/task-7-unit-key-builder.log

  Scenario: Unit tests for RedisExchangeRateCacheAdapter
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test --tests "*CacheAdapter*" 2>&1
    Expected: All tests pass (0 failures); includes hit/miss/error scenarios
    Evidence: .sisyphus/evidence/task-7-unit-adapter.log

  Scenario: Integration tests for RetrieveConvertedControllerV1 with mocked Treasury
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test --tests "*RetrieveConvertedControllerV1IntegrationTest" 2>&1
    Expected: All tests pass (0 failures); Treasury mock was set up and used
    Evidence: .sisyphus/evidence/task-7-integration.log

  Scenario: Treasury API mocking confirmed (WireMock stubs)
    Tool: PowerShell
    Steps:
      1. (Get-Content src/test/kotlin/ -Recurse | Select-String "WireMock\|wiremock\|stubFor" | Measure-Object | Select-Object -ExpandProperty Count)
      2. OR (Get-Content src/test/kotlin/ -Recurse | Select-String "Mockito\|mock\|when.*thenReturn" | Measure-Object | Select-Object -ExpandProperty Count)
    Expected: ≥1 match (either WireMock or Mockito mocking confirmed)
    Evidence: .sisyphus/evidence/task-7-mocking.log

  Scenario: JaCoCo coverage ≥90% overall
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test jacocoTestReport 2>&1
      3. (Get-Content build/reports/jacoco/test/html/index.html) -match "0\.9[0-9]|1\.00" | Select-String "overall|total"
    Expected: Overall coverage ≥0.90; output visible in HTML report
    Evidence: .sisyphus/evidence/task-7-jacoco-coverage.log

  Scenario: Domain package coverage = 100%
    Tool: PowerShell
    Steps:
      1. (Get-Content build/reports/jacoco/test/html/com.charlesluxinger.wex_transactions.domain/index.html) -match "1\.00"
    Expected: domain/ package shows 1.00 (100%) coverage
    Evidence: .sisyphus/evidence/task-7-domain-coverage.log

  Scenario: All tests pass with clean build
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test 2>&1 | Select-String "passed|failed"
    Expected: "N passed, 0 failed" (all tests succeed)
    Evidence: .sisyphus/evidence/task-7-all-tests.log

  Scenario: Detekt and ktlint clean
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew detekt 2>&1 | Select-String "violations|errors"
      3. ./gradlew ktlintMainSourceSetCheck 2>&1
    Expected: No violations; ktlint passes
    Evidence: .sisyphus/evidence/task-7-linting.log
  ```

  **Commit**: YES | Message: `test: add unit and integration tests for Redis exchange-rate cache with mocked Treasury API` | Files: [src/test/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilderTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapterTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt]

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before marking work complete.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before completing.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback → fix → re-run → present again → wait for okay.

- [ ] F1. Plan Compliance Audit — oracle

  **What to check**:
  - All Tasks 1-7 completed as specified in plan
  - No scope creep (features added outside original request)
  - All deliverables present (files created, migrations edited, tests written)
  - Architecture compliance (hexagonal + DDD; no `*PortImpl` naming; adapters in infra/)
  - Guardrails honored (Redis-only cache, no DB fallback, 6-month TTL locked, Treasury mocked in tests)

  **Tool**: oracle agent

  **QA Scenarios**:
  ```
  Scenario: Verify all 7 tasks completed as documented
    Tool: oracle (read-only review)
    Steps:
      1. Verify .sisyphus/plans/redis-exchange-rates.md matches git log (7 commits with correct scopes)
      2. Verify Tasks 1-7 checklist items all marked ✓
      3. Cross-check against original user request (2026-05-24 conversation history)
    Expected: All 7 tasks match plan exactly; no ad-hoc commits; scope boundaries honored
    Evidence: .sisyphus/evidence/F1-plan-compliance.log

  Scenario: Verify no scope creep
    Tool: oracle (review git diff Tasks 1-7 commits)
    Steps:
      1. Review each task commit for unplanned changes (date filtering, purchase currency override, new APIs)
      2. Confirm only exchange_rates migration/cache code touched
    Expected: Zero out-of-scope modifications
    Evidence: .sisyphus/evidence/F1-no-creep.log

  Scenario: Verify architecture compliance
    Tool: oracle (code structure review)
    Steps:
      1. Confirm ExchangeRateCachePort in domain/port/outbound/
      2. Confirm RedisExchangeRateCacheAdapter in infra/adapter/cache/ (not named *PortImpl)
      3. Confirm RetrieveConvertedUseCaseImpl in application/service/retrieveConverted/
    Expected: All package placements match hexagonal + DDD rules
    Evidence: .sisyphus/evidence/F1-architecture.log

  Scenario: Verify guardrails honored
    Tool: oracle (source code review)
    Steps:
      1. Confirm no database fallback in cache-miss path (RetrieveConvertedUseCaseImpl→Treasury only)
      2. Confirm TTL set to Duration.ofDays(180) (not configurable)
      3. Confirm Treasury API mocked in all test files (no @SpringBootTest with real HTTP)
      4. Confirm cache key format locked: exchangeRate:{src}:{tgt}
    Expected: All 4 guardrails present, no exceptions
    Evidence: .sisyphus/evidence/F1-guardrails.log
  ```

- [ ] F2. Code Quality Review — unspecified-high

  **What to check**:
  - Zero detekt violations
  - Zero ktlint violations
  - Code follows project conventions (4-space indents, 120-char line limit, LF)
  - No AI slop patterns (verbose comments, redundant logic, unclear names)
  - Kotlin idioms respected (null safety, type inference, extension functions)
  - StringRedisTemplate usage correct (Spring Data Redis best practices)
  - TTL enforcement (Duration.ofDays(180) in adapter, no variations)
  - Cache key format consistent (`exchangeRate:sourceCurrency:targetCurrency`)
  - Error handling (null/silent fail on miss/error, no exceptions thrown)

  **Tool**: unspecified-high agent

  **QA Scenarios**:
  ```
  Scenario: Detekt passes with zero violations
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew detekt 2>&1 | Select-String "violations|errors"
    Expected: No violations found; exit code 0
    Evidence: .sisyphus/evidence/F2-detekt.log

  Scenario: ktlint passes with zero violations
    Tool: PowerShell
    Steps:
      1. ./gradlew ktlintMainSourceSetCheck 2>&1 | Select-String "error|Error"
    Expected: No formatting errors; exit code 0
    Evidence: .sisyphus/evidence/F2-ktlint.log

  Scenario: TTL is hardcoded to 180 days
    Tool: PowerShell (grep pattern)
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt" "Duration.ofDays\(180\)"
    Expected: Found 1 match (hardcoded TTL, not configurable)
    Evidence: .sisyphus/evidence/F2-ttl-hardcoded.log

  Scenario: Cache key format is locked
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt" 'exchangeRate:.*:'
    Expected: Found 1 match confirming format
    Evidence: .sisyphus/evidence/F2-cache-key-format.log

  Scenario: StringRedisTemplate usage is correct
    Tool: PowerShell (oracle review)
    Steps:
      1. Review RedisExchangeRateCacheAdapter source: ObjectMapper + StringRedisTemplate + opsForValue() pattern
    Expected: Follows Spring Data Redis best practices; no raw Redis client calls
    Evidence: .sisyphus/evidence/F2-redis-best-practices.log

  Scenario: Error handling is silent (null on miss/error)
    Tool: PowerShell (oracle review)
    Steps:
      1. Review ExchangeRateCacheAdapter.getRate() return path: catches exceptions, returns null
      2. Verify no throw statements in cache adapter
    Expected: All errors result in null return, not exceptions
    Evidence: .sisyphus/evidence/F2-error-handling.log
  ```

- [ ] F3. Real Manual QA — unspecified-high (+ playwright for API)

  **What to check**:
  - Docker compose up -d succeeds; redis healthy (docker compose ps | grep redis shows "healthy")
  - Redis health check passes (docker compose exec redis redis-cli ping returns PONG)
  - Application starts without errors (logs show Redis connection success, no ExchangeRate repository errors)
  - API endpoint working (GET /api/v1/purchases/{purchaseId}/converted?targetCurrency=... with mocked Treasury returns correct rate)
  - Cache hit scenario: Second call with same params returns cached rate (no new Treasury call)
  - Cache miss scenario: First call with new params hits Treasury mock, saves cache, returns rate
  - Redis failure scenario: Restart redis-cli; app continues to Treasury fallback (no database fallback)
  - Documentation updated correctly (.docs/run-and-test-guide.md mentions Redis, env vars, failure playbook)

  **Tool**: unspecified-high agent + playwright for API interactions

  **QA Scenarios**:
  ```
  Scenario: Docker compose up succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. docker compose up -d 2>&1
      3. Start-Sleep -Seconds 5
      4. docker compose ps
    Expected: redis and app services both show "Up" or "healthy"
    Evidence: .sisyphus/evidence/F3-docker-up.log

  Scenario: Redis health check passes
    Tool: PowerShell
    Steps:
      1. docker compose exec redis redis-cli ping
    Expected: Output is PONG
    Evidence: .sisyphus/evidence/F3-redis-health.log

  Scenario: Application logs show Redis connection success
    Tool: PowerShell
    Steps:
      1. docker compose logs app 2>&1 | Select-String "redis|Redis|connection.*success" -CaseSensitive
    Expected: At least 1 match showing successful Redis connection
    Evidence: .sisyphus/evidence/F3-app-redis-logs.log

  Scenario: API endpoint returns correct rate with mocked Treasury
    Tool: Playwright
    Steps:
      1. Start app if not running: docker compose up -d
      2. Ensure a purchase exists (create via POST /api/v1/purchases)
      3. GET http://localhost:8080/api/v1/purchases/{purchaseId}/converted?targetCurrency=BRL
      4. Verify response includes exchangeRateUsed and convertedAmount
    Expected: HTTP 200; response contains valid exchangeRateUsed (number > 0)
    Evidence: .sisyphus/evidence/F3-api-response.log

  Scenario: Cache hit (second call with same params is instant)
    Tool: Playwright
    Steps:
      1. Call API twice with same params
      2. Measure response time difference (second should be <100ms; first may be >500ms if Treasury mocked)
    Expected: Second response time < 100ms (cache hit)
    Evidence: .sisyphus/evidence/F3-cache-hit.log

  Scenario: Cache miss (first new param hits Treasury mock)
    Tool: Playwright
    Steps:
      1. Call API with new targetCurrency (e.g., EUR if previous was BRL)
      2. Verify response includes correct rate and is saved to Redis
      3. Call again with same currency; verify instant response
    Expected: First call slower (Treasury mock); second call fast (cached)
    Evidence: .sisyphus/evidence/F3-cache-miss.log

  Scenario: Redis restart doesn't break app (fallback to Treasury)
    Tool: PowerShell
    Steps:
      1. docker compose restart redis
      2. Wait 2 seconds
      3. Call API with new param (not cached after restart)
      4. Verify response is correct (Treasury fallback works)
    Expected: API still returns valid rate despite Redis restart
    Evidence: .sisyphus/evidence/F3-redis-restart.log

  Scenario: Documentation updated with Redis details
    Tool: PowerShell
    Steps:
      1. Select-String -Path ".docs/run-and-test-guide.md" "redis|Redis|REDIS_HOST|REDIS_PORT|cache"
    Expected: At least 3 matches confirming Redis, host/port env vars, and cache mention
    Evidence: .sisyphus/evidence/F3-docs-updated.log
  ```

- [ ] F4. Scope Fidelity Check — deep

  **What to check**:
  - All JPA exchange-rate artifacts deleted (ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, ExchangeRateRepositoryPort gone)
  - V1 migration edited (exchange_rates table removed, purchases table intact)
  - No database fallback on cache miss (code paths verified: cache miss → Treasury only)
  - Request parameter targetCurrency respected (not purchase-stored; API contract unchanged)
  - Cache key format locked (no variations)
  - TTL locked to 6 months (no config variations)
  - Treasury API mocked (WireMock or Mockito; no real HTTP calls in tests)
  - Architecture maintained (hexagonal + DDD; no repository pattern misuse)
  - Test coverage ≥90% JaCoCo gate (overall 0.9, domain 1.0)
  - All QA scenarios from Tasks 1-7 executed and passed
  - No scope creep (e.g., no date-based filtering, no purchase-based currency override, no new APIs)

  **Tool**: deep agent

  **QA Scenarios** (part 1 - JPA/migration/DB fallback):
  ```
  Scenario: All JPA artifacts deleted
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin" -Recurse "ExchangeRateJpaEntity|ExchangeRateRepositoryAdapter|ExchangeRateRepositoryPort" -ErrorAction SilentlyContinue
    Expected: 0 results (artifacts completely deleted)
    Evidence: .sisyphus/evidence/F4-jpa-deleted.log

  Scenario: V1 migration contains no exchange_rates table
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/resources/db/migration/V1__initial_schema.sql" "CREATE TABLE exchange_rates"
    Expected: 0 results (table creation removed)
    Evidence: .sisyphus/evidence/F4-v1-edited.log

  Scenario: V1 migration still contains purchases table
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/resources/db/migration/V1__initial_schema.sql" "CREATE TABLE purchases"
    Expected: 1 match (purchases table intact)
    Evidence: .sisyphus/evidence/F4-purchases-intact.log


  Scenario: No database fallback in cache miss path
    Tool: PowerShell (oracle review)
    Steps:
      1. Review RetrieveConvertedUseCaseImpl.execute() or retrieveConverted(query) method
      2. Verify flow: cache.getRate() null → treasuryClient.getRate() → cache.saveRate() → return
      3. Confirm NO database/repository access in this path
    Expected: Cache miss always goes to Treasury API only; zero repo access
    Evidence: .sisyphus/evidence/F4-no-db-fallback.log
  ```

  **QA Scenarios** (part 2 - request params/cache/TTL/mocking):
  ```
  Scenario: Request parameter targetCurrency respected
    Tool: PowerShell (oracle review)
    Steps:
      1. Review API controller RetrieveConvertedControllerV1
      2. Verify RetrieveConvertedQuery is built from request params: targetCurrency from request, NOT from purchase.currency
      3. Confirm conversion happens with request-supplied targetCurrency
    Expected: Request param drives conversion; purchase currency not used for override
    Evidence: .sisyphus/evidence/F4-request-param-driven.log

  Scenario: Cache key format is locked (no variations)
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateCacheKeyBuilder.kt" 'fun buildCacheKey' -A 3
    Expected: Only 1 buildCacheKey method; always returns "exchangeRate:{src}:{tgt}" format
    Evidence: .sisyphus/evidence/F4-cache-key-locked.log

  Scenario: TTL locked to Duration.ofDays(180)
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt" "Duration"
    Expected: 1 match: Duration.ofDays(180); no other TTL configs or externalized properties
    Evidence: .sisyphus/evidence/F4-ttl-locked.log

  Scenario: Treasury API mocked in tests (no real HTTP)
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/test/kotlin" -Recurse "@WireMockTest|Mockito|mock.*TreasuryClient|WireMock.stubFor" -ErrorAction SilentlyContinue
    Expected: At least 1 match confirming mocking framework used
    Evidence: .sisyphus/evidence/F4-treasury-mocked.log
  ```

  **QA Scenarios** (part 3 - architecture/coverage/scope):
  ```
  Scenario: Architecture compliance (no repository in RetrieveConverted path)
    Tool: PowerShell (oracle review)
    Steps:
      1. Review RetrieveConvertedUseCaseImpl: should only call cache port + external client
      2. Confirm zero import of Repository/Adapter classes
      3. Verify use case in application/service/retrieveConverted/ (not domain/)
    Expected: Clean architecture (use case → inbound port → outbound ports only)
    Evidence: .sisyphus/evidence/F4-architecture-clean.log

  Scenario: Overall JaCoCo coverage ≥90%
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test jacocoTestReport 2>&1
      3. (Get-Content build/reports/jacoco/test/html/index.html) -match "0\.9[0-9]|1\.00"
    Expected: Overall coverage >= 0.90 (90%)
    Evidence: .sisyphus/evidence/F4-coverage-overall.log

  Scenario: Domain package coverage = 100%
    Tool: PowerShell
    Steps:
      1. (Get-Content build/reports/jacoco/test/html/com.charlesluxinger.wex_transactions.domain/index.html) -match "1\.00"
    Expected: Found 1 match (domain/ coverage is 100%)
    Evidence: .sisyphus/evidence/F4-coverage-domain.log

  Scenario: No scope creep (no date-based filtering, no currency override, no new APIs)
    Tool: PowerShell (oracle review)
    Steps:
      1. Search for "date|Date|currency.*override|override.*currency|rateAsOf|getRateAsOf" in new code
      2. Verify no new API endpoints added (existing GET /api/v1/purchases/{purchaseId}/converted preserved)
      3. Confirm cache key does NOT include date parameter
    Expected: 0 matches for date-filtering or purchase-currency-override; API contract unchanged
    Evidence: .sisyphus/evidence/F4-no-creep.log
  ```

---

## Commit Strategy
- **Atomic commits per task** (no multi-task commits)
- **Conventional commit messages**: `type(scope): desc`
  - Task 1: `config(infra): add Redis 8-alpine service, spring-data-redis dependency, and connection properties; update run-and-test-guide.md`
  - Task 2: `db(migration): remove exchange_rates table from V1 (no longer persisted; cache + API only)`
  - Task 3: `domain(port): add ExchangeRateCachePort and ExchangeRateCacheKeyBuilder for Redis cache interface`
  - Task 4: `infra(cache): implement RedisExchangeRateCacheAdapter with 180-day TTL`
  - Task 5: `feat(app): refactor RetrieveConvertedUseCaseImpl to use Redis cache for exchange rates (cache-first, fallback to Treasury API)`
  - Task 6: `refactor: delete ExchangeRateJpaEntity, ExchangeRateRepositoryAdapter, and ExchangeRateRepositoryPort (replaced by Redis cache)`
  - Task 7: `test: add unit and integration tests for Redis exchange-rate cache with mocked Treasury API`
- **Signing**: All commits must be signed (git config user.signingKey)

## Success Criteria
1. **Plan compliance**: All 7 tasks executed as specified; no scope creep
2. **Architecture compliance**: Hexagonal + DDD patterns maintained; all adapters correctly placed
3. **Code quality**: Zero detekt/ktlint violations; all tests pass; coverage ≥90%
4. **Guardrails honored**: Redis-only, no DB fallback, 6-month TTL locked, Treasury mocked, cache key format locked, request-driven targetCurrency respected
5. **Documentation**: `.docs/run-and-test-guide.md` updated; Redis addition reflected
6. **Verification**: All 4 parallel agents (F1-F4) APPROVE; user gives explicit okay before completion
7. **Atomic commits**: 7 commits (one per task); all signed; conventional messages
8. **Zero manual intervention**: All QA scenarios executed by agents; no human verification required in final gates
