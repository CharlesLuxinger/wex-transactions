# Refactor RetrieveConvertedUseCaseImpl — Redis Stream Event Publishing

## TL;DR
> **Summary**: After `exchangeRateClientPort.fetchNearestPriorRate()` returns, publish a domain event to Redis Stream `exchange-rate-fetched-events`. Remove the inline synchronous `exchangeRateCachePort.saveRate()` call. Async consumer reads the stream and saves the rate to cache. Publish failure must NOT block the flow.
> **Deliverables**: `ExchangeRateFetchedEvent` (domain event), `ExchangeRateEventPort` (domain port), `RedisExchangeRateEventAdapter` (publisher), `ExchangeRateFetchedEventListener` (consumer), `ExchangeRateEventsStreamProperties` + `ExchangeRateEventsStreamConfig` (stream config), refactored `RetrieveConvertedUseCaseImpl` (remove sync save + inject event port), unit tests for use case / adapter / listener
> **Effort**: Medium
> **Parallel**: YES - 3 waves (Wave 1: domain artifacts; Wave 2: infra + refactor in parallel; Wave 3: tests)
> **Critical Path**: Task 1 (domain) → [Task 2 (infra) + Task 3 (refactor) parallel] → Task 4 (tests) → F1-F4

## Context
### Original Request
User requested: Refactor `RetrieveConvertedUseCaseImpl` to publish a Redis Stream event after `exchangeRateClientPort` is called. The event is processed asynchronously to save the exchange rate in the cache. If the publish fails, the flow must not be blocked. The synchronous `exchangeRateCachePort.saveRate()` call in `fetchClient()` should be removed — only the async consumer saves to cache.

Stream key: `exchange-rate-fetched-events`. Consumer group: `exchange-rate-fetched-group`. Event payload: full ExchangeRate (sourceCurrency, targetCurrency, rate, retrievedAt, rateDate).

### Interview Summary
- **Scope confirmed**: Event publishing ONLY. No coroutine parallelization (explicitly asked and confirmed with user). Any previous notes about coroutine racing between cache and client are explicitly out of scope for this plan.
- **Cache save strategy**: Remove sync `saveRate()` from use case. Async consumer via Redis Stream is the ONLY path that saves the fetched rate to cache.
- **Event model**: `ExchangeRateFetchedEvent` with sourceCurrency, targetCurrency, rate, retrievedAt, rateDate
- **Pattern**: Follow betrader-backend Redis Stream (CreateTradeEventPublisher + CreateTradeEventListener + RedisTradeEventsConfig)
- **Resilience**: Publish failure → `runCatching` + warn log, does NOT throw. Listener acks only after successful cache save (unacknowledged = re-delivery).
- **Startup**: Best-effort — if Redis stream/group creation fails, log warn, app starts.
- **Recovery scheduler**: Excluded from MVP.

### Metis Review (gaps addressed)
Metis identified: (1) start-up policy (addressed: best-effort), (2) recovery scheduler (addressed: deferred from MVP), (3) ack policy (addressed: ack after successful cache save), (4) duplicate delivery (addressed: cache overwrite is safe idempotency), (5) logging requirement (addressed: structured warn on publish failure), (6) `rateDate` source (addressed: `purchase.transactionDate.value.toLocalDate()` — same date passed to `fetchNearestPriorRate`). All addressed.

## Work Objectives
### Core Objective
Replace the synchronous cache write in `RetrieveConvertedUseCaseImpl.fetchClient()` with an async event-driven flow: publish event to Redis Stream → async consumer reads stream → consumer saves rate to cache via `exchangeRateCachePort`. Publish failure must not propagate. Architecture follows hexagonal DDD.

### Deliverables
1. `ExchangeRateFetchedEvent` — domain event model (sourceCurrency, targetCurrency, rate, retrievedAt, rateDate)
2. `ExchangeRateEventPort` — outbound domain port interface (`fun publish(event: ExchangeRateFetchedEvent)`)
3. `RedisExchangeRateEventAdapter` — infra adapter implementing port, publishes JSON to Redis Stream via `StringRedisTemplate.opsForStream().add()` with `runCatching` + warn log
4. `ExchangeRateFetchedEventListener` — infra listener implementing `StreamListener<String, MapRecord>`, deserializes event, calls `exchangeRateCachePort.saveRate()`, acks only on success
5. `ExchangeRateEventsStreamConfig` — infra config: creates consumer group (best-effort), sets up `StreamMessageListenerContainer`
6. Refactored `RetrieveConvertedUseCaseImpl` — inject `ExchangeRateEventPort`, call `publish()` after `exchangeRateClientPort`, remove `exchangeRateCachePort.saveRate()`
7. Unit tests for use case, adapter, listener (no integration tests — all scenarios covered by unit mocks)

### Definition of Done (verifiable conditions with commands)
```powershell
# Domain artifacts exist
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt  # Must be $true
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt  # Must be $true
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt  # Must be $true
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt  # Must be $true
Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamConfig.kt  # Must be $true

# Compilation
./gradlew compileKotlin  # Must succeed

# Use case no longer calls saveRate
Select-String -Path src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt "saveRate"  # Expected: 0 matches

# Use case now calls publish
Select-String -Path src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt "eventPort"  # Expected: ≥2 matches

# All tests pass
./gradlew test  # All pass

# Quality gates
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck  # Must pass
./gradlew --no-daemon detekt  # Must pass
```

### Must Have
- `ExchangeRateFetchedEvent` with fields: sourceCurrency (String), targetCurrency (String), rate (BigDecimal), retrievedAt (Instant), rateDate (LocalDate)
- `ExchangeRateEventPort` with single method: `fun publish(event: ExchangeRateFetchedEvent)`
- `RedisExchangeRateEventAdapter` uses `StringRedisTemplate.opsForStream().add()` with `runCatching` + warn log on failure
- `ExchangeRateFetchedEventListener` implements `StreamListener<String, MapRecord<String, String, String>>`, calls `exchangeRateCachePort.saveRate()`, acks after success
- `ExchangeRateEventsStreamConfig` creates consumer group `exchange-rate-fetched-group` for stream `exchange-rate-fetched-events`, best-effort on error
- `RetrieveConvertedUseCaseImpl` refactored: `fetchClient()` publishes event after client call, NO inline `saveRate()`, `runCatching` for publish failure
- Stream field structure: `mapOf("payload" to objectMapper.writeValueAsString(event))`
- Consumer name: `exchange-rate-fetched-consumer-{UUID}`
- Tests: use-case unit (Mockito), adapter unit (Mockito + StringRedisTemplate mock), listener unit (Mockito + simulated stream record). **No integration tests** (no Testcontainers Redis dependency in build.gradle.kts — only unit tests with mocks)
- Existing test `cache miss fetches treasury and stores in cache` updated: remove `saveRate` assertion, add `publish` assertion + `saveRate` never assertion

### Must NOT Have
- Coroutine parallelization (race between cache and client)
- Inline `exchangeRateCachePort.saveRate()` in `RetrieveConvertedUseCaseImpl`
- Recovery scheduler / pending-message claim loop (deferred)
- Fail-fast startup on Redis unavailability
- Changes to `ExchangeRateCachePort`, `ExchangeRateClientPort`, or controller
- Changes to cache key format, TTL, or adapter
- Generic event framework or multi-stream abstractions
- Manual verification steps in final gates

### Guardrails (from Metis)
1. **Publish failure isolation**: `runCatching` in adapter + warn log. Exception NEVER propagates to caller.
2. **Duplicate delivery safety**: Cache overwrite is naturally idempotent (same source+target+rate overwrites same key).
3. **Listener ack discipline**: Only acknowledge after `cachePort.saveRate()` succeeds. On failure, stream re-delivers.
4. **Startup resilience**: Group creation failure → log warn, container may not start but app boots.
5. **rateDate source**: Must use `purchase.transactionDate.value.toLocalDate()` (the same date passed to `fetchNearestPriorRate`).
6. **Consumer instance naming**: Append UUID to consumer name to support multiple instances.

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- **Test decision**: Tests-after with JUnit5 + Mockito (unit only — no Testcontainers Redis dependency in build.gradle.kts)
- **QA policy**: Every task has agent-executed scenarios with specific PowerShell commands, expected outputs, evidence files
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves

**Wave 1** (serial — foundation):
- Task 1: Create domain event model + outbound port (blocks everything)

**Wave 2** (parallel — both depend on Task 1 only):
- Task 2: Create infra publisher adapter + listener + config
- Task 3: Refactor RetrieveConvertedUseCaseImpl

**Wave 3** (serial — depends on Task 2 + Task 3):
- Task 4: Write unit tests (use case, adapter, listener)

**Wave 5** (final verification, parallel):
- F1-F4: 4 review agents

### Dependency Matrix
| Task | Depends On | Blocks |
|------|-----------|--------|
| Task 1 (domain) | None | Task 2, Task 3 |
| Task 2 (infra) | Task 1 | Task 4 |
| Task 3 (refactor) | Task 1 | Task 4 |
| Task 4 (tests) | Task 2, Task 3 | F1-F4 |
| F1-F4 (verify) | Task 4 | (none) |

### Agent Dispatch Summary
| Wave | Tasks | Category | Parallelization |
|------|-------|----------|-----------------|
| Wave 1 | 1 | quick | Serial (foundation) |
| Wave 2 | 2, 3 | unspecified-high | Parallel (both depend on Task 1 only) |
| Wave 3 | 4 | unspecified-high | Serial (depends on Task 2 + Task 3) |
| Wave 5 | F1-F4 | oracle + unspecified-high + deep | All 4 parallel |

## TODOs

- [ ] 1. Create ExchangeRateFetchedEvent domain model and ExchangeRateEventPort outbound port

  **What to do**:

  **File: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt`**
  ```kotlin
  package com.charlesluxinger.wex_transactions.domain.event

  import java.math.BigDecimal
  import java.time.Instant
  import java.time.LocalDate

  data class ExchangeRateFetchedEvent(
      val sourceCurrency: String,
      val targetCurrency: String,
      val rate: BigDecimal,
      val retrievedAt: Instant,
      val rateDate: LocalDate,
  )
  ```

  **File: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt`**
  ```kotlin
  package com.charlesluxinger.wex_transactions.domain.port.outbound

  import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent

  fun interface ExchangeRateEventPort {
      fun publish(event: ExchangeRateFetchedEvent)
  }
  ```

  Note: Using `fun interface` (SAM) for cleaner lambda-friendly usage.

  **Must NOT do**:
  - Add Redis/infra dependencies to domain layer
  - Make event fields mutable
  - Use domain model types (TargetCurrency, ExchangeRate) — use primitives/Strings to avoid serialization coupling
  - Add business logic to event class

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: Two simple data files, no logic, no tests required
  - Skills: [] - Not needed
  - Omitted: [tdd, kotlin-specialist] - Data-only definitions

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: Task 3 | Blocked By: (none)

  **References**:
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt` - Event uses similar fields
  - AGENTS.md: "Outbound ports in domain/port/outbound/"

  **Acceptance Criteria** (agent-executable only):
  - [ ] Test-Path `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt` returns $true
  - [ ] Test-Path `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt` returns $true
  - [ ] ./gradlew compileKotlin succeeds

  **QA Scenarios**:
  ```
  Scenario: ExchangeRateFetchedEvent class created
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt) -match "data class ExchangeRateFetchedEvent|sourceCurrency|targetCurrency|rate|retrievedAt|rateDate"
    Expected: $true; 6 matches (class + 5 fields)
    Evidence: .sisyphus/evidence/task-1-event-class.log

  Scenario: ExchangeRateEventPort interface created
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt) -match "fun interface ExchangeRateEventPort|fun publish"
    Expected: $true; 2 matches
    Evidence: .sisyphus/evidence/task-1-port-interface.log

  Scenario: Kotlin compilation succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL"
    Evidence: .sisyphus/evidence/task-1-compile.log
  ```

  **Commit**: YES | Message: `domain(event): add ExchangeRateFetchedEvent and ExchangeRateEventPort for async cache save` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt, src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt]

---

- [ ] 2. Create RedisExchangeRateEventAdapter, ExchangeRateFetchedEventListener, and ExchangeRateEventsStreamConfig

  **What to do**:

  **Directory**: Create `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/`

  **File: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt`**
  ```kotlin
  package com.charlesluxinger.wex_transactions.infra.adapter.event.config

  data class ExchangeRateEventsStreamProperties(
      val key: String = "exchange-rate-fetched-events",
      val group: String = "exchange-rate-fetched-group",
      val consumer: String = "exchange-rate-fetched-consumer",
      val payloadField: String = "payload",
  )
  ```

  **File: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt`**
  ```kotlin
  package com.charlesluxinger.wex_transactions.infra.adapter.event

  import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
  import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateEventPort
  import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
  import com.fasterxml.jackson.databind.ObjectMapper
  import org.slf4j.LoggerFactory
  import org.springframework.data.redis.core.StringRedisTemplate
  import org.springframework.stereotype.Component

  @Component
  class RedisExchangeRateEventAdapter(
      private val stringRedisTemplate: StringRedisTemplate,
      private val objectMapper: ObjectMapper,
      private val streamProperties: ExchangeRateEventsStreamProperties,
  ) : ExchangeRateEventPort {

      override fun publish(event: ExchangeRateFetchedEvent) {
          runCatching {
              val payload = objectMapper.writeValueAsString(event)
              val streamKey = streamProperties.key
              val payloadRecord = mapOf(streamProperties.payloadField to payload)
              stringRedisTemplate.opsForStream<String, String>().add(streamKey, payloadRecord)
          }.onFailure { exception ->
              logger.warn(
                  "Failed to publish ExchangeRateFetchedEvent to stream={} due to {}: {}",
                  streamProperties.key,
                  exception.javaClass.simpleName,
                  exception.message,
              )
          }
      }

      companion object {
          private val logger = LoggerFactory.getLogger(RedisExchangeRateEventAdapter::class.java)
      }
  }
  ```

  **File: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt`**
  ```kotlin
  package com.charlesluxinger.wex_transactions.infra.adapter.event

  import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
  import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
  import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
  import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
  import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
  import com.fasterxml.jackson.databind.DeserializationFeature
  import com.fasterxml.jackson.databind.ObjectMapper
  import org.slf4j.LoggerFactory
  import org.springframework.data.redis.connection.stream.Consumer
  import org.springframework.data.redis.connection.stream.MapRecord
  import org.springframework.data.redis.core.StringRedisTemplate
  import org.springframework.data.redis.stream.StreamListener
  import org.springframework.stereotype.Component
  import java.util.UUID

  @Component
  class ExchangeRateFetchedEventListener(
      private val exchangeRateCachePort: ExchangeRateCachePort,
      private val stringRedisTemplate: StringRedisTemplate,
      private val streamProperties: ExchangeRateEventsStreamProperties,
      objectMapper: ObjectMapper,
  ) : StreamListener<String, MapRecord<String, String, String>> {

      private val eventReader = objectMapper.copy()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      private val consumerName = "${streamProperties.consumer}-${UUID.randomUUID()}"
      val consumer: Consumer = Consumer.from(streamProperties.group, consumerName)
      private val streamOps = stringRedisTemplate.opsForStream<String, String>()

      override fun onMessage(record: MapRecord<String, String, String>) {
          handleRecord(record)
      }

      private fun handleRecord(record: MapRecord<String, String, String>) {
          val event = extractPayload(record)?.let { payload ->
              runCatching { eventReader.readValue(payload, ExchangeRateFetchedEvent::class.java) }
                  .onFailure { ex ->
                      logger.warn("Ignoring malformed exchange-rate-fetched stream payload", ex)
                  }.getOrNull()
          } ?: return

          runCatching {
              val rate = ExchangeRate(
                  rate = event.rate,
                  sourceCurrency = TargetCurrency(event.sourceCurrency),
                  targetCurrency = TargetCurrency(event.targetCurrency),
                  retrievedAt = event.retrievedAt,
              )
              exchangeRateCachePort.saveRate(
                  sourceCurrency = TargetCurrency(event.sourceCurrency),
                  targetCurrency = TargetCurrency(event.targetCurrency),
                  rate = rate,
              )
          }.onSuccess {
              acknowledge(record)
          }.onFailure { ex ->
              logger.error("Failed to save exchange rate from stream event", ex)
          }
      }

      private fun extractPayload(record: MapRecord<String, String, String>): String? {
          val payload = record.value[streamProperties.payloadField]
              ?: record.value.values.singleOrNull()

          if (payload == null) {
              logger.warn("Ignoring exchange-rate-fetched stream payload with missing body")
          }
          return payload
      }

      private fun acknowledge(record: MapRecord<String, String, String>) {
          streamOps.acknowledge(streamProperties.group, record)
      }

      companion object {
          private val logger = LoggerFactory.getLogger(ExchangeRateFetchedEventListener::class.java)
      }
  }
  ```

  **File: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamConfig.kt`**
  ```kotlin
  package com.charlesluxinger.wex_transactions.infra.adapter.event.config

  import com.charlesluxinger.wex_transactions.infra.adapter.event.ExchangeRateFetchedEventListener
  import org.slf4j.LoggerFactory
  import org.springframework.context.annotation.Bean
  import org.springframework.context.annotation.Configuration
  import org.springframework.data.redis.RedisSystemException
  import org.springframework.data.redis.connection.RedisConnectionFactory
  import org.springframework.data.redis.connection.stream.MapRecord
  import org.springframework.data.redis.connection.stream.ReadOffset
  import org.springframework.data.redis.connection.stream.StreamOffset
  import org.springframework.data.redis.core.StreamOperations
  import org.springframework.data.redis.core.StringRedisTemplate
  import org.springframework.data.redis.stream.StreamMessageListenerContainer
  import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
  import java.time.Duration

  @Configuration
  class ExchangeRateEventsStreamConfig {

      @Bean
      fun exchangeRateEventsStreamProperties(): ExchangeRateEventsStreamProperties =
          ExchangeRateEventsStreamProperties()

      @Bean
      fun exchangeRateEventsStreamOffset(properties: ExchangeRateEventsStreamProperties): StreamOffset<String> =
          StreamOffset.create(properties.key, ReadOffset.lastConsumed())

      @Bean
      fun exchangeRateEventsStreamListenerContainer(
          redisConnectionFactory: RedisConnectionFactory,
          streamOffset: StreamOffset<String>,
          eventListener: ExchangeRateFetchedEventListener,
          stringRedisTemplate: StringRedisTemplate,
          properties: ExchangeRateEventsStreamProperties,
      ): StreamMessageListenerContainer<String, MapRecord<String, String, String>> {
          val streamOps = stringRedisTemplate.opsForStream<String, String>()
          createConsumerGroupIfMissing(streamOps, properties.key, properties.group)
          val options = StreamMessageListenerContainerOptions.builder()
              .pollTimeout(Duration.ofSeconds(2))
              .build()
          val container = StreamMessageListenerContainer.create(redisConnectionFactory, options)
          container.receive(eventListener.consumer, streamOffset, eventListener)
          container.start()
          return container
      }
  }

  private fun createConsumerGroupIfMissing(
      streamOps: StreamOperations<String, String, String>,
      streamKey: String,
      groupName: String,
  ) {
      try {
          streamOps.createGroup(streamKey, ReadOffset.latest(), groupName)
      } catch (ex: RedisSystemException) {
          val cause = ex.cause
          if (cause?.message?.contains("BUSYGROUP", ignoreCase = true) != true) {
              logger.warn("Failed to create Redis consumer group group={} stream={}", groupName, streamKey, ex)
          }
      }
  }

  private val logger = LoggerFactory.getLogger("ExchangeRateEventsStreamConfig")
  ```

  **Must NOT do**:
  - Name adapter `*PortImpl` (use `*Adapter` pattern per AGENTS.md)
  - Throw from `publish()` on Redis error (runCatching + warn)
  - Use `RedisTemplate<Object, Object>` (use `StringRedisTemplate`)
  - Fail app startup on group creation failure (best-effort only)
  - Add recovery scheduler (deferred from MVP)
  - Add idempotency logic (cache overwrite is safe)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Three infra files with stream pub/sub, container config, error handling
  - Skills: [kotlin-specialist] - Spring Boot auto-config, StringRedisTemplate Stream API
  - Omitted: [tdd] - Tests written in Task 4

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: Task 4 | Blocked By: Task 1

  **References**:
  - Pattern (publisher): `C:\Users\charl\Projetos\backend\betrader-backend\src\main\kotlin\io\bevion\betrader\infra\notification\CreateTradeEventPublisher.kt` - StringRedisTemplate.opsForStream().add(), runCatching, warn log
  - Pattern (listener): `C:\Users\charl\Projetos\backend\betrader-backend\src\main\kotlin\io\bevion\betrader\infra\notification\CreateTradeEventListener.kt` - StreamListener, extractPayload, acknowledge
  - Pattern (config): `C:\Users\charl\Projetos\backend\betrader-backend\src\main\kotlin\io\bevion\betrader\infra\notification\config\RedisTradeEventsConfig.kt` - createConsumerGroupIfMissing, StreamMessageListenerContainer
  - Existing: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt` - Failure-swallow style with runCatching
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt` - Contract to implement

  **Acceptance Criteria** (agent-executable only):
  - [ ] Test-Path `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt` returns $true
  - [ ] Test-Path `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt` returns $true
  - [ ] Test-Path `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt` returns $true
  - [ ] Test-Path `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamConfig.kt` returns $true
  - [ ] ./gradlew compileKotlin succeeds
  - [ ] Adapter implements ExchangeRateEventPort
  - [ ] Adapter wraps Redis Stream add in runCatching (no throw)
  - [ ] Listener acknowledges only after successful cache save
  - [ ] Listener uses UUID-suffixed consumer name

  **QA Scenarios**:
  ```
  Scenario: Adapter class created and implements port
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt) -match "class RedisExchangeRateEventAdapter|: ExchangeRateEventPort|StringRedisTemplate|runCatching"
    Expected: $true; 4 matches
    Evidence: .sisyphus/evidence/task-2-adapter-class.log

  Scenario: Listener class created with ack discipline
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt) -match "StreamListener|acknowledge|saveRate"
    Expected: $true; 3 matches
    Evidence: .sisyphus/evidence/task-2-listener-class.log

  Scenario: Config class creates consumer group best-effort
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamConfig.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamConfig.kt) -match "createGroup|BUSYGROUP|StreamMessageListenerContainer"
    Expected: $true; 3 matches
    Evidence: .sisyphus/evidence/task-2-config-class.log

  Scenario: Stream properties class
    Tool: PowerShell
    Steps:
      1. Test-Path src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt
      2. (Get-Content src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt) -match "exchange-rate-fetched-events|exchange-rate-fetched-group|exchange-rate-fetched-consumer|payload"
    Expected: $true; 4 matches
    Evidence: .sisyphus/evidence/task-2-stream-properties.log

  Scenario: Kotlin compilation succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL"
    Evidence: .sisyphus/evidence/task-2-compile.log
  ```

  **Commit**: YES | Message: `infra(event): add Redis Stream publisher, listener, and config for exchange-rate-fetched events` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt, src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListener.kt, src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt, src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamConfig.kt]

---

- [ ] 3. Refactor RetrieveConvertedUseCaseImpl — inject ExchangeRateEventPort, publish after client call, remove sync cache save

  **What to do**:
  - Open `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`
  - Add `ExchangeRateEventPort` as constructor parameter
  - Keep `exchangeRateCachePort` (still used for `getRate()` on line 32)
  - Remove `exchangeRateCachePort.saveRate()` call from `fetchClient()` method (lines 63-68)
  - Replace the `?.also { ... saveRate(...) }` block with a `runCatching` block that calls `exchangeRateEventPort.publish(...)`
  - The event `rateDate` must be the `rateDate: LocalDate` parameter (purchase.transactionDate)
  - The event `retrievedAt` must be `fetchedRate.retrievedAt`

  **Expected new fetchClient() structure**:
  ```kotlin
  private fun fetchClient(
      sourceCurrency: TargetCurrency,
      targetCurrency: TargetCurrency,
      rateDate: LocalDate,
  ): ExchangeRate? {
      val fetchedRate = exchangeRateClientPort.fetchNearestPriorRate(
          sourceCurrency = sourceCurrency,
          targetCurrency = targetCurrency,
          rateDate = rateDate,
      )

      if (fetchedRate != null) {
          runCatching {
              exchangeRateEventPort.publish(
                  ExchangeRateFetchedEvent(
                      sourceCurrency = sourceCurrency.code,
                      targetCurrency = targetCurrency.code,
                      rate = fetchedRate.rate,
                      retrievedAt = fetchedRate.retrievedAt,
                      rateDate = rateDate,
                  )
              )
          }
      }

      return fetchedRate
  }
  ```
  - Remove unused imports (check if any become unused)
  - Add imports for `ExchangeRateFetchedEvent` and `ExchangeRateEventPort`

  **Must NOT do**:
  - Change `retrieveConverted()` return type or signature
  - Change `RetrieveConvertedQuery` or `RetrieveConvertedResponse`
  - Remove `exchangeRateCachePort.getRate()` call (cache-first lookup stays)
  - Block the flow on publish failure (use `runCatching` — no throw)
  - Add coroutines or make method suspend
  - Change `exchangeRateClientPort` call

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: Application service refactor; event integration; dependency injection change
  - Skills: [kotlin-specialist] - Constructor injection, idiomatic Kotlin error handling
  - Omitted: [tdd] - Tests updated in Task 4

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: Task 4 | Blocked By: Task 1

  **References**:
  - File: `src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt` - Target file (current content lines 54-69)
  - Port: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/port/outbound/ExchangeRateEventPort.kt` - New dependency
  - Event: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/event/ExchangeRateFetchedEvent.kt` - Event to publish
  - Model: `src/main/kotlin/com/charlesluxinger/wex_transactions/domain/model/ExchangeRate.kt` - `retrievedAt` field source
  - Pattern: `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapter.kt` - runCatching pattern for failure isolation
  - AGENTS.md: "Use cases in application/service/**; orchestration only; no direct adapter access"

  **Acceptance Criteria** (agent-executable only):
  - [ ] ./gradlew compileKotlin succeeds
  - [ ] (Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "saveRate" | Measure-Object | Select-Object -ExpandProperty Count) -eq 0
  - [ ] (Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "eventPort" | Measure-Object | Select-Object -ExpandProperty Count) -ge 2
  - [ ] (Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "runCatching" | Measure-Object | Select-Object -ExpandProperty Count) -ge 1
  - [ ] (Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "ExchangeRateFetchedEvent" | Measure-Object | Select-Object -ExpandProperty Count) -ge 1

  **QA Scenarios**:
  ```
  Scenario: ExchangeRateEventPort injected
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "eventPort|ExchangeRateEventPort"
    Expected: ≥2 matches (import + constructor param)
    Evidence: .sisyphus/evidence/task-3-event-port-injection.log

  Scenario: No more sync saveRate call
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "saveRate" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: 0 (no inline cache save)
    Evidence: .sisyphus/evidence/task-3-no-save-rate.log

  Scenario: Publish wrapped in runCatching (fail-safe)
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "runCatching"
    Expected: ≥1 match
    Evidence: .sisyphus/evidence/task-3-run-catching.log

  Scenario: Kotlin compilation succeeds
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew compileKotlin 2>&1
    Expected: "BUILD SUCCESSFUL"
    Evidence: .sisyphus/evidence/task-3-compile.log
  ```

  **Commit**: YES | Message: `feat(app): inject ExchangeRateEventPort in RetrieveConvertedUseCaseImpl, publish event after client call, remove sync cache save` | Files: [src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt]

---

- [ ] 4. Write unit and integration tests

  **What to do**:

  **4a. Update existing `RetrieveConvertedUseCaseImplTest`** (`src/test/kotlin/.../application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt`):
  - Add `ExchangeRateEventPort` mock field
  - Pass to constructor
  - Update `cache miss fetches treasury and stores in cache` test:
    - Remove `verify(exchangeRateCachePort).saveRate(...)` assertion
    - Add `verify(exchangeRateEventPort).publish(...)` assertion — verify event contains correct sourceCurrency, targetCurrency, rate, retrievedAt, rateDate
    - Add `verify(exchangeRateCachePort, never()).saveRate(...)` — assert sync save is NOT called
  - Add new test: `publish failure does not block response`:
    - Mock `exchangeRateEventPort.publish(...)` to throw RuntimeException
    - Verify response still returns converted amount with correct values
  - Keep existing tests: cache hit, purchase not found, rate unavailable — unchanged

  **4b. Create `RedisExchangeRateEventAdapterTest`** (`src/test/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapterTest.kt`):
  - Mock `StringRedisTemplate`, `ObjectMapper`, and `ExchangeRateEventsStreamProperties`
  - Test: publish success — verify `opsForStream().add()` called with correct stream key and payload map
  - Test: publish failure — mock `opsForStream().add()` to throw, verify no exception propagates
  - Test: verify stream key comes from properties

  **4c. Create `ExchangeRateFetchedEventListenerTest`** (`src/test/kotlin/.../infra/adapter/event/ExchangeRateFetchedEventListenerTest.kt`):
  - Mock `ExchangeRateCachePort`, `StreamOperations`, simulated `MapRecord<String, String, String>`
  - Test: valid event — verify `cachePort.saveRate()` called with correct TargetCurrency and ExchangeRate values
  - Test: valid event — verify `streamOperations.acknowledge()` called
  - Test: malformed payload — verify no cache save, no acknowledge
  - Test: cache save failure — verify acknowledge NOT called (re-delivery behavior)

  **Must NOT do**:
  - Call actual Redis or external services (unit tests only)
  - Remove existing valid test assertions (`verify(clientPort, never())` in cache-hit test, etc.)
  - Leave `saveRate` as-called assertion in cache-miss test (must assert `never()` instead)
  - Skip publish-failure test (critical for non-blocking requirement)
  - Add integration tests (out of scope — unit tests with mocks cover all scenarios)

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: 3 test files: update 1 existing + create 2 new; multiple Mockito scenarios; comprehensive coverage
  - Skills: [tdd, kotlin-specialist] - Mockito patterns, Kotlin test conventions, argument captors
  - Omitted: [] - TDD patterns essential

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: F1-F4 | Blocked By: Task 2, Task 3

  **References**:
  - Existing test: `src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt` - Update this file
  - Test pattern: `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/cache/RedisExchangeRateCacheAdapterTest.kt` - Mockito + StringRedisTemplate pattern
  - Policy: `.specs/codebase/TESTING.md` - Unit (Mockito OK), Integration (Testcontainers)
  - JaCoCo gate: `build.gradle.kts` - 0.9 overall, 1.0 domain

  **Acceptance Criteria** (agent-executable only):
  - [ ] ./gradlew test --tests "com.charlesluxinger.wex_transactions.application.service.retrieveConverted.RetrieveConvertedUseCaseImplTest" passes (0 failures)
  - [ ] ./gradlew test --tests "com.charlesluxinger.wex_transactions.infra.adapter.event.RedisExchangeRateEventAdapterTest" passes (0 failures)
  - [ ] ./gradlew test --tests "com.charlesluxinger.wex_transactions.infra.adapter.event.ExchangeRateFetchedEventListenerTest" passes (0 failures)
  - [ ] ./gradlew test passes overall (0 failures)
  - [ ] ./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck passes
  - [ ] ./gradlew --no-daemon detekt passes

  **QA Scenarios**:
  ```
  Scenario: Use case test — all pass (cache miss publishes event, failure doesn't block)
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew test --tests "com.charlesluxinger.wex_transactions.application.service.retrieveConverted.RetrieveConvertedUseCaseImplTest" 2>&1
    Expected: "BUILD SUCCESSFUL" with 0 test failures
    Evidence: .sisyphus/evidence/task-4-usecase-test.log

  Scenario: Use case test — publish failure test exists
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt" "publish failure|does not block"
    Expected: ≥1 match (test case exists)
    Evidence: .sisyphus/evidence/task-4-publish-failure-test.log

  Scenario: Adapter unit test — all pass
    Tool: PowerShell
    Steps:
      1. ./gradlew test --tests "com.charlesluxinger.wex_transactions.infra.adapter.event.RedisExchangeRateEventAdapterTest" 2>&1
    Expected: "BUILD SUCCESSFUL" with 0 test failures
    Evidence: .sisyphus/evidence/task-4-adapter-test.log

  Scenario: Listener unit test — all pass
    Tool: PowerShell
    Steps:
      1. ./gradlew test --tests "com.charlesluxinger.wex_transactions.infra.adapter.event.ExchangeRateFetchedEventListenerTest" 2>&1
    Expected: "BUILD SUCCESSFUL" with 0 test failures
    Evidence: .sisyphus/evidence/task-4-listener-test.log

  Scenario: All tests pass + quality gates clean
    Tool: PowerShell
    Steps:
      1. ./gradlew test 2>&1 | Select-String "BUILD"
      2. ./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck 2>&1
      3. ./gradlew --no-daemon detekt 2>&1 | Select-String "violations"
    Expected: BUILD SUCCESSFUL; ktlint passes; detekt has 0 violations
    Evidence: .sisyphus/evidence/task-4-all-tests.log
  ```

  **Commit**: YES | Message: `test: add unit tests for event publish, adapter, and listener; update use case tests for async cache save` | Files: [src/test/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImplTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapterTest.kt, src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListenerTest.kt]

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before marking work complete.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before completing.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback → fix → re-run → present again → wait for okay.

- [ ] F1. Plan Compliance Audit — oracle

  **What to check**:
  - All Tasks 1-4 completed as specified
  - No scope creep (no coroutine parallelization, no recovery scheduler, no API changes)
  - All deliverables present (5 new files + 3 modified/test files)
  - Architecture compliance (hexagonal + DDD; outbound port in domain/; adapter in infra/)
  - Guardrails honored: publish isolated via runCatching, ack only after save success, best-effort startup, rateDate from purchase date

  **QA Scenarios**:
  ```
  Scenario: Verify all 4 tasks completed
    Tool: oracle (read-only review)
    Steps:
      1. Verify .sisyphus/plans/refactor-retrieve-converted.md matches git log (4 commits)
      2. Verify Tasks 1-4 checklist items all marked ✓
      3. Cross-check against original user request
    Expected: 4 commits with correct scopes; no ad-hoc changes
    Evidence: .sisyphus/evidence/F1-plan-compliance.log

  Scenario: Verify no scope creep
    Tool: oracle (review git diff)
    Steps:
      1. Review each task commit for unplanned changes
      2. Confirm only event-related files touched
    Expected: Zero out-of-scope modifications
    Evidence: .sisyphus/evidence/F1-no-creep.log

  Scenario: Verify architecture compliance
    Tool: oracle (code structure review)
    Steps:
      1. Confirm ExchangeRateEventPort in domain/port/outbound/
      2. Confirm RedisExchangeRateEventAdapter in infra/adapter/event/ (not *PortImpl)
      3. Confirm ExchangeRateFetchedEventListener in infra/adapter/event/
      4. Confirm RetrieveConvertedUseCaseImpl in application/service/
    Expected: All package placements match hexagonal DDD
    Evidence: .sisyphus/evidence/F1-architecture.log

  Scenario: Verify guardrails honored
    Tool: oracle (source code review)
    Steps:
      1. Confirm fetchClient() no longer calls saveRate()
      2. Confirm publish() wrapped in runCatching (no throw on failure)
      3. Confirm listener acks only after cachePort.saveRate() succeeds
      4. Confirm group creation best-effort (BUSYGROUP catch)
      5. Confirm event rateDate = purchase.transactionDate
    Expected: All 5 guardrails present
    Evidence: .sisyphus/evidence/F1-guardrails.log
  ```

- [ ] F2. Code Quality Review — unspecified-high

  **What to check**:
  - Zero detekt violations
  - Zero ktlint violations
  - Code follows project conventions (4-space indents, 120-char line limit, LF)
  - No AI slop patterns (verbose comments, redundant logic, unclear names)
  - runCatching used correctly (no unused Result, no .getOrThrow())
  - StringRedisTemplate.opsForStream() usage correct
  - Adapter not named *PortImpl
  - Listener ack discipline correct (ack only after save)

  **QA Scenarios**:
  ```
  Scenario: Detekt passes with zero violations
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew --no-daemon detekt 2>&1 | Select-String "violations"
    Expected: No violations found
    Evidence: .sisyphus/evidence/F2-detekt.log

  Scenario: ktlint passes
    Tool: PowerShell
    Steps:
      1. ./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck 2>&1
    Expected: Build successful (no formatting errors)
    Evidence: .sisyphus/evidence/F2-ktlint.log

  Scenario: No saveRate in use case
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "saveRate" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: 0
    Evidence: .sisyphus/evidence/F2-no-save-rate.log

  Scenario: runCatching in adapter publish
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapter.kt" "runCatching"
    Expected: ≥1 match
    Evidence: .sisyphus/evidence/F2-run-catching.log
  ```

- [ ] F3. Real Manual QA — unspecified-high (+ playwright if API)

  **What to check**:
  - Smoke test: verify all existing API endpoints still respond correctly
  - Verify event publish with real Redis (docker compose up)
  - Verify no regression in retrieveConverted flow

  **QA Scenarios**:
  ```
  Scenario: All tests pass
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew --no-daemon test jacocoTestReport 2>&1
    Expected: BUILD SUCCESSFUL, all tests pass, JaCoCo report generated
    Evidence: .sisyphus/evidence/F3-tests-pass.log

  Scenario: Compilation and quality gates clean
    Tool: PowerShell
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. ./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck detekt 2>&1
    Expected: No violations, BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/F3-quality-gates.log
  ```

- [ ] F4. Scope Fidelity Check — deep

  **What to check**:
  - Only planned files were created/modified
  - No coroutine parallelization added
  - No recovery scheduler added
  - No changes to cache port, cache adapter, or controller
  - No changes to API contracts
  - Event model fields match spec (sourceCurrency, targetCurrency, rate, retrievedAt, rateDate)
  - Stream key: `exchange-rate-fetched-events`, group: `exchange-rate-fetched-group`

  **QA Scenarios**:
  ```
  Scenario: Verify only planned files changed
    Tool: PowerShell (git diff)
    Steps:
      1. cd C:\Users\charl\Projetos\wex-transactions
      2. rtk git diff --name-only HEAD~4
    Expected: Only the 10 planned files (6 new source + 3 test + 1 modified use case)
    Evidence: .sisyphus/evidence/F4-scope-fidelity.log

  Scenario: Verify no coroutine changes
    Tool: PowerShell (grep)
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt" "suspend|async|coroutine|kotlinx" | Measure-Object | Select-Object -ExpandProperty Count
    Expected: 0 (no coroutine additions)
    Evidence: .sisyphus/evidence/F4-no-coroutines.log

  Scenario: Verify stream properties correct
    Tool: PowerShell
    Steps:
      1. Select-String -Path "src/main/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt" "exchange-rate-fetched-events|exchange-rate-fetched-group"
    Expected: Both matches found
    Evidence: .sisyphus/evidence/F4-stream-props.log
  ```

## Commit Strategy
| Task | Type | Scope | Description | Files |
|------|------|-------|-------------|-------|
| 1 | domain | event | Add ExchangeRateFetchedEvent and ExchangeRateEventPort | 2 new |
| 2 | infra | event | Add Redis Stream publisher, listener, properties, config | 4 new |
| 3 | feat | app | Inject ExchangeRateEventPort, publish event, remove sync save | 1 modified |
| 4 | test | - | Add unit tests and update existing use case test | 2 new + 1 modified |
| **Total** | | | **6 new source + 1 modified source + 2 new test + 1 modified test** | **10 files** |

## Success Criteria
- All 4 implementation tasks completed with passing acceptance criteria
- All 4 verification agents (F1-F4) approve
- User explicitly confirms "okay" before final completion
- Plan draft cleaned up
