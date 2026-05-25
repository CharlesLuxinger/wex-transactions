# TraceID + Centralized Logging

## TL;DR
> **Summary**: Add TraceID propagation (HTTP → Redis Stream → Consumer) via MDC, AOP layer-boundary logging, and structured logback pattern.
> **Deliverables**: TraceIdFilter, logback-spring.xml, AOP logging aspect, envelope traceId in Redis Stream, tests
> **Effort**: Medium (6 tasks, ~300 lines new code)
> **Parallel**: YES — 4 waves
> **Critical Path**: Task 1 → Tasks 2+3 → Tasks 4+5 → Task 6

## Context
### Original Request
Centralized logging + TraceID in logging context. TraceID propagates from publisher → consumer via queue messages.

### Interview Summary
- TraceID stored in SLF4J MDC, UUID-generated, honors inbound `X-Trace-Id` header
- Messaging envelope carries traceId as separate Redis stream field (not in domain event)
- AOP logs layer boundaries: controllers, use case implementations, outbound adapters
- Structured text log pattern (not JSON)
- Companion object logger pattern preserved

### Metis Review (gaps addressed)
- Missing envelope traceId handled gracefully (backward compat)
- Consumer MDC cleared in `finally` block
- No request/response body logging
- Pointcuts whitelisted by package, not catch-all
- Log assertions via `ListAppender` / `OutputCaptureExtension`
- Tests cover: happy path, missing envelope traceId, malformed envelope, exception path

## Work Objectives
### Core Objective
Every HTTP request gets a unique TraceID visible across all log lines for that request's flow, including the async Redis stream consumer processing the published event.

### Deliverables
1. `TraceIdFilter` — honors `X-Trace-Id` or generates UUID, sets/clears MDC
2. `logback-spring.xml` — pattern includes `[%X{traceId}]`
3. `ApplicationFlowLoggingAspect` — logs entry/exit/timing for controllers, use cases, outbound adapters
4. Envelope traceId in `RedisExchangeRateEventAdapter` — adds `traceId` field to stream record
5. Envelope traceId extraction in `ExchangeRateFetchedEventListener` — reads `traceId`, sets/clears MDC
6. Full test coverage per layer

### Definition of Done (verifiable conditions with commands)
```
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
All tests pass. Code coverage thresholds maintained. No detekt baseline changes.

### Must Have
- MDC traceId visible in all log lines from request → response
- Redis stream record contains `traceId` field alongside `payload`
- Listener sets MDC traceId from envelope, clears in `finally`
- Missing traceId in old messages handled gracefully (falls back to generated UUID)
- AOP logs each layer boundary with START/SUCCESS/ERROR markers
- AOP includes elapsed time in SUCCESS/ERROR

### Must NOT Have
- No traceId in domain event (`ExchangeRateFetchedEvent`)
- No request/response body logging in AOP aspect
- No OpenTelemetry, Zipkin, or distributed tracing
- No companion object logger replacement
- No payload logging in aspect

## Verification Strategy
> **ZERO HUMAN INTERVENTION** — all verification agent-executed.
- **Test decision**: TDD (RED-GREEN-REFACTOR)
- **Framework**: JUnit5 + Mockito (unit), ListAppender (log assertions), RestAssured + Testcontainers (integration)
- **QA policy**: Every task has agent-executed scenarios
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves

Wave 1: [Foundation] dependency + logback config
Wave 2: [Core components] filter + AOP aspect (parallel)
Wave 3: [Messaging] publisher envelope + consumer MDC (parallel)
Wave 4: [Integration] end-to-end traceId test

### Dependency Matrix

| Task | Depends On | Blocks |
|------|-----------|--------|
| 1. AOP dep + logback | — | 2, 3 |
| 2. TraceIdFilter + tests | 1 | 4, 5 |
| 3. AOP aspect + tests | 1 | 6 |
| 4. Publisher envelope + tests | 2 | 6 |
| 5. Consumer MDC + tests | 2 | 6 |
| 6. Integration test | 3, 4, 5 | — |

### Agent Dispatch Summary

| Wave | Tasks | Category |
|------|-------|----------|
| 1 | 1 | quick |
| 2 | 2, 3 | quick, quick |
| 3 | 4, 5 | quick, quick |
| 4 | 6 | unspecified-high |

## TODOs
> Implementation + Test = ONE task.

- [ ] 1. Add AOP dependency and create logback-spring.xml

  **What to do**:
  1. Add `implementation("org.springframework.boot:spring-boot-starter-aop")` to `build.gradle.kts` dependencies block (after line 50, before runtimeOnly)
  2. Create `src/main/resources/logback-spring.xml` with Spring Boot-compatible config:
     - Console appender with pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-}] %logger{36} - %msg%n`
     - Root level INFO
     - Spring profile support (no change needed for dev/prod)

  **Must NOT do**: Do not remove or modify any existing dependency.

  **Recommended Agent Profile**:
  - Category: `quick` — two trivial file changes
  - Skills: [] — no skills needed
  - Omitted: all — not needed

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [2,3] | Blocked By: []

  **References**:
  - Build file: `build.gradle.kts:41-63` — existing dependency block structure
  - Resource location: `src/main/resources/application.yaml` — sibling config file

  **Acceptance Criteria**:
  - [ ] `./gradlew dependencies` resolves without error after adding aop starter
  - [ ] `src/main/resources/logback-spring.xml` exists and valid XML
  - [ ] App starts without error

  **QA Scenarios**:
  ```
  Scenario: Dependency resolves
    Tool: Bash
    Steps: run `./gradlew dependencies --configuration runtimeClasspath | Select-String "spring-boot-starter-aop"`
    Expected: output contains "spring-boot-starter-aop"
    Evidence: .sisyphus/evidence/task-1-dep.txt

  Scenario: App starts with logback config
    Tool: Bash
    Steps: run `./gradlew build 2>&1 | Select-String "BUILD SUCCESS"`
    Expected: output contains "BUILD SUCCESS"
    Evidence: .sisyphus/evidence/task-1-build.txt
  ```

  **Commit**: YES | Message: `feat(logging): add spring-boot-starter-aop and logback-spring.xml with traceId pattern` | Files: [build.gradle.kts, src/main/resources/logback-spring.xml]

---

- [ ] 2. Create TraceIdFilter (OncePerRequestFilter) + unit tests

  **What to do**:
  1. Create `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/logging/TraceIdFilter.kt`:
     - Extends `OncePerRequestFilter`
     - Reads `X-Trace-Id` header from request; if present and non-blank, use it; otherwise generate `UUID.randomUUID().toString().replace("-", "")`
     - Sets MDC key `traceId` with the value
     - Calls `filterChain.doFilter(request, response)`
     - In `finally` block: `MDC.clear()`
     - `@Component` with `@Order(Ordered.HIGHEST_PRECEDENCE)`
     - `shouldNotFilter` for non-HTTP or actuator paths (`/actuator/health` etc.)
  2. Create `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/logging/TraceIdFilterTest.kt`:
     - Test: when X-Trace-Id header present, filter uses it (verify via MDC get)
     - Test: when X-Trace-Id header absent, filter generates UUID (verify format)
     - Test: when X-Trace-Id header blank/empty, filter generates UUID
     - Test: MDC is cleared after request completes
     - Test: filter is skipped for actuator path
     - Use `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`
     - Assert MDC via `MDC.get("traceId")` before/after chain

  **Must NOT do**: Do not set response header for traceId. Do not log request body.

  **Recommended Agent Profile**:
  - Category: `quick` — single filter + single test file
  - Skills: [] — pure Spring + SLF4J MDC, no external libs needed
  - Omitted: all

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: [4,5] | Blocked By: [1]

  **References**:
  - Controller pattern: `src/main/kotlin/.../infra/client/purchase/PurchaseControllerV1.kt` — package structure
  - Existing filter pattern: none yet — this is the first filter
  - Test pattern: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt` — test structure with MockHttpServletRequest

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*TraceIdFilter*"` passes
  - [ ] `./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck` passes
  - [ ] All 5 test scenarios pass

  **QA Scenarios**:
  ```
  Scenario: Uses inbound X-Trace-Id
    Tool: Bash
    Steps: run `./gradlew test --tests "*TraceIdFilter*shouldUseInboundHeader*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-2-filter-header.txt

  Scenario: Generates UUID when header absent
    Tool: Bash
    Steps: run `./gradlew test --tests "*TraceIdFilter*shouldGenerateUuidWhenNoHeader*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-2-filter-uuid.txt

  Scenario: MDC cleared after request
    Tool: Bash
    Steps: run `./gradlew test --tests "*TraceIdFilter*shouldClearMdcAfterRequest*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-2-filter-cleanup.txt

  Scenario: Skipped for actuator paths
    Tool: Bash
    Steps: run `./gradlew test --tests "*TraceIdFilter*shouldSkipActuatorPath*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-2-filter-skip.txt
  ```

  **Commit**: YES | Message: `feat(logging): add TraceIdFilter with X-Trace-Id header support and MDC management` | Files: [src/main/kotlin/.../infra/logging/TraceIdFilter.kt, src/test/kotlin/.../infra/logging/TraceIdFilterTest.kt]

---

- [ ] 3. Create AOP logging aspect (ApplicationFlowLoggingAspect) + config + tests

  **What to do**:
  1. Create `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/logging/AopLoggingConfig.kt`:
     - `@Configuration @EnableAspectJAutoProxy(proxyTargetClass = true)` class
  2. Create `src/main/kotlin/com/charlesluxinger/wex_transactions/infra/logging/ApplicationFlowLoggingAspect.kt`:
     - `@Aspect @Component` class
     - `private val logger = LoggerFactory.getLogger(this::class.java)`
     - `@Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * *(..))")` → logs as `[CONTROLLER]`
     - `@Around("execution(public * com.charlesluxinger.wex_transactions.application.service..*UseCaseImpl.*(..))")` → logs as `[USECASE]`
     - `@Around("execution(public * com.charlesluxinger.wex_transactions.infra.adapter..*.*(..))")` → logs as `[ADAPTER]`
     - Helper method `logExecutionStep(layer, joinPoint)`:
       - Records `signature = ClassName.methodName`
       - Records `args = joinPoint.args` as `[arg1, arg2]` (toString only — no nested expansion)
       - `logger.info("[{}][START] {} args={}", layer, signature, args)`
       - `joinPoint.proceed()` → `[SUCCESS]` with `elapsedMs`
       - On exception: `[ERROR]` with `elapsedMs` + exception message, then rethrow
  3. Create `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/logging/ApplicationFlowLoggingAspectTest.kt`:
     - Use Spring Boot test slice with `@SpringBootTest` + minimal context
     - Use `ch.qos.logback.classic.Logger` + `ListAppender` to capture log events
     - Stub a controller bean and use case bean in the context
     - Test: controller method produces `[CONTROLLER][START]` and `[CONTROLLER][SUCCESS]`
     - Test: use case method produces `[USECASE][START]` and `[USECASE][SUCCESS]`
     - Test: failing method produces `[ERROR]` with `elapsedMs=`
     - Test: log events contain traceId in MDC (set MDC before call)
  
  **Must NOT do**: Do not log full request/response bodies. Do not include all public methods — only the three pointcut groups.

  **Recommended Agent Profile**:
  - Category: `quick` — 2 source files + 1 test file, well-defined pattern
  - Skills: [] — standard Spring AOP + Logback
  - Omitted: all

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [6] | Blocked By: [1]

  **References**:
  - Betrader reference: `C:\Users\charl\Projetos\backend\betrader-backend\src\main\kotlin\io\bevion\betrader\infra\logging\ApplicationFlowLoggingAspect.kt` — AOP pattern to follow
  - Betrader reference: `C:\Users\charl\Projetos\backend\betrader-backend\src\main\kotlin\io\bevion\betrader\infra\logging\AopLoggingConfig.kt` — config pattern
  - Existing controllers: `PurchaseControllerV1.kt`, `RetrieveConvertedControllerV1.kt` — pointcut targets
  - Existing use cases: `StorePurchaseUseCaseImpl.kt`, `RetrieveConvertedUseCaseImpl.kt` — pointcut targets
  - Existing adapters: `RedisExchangeRateEventAdapter.kt`, `RedisExchangeRateCacheAdapter.kt`, `ExchangeRateTreasuryAdapter.kt`, `PurchaseRepositoryJPAAdapter.kt` — pointcut targets

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*ApplicationFlowLoggingAspect*"` passes
  - [ ] Aspect logs CONTROLLER, USECASE, ADAPTER layers
  - [ ] `./gradlew detekt` passes (no baseline changes)

  **QA Scenarios**:
  ```
  Scenario: Controller produces START and SUCCESS logs
    Tool: Bash
    Steps: run `./gradlew test --tests "*ApplicationFlowLoggingAspectTest*shouldLogControllerStartAndSuccess*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-3-aspect-controller.txt

  Scenario: Use case logs with elapsed time
    Tool: Bash
    Steps: run `./gradlew test --tests "*ApplicationFlowLoggingAspectTest*shouldLogUseCaseWithElapsed*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-3-aspect-usecase.txt

  Scenario: Exception produces ERROR log
    Tool: Bash
    Steps: run `./gradlew test --tests "*ApplicationFlowLoggingAspectTest*shouldLogErrorOnException*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-3-aspect-error.txt
  ```

  **Commit**: YES | Message: `feat(logging): add AOP logging aspect for controller, use case, and adapter layers` | Files: [src/main/kotlin/.../infra/logging/AopLoggingConfig.kt, src/main/kotlin/.../infra/logging/ApplicationFlowLoggingAspect.kt, src/test/kotlin/.../infra/logging/ApplicationFlowLoggingAspectTest.kt]

---

- [ ] 4. Modify RedisExchangeRateEventAdapter to include traceId in stream envelope + tests

  **What to do**:
  1. Modify `RedisExchangeRateEventAdapter.kt`:
     - Inject `traceId` from MDC: `MDC.get("traceId") ?: UUID.randomUUID().toString().replace("-", "")`
     - Add `traceId` to the `payloadRecord` map: `mapOf(streamProperties.payloadField to payload, streamProperties.traceIdField to traceId)`
     - Log warning if MDC traceId is missing (fallback to generated)
  2. Modify `ExchangeRateEventsStreamProperties.kt`:
     - Add field: `val traceIdField: String = "traceId"`
  3. Create `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/RedisExchangeRateEventAdapterTraceIdTest.kt`:
     - Mock StringRedisTemplate and ObjectMapper
     - Test: published record contains both `payload` and `traceId` fields
     - Test: traceId is taken from MDC when present
     - Test: traceId is generated when MDC is empty (fallback)
     - Test: payload content is unchanged (ExchangeRateFetchedEvent serialization preserved)
  
  **Must NOT do**: Do not modify `ExchangeRateFetchedEvent` domain class. Do not change the `publish` method signature.

  **Recommended Agent Profile**:
  - Category: `quick` — modify 2 existing files + 1 test file
  - Skills: [] — standard Mockito + Redis testing
  - Omitted: all

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [6] | Blocked By: [2]

  **References**:
  - Existing adapter: `src/main/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapter.kt:17-30` — publish method to modify
  - Existing properties: `src/main/kotlin/.../infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt` — add traceIdField
  - Existing test: `src/test/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapterTest.kt` — test structure reference
  - Listener: `src/main/kotlin/.../infra/adapter/event/ExchangeRateFetchedEventListener.kt:66-75` — consumer that reads payload field

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*RedisExchangeRateEventAdapter*"` passes (existing + new tests)
  - [ ] `./gradlew detekt` passes
  - [ ] Published record has traceId field alongside payload

  **QA Scenarios**:
  ```
  Scenario: TraceId present in published record
    Tool: Bash
    Steps: run `./gradlew test --tests "*RedisExchangeRateEventAdapterTraceIdTest*shouldIncludeTraceIdInRecord*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-4-adapter-traceId.txt

  Scenario: Payload unchanged when traceId added
    Tool: Bash
    Steps: run `./gradlew test --tests "*RedisExchangeRateEventAdapterTraceIdTest*shouldPreservePayloadContent*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-4-adapter-payload.txt

  Scenario: Fallback generates UUID when MDC empty
    Tool: Bash
    Steps: run `./gradlew test --tests "*RedisExchangeRateEventAdapterTraceIdTest*shouldGenerateTraceIdWhenMdcEmpty*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-4-adapter-fallback.txt
  ```

  **Commit**: YES | Message: `feat(logging): propagate traceId in Redis stream envelope from publisher` | Files: [src/main/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapter.kt, src/main/kotlin/.../infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt, src/test/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapterTraceIdTest.kt]

---

- [ ] 5. Modify ExchangeRateFetchedEventListener to extract traceId from envelope + tests

  **What to do**:
  1. Modify `ExchangeRateFetchedEventListener.kt`:
     - In `handleRecord()` method, before processing:
       - Read `traceId` from `record.value[streamProperties.traceIdField]`
       - If present and non-blank: `MDC.put("traceId", traceId)`
       - If absent/blank: generate `UUID.randomUUID().toString().replace("-", "")` and put in MDC
     - Wrap processing in `try { ... } finally { MDC.clear() }`
  2. Create `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/adapter/event/ExchangeRateFetchedEventListenerTraceIdTest.kt`:
     - Mock ExchangeRateCachePort, StringRedisTemplate, stream properties
     - Test: when record has traceId, MDC contains that traceId during processing
     - Test: when record has no traceId, MDC contains generated UUID
     - Test: MDC is cleared after processing (in finally)
     - Test: listener still processes successfully when traceId field is absent (backward compat)
     - Test: exception during processing still clears MDC

  **Must NOT do**: Do not modify `onMessage` signature or add traceId to domain event. Do not log payload/event body.

  **Recommended Agent Profile**:
  - Category: `quick` — modify 1 existing file + 1 test file
  - Skills: [] — standard Mockito + MDC testing
  - Omitted: all

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [6] | Blocked By: [2]

  **References**:
  - Existing listener: `src/main/kotlin/.../infra/adapter/event/ExchangeRateFetchedEventListener.kt:37-64` — handleRecord method to modify
  - Existing test: `src/test/kotlin/.../infra/adapter/event/ExchangeRateFetchedEventListenerTest.kt` — test structure reference
  - Stream properties: `src/main/kotlin/.../infra/adapter/event/config/ExchangeRateEventsStreamProperties.kt` — traceIdField getter

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*ExchangeRateFetchedEventListener*"` passes (existing + new tests)
  - [ ] `./gradlew detekt` passes
  - [ ] MDC traceId is set from envelope and cleared after processing

  **QA Scenarios**:
  ```
  Scenario: MDC set from envelope traceId
    Tool: Bash
    Steps: run `./gradlew test --tests "*ExchangeRateFetchedEventListenerTraceIdTest*shouldSetMdcFromEnvelope*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-5-listener-mdc-set.txt

  Scenario: MDC cleared in finally after processing
    Tool: Bash
    Steps: run `./gradlew test --tests "*ExchangeRateFetchedEventListenerTraceIdTest*shouldClearMdcAfterProcessing*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-5-listener-mdc-clear.txt

  Scenario: Handles missing traceId (backward compat)
    Tool: Bash
    Steps: run `./gradlew test --tests "*ExchangeRateFetchedEventListenerTraceIdTest*shouldHandleMissingTraceId*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-5-listener-backward.txt

  Scenario: MDC cleared even on exception
    Tool: Bash
    Steps: run `./gradlew test --tests "*ExchangeRateFetchedEventListenerTraceIdTest*shouldClearMdcEvenOnException*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-5-listener-exception.txt
  ```

  **Commit**: YES | Message: `feat(logging): extract and propagate traceId from Redis stream envelope in consumer` | Files: [src/main/kotlin/.../infra/adapter/event/ExchangeRateFetchedEventListener.kt, src/test/kotlin/.../infra/adapter/event/ExchangeRateFetchedEventListenerTraceIdTest.kt]

---

- [ ] 6. Integration test: end-to-end traceId flow via RestAssured + Redis

  **What to do**:
  1. Create `src/test/kotlin/com/charlesluxinger/wex_transactions/infra/client/retrieveConverted/RetrieveConvertedControllerV1TraceIdIntegrationTest.kt`:
     - Extends `AbstractRestApiIntegrationTest` (existing base class)
     - Test: POST purchase with X-Trace-Id header → response captured
     - Test: Retrieve converted purchase → verify traceId consistent across API calls via log assertions
     - Use `OutputCaptureExtension` or Logback `ListAppender` attached to root logger before test
     - Capture all log events during the request
     - Assert: all log lines for the flow contain the same traceId
     - Assert: `[CONTROLLER]`, `[USECASE]`, `[ADAPTER]` markers appear in captured logs
     - Set up Redis stream consumer group before test (use `TestContainersSupport` if available)
     - Note: This test may require the full Redis + PostgreSQL stack via Testcontainers
     - If Testcontainers is too heavy, write as a focused Spring integration test with embedded components
  2. Verify with:
     ```
     ./gradlew test --tests "*TraceIdIntegrationTest*"
     ./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
     ./gradlew detekt
     ```

  **Must NOT do**: Do not assert on specific log message text (brittle). Assert on markers and traceId values only. Do not modify existing integration test infrastructure.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — integration test touching Redis, PostgreSQL, and multiple layers
  - Skills: [] — standard Testcontainers + RestAssured
  - Omitted: all

  **Parallelization**: Can Parallel: NO | Wave 4 | Blocks: [] | Blocked By: [3, 4, 5]

  **References**:
  - Existing base class: `src/test/kotlin/.../config/AbstractRestApiIntegrationTest.kt` — test infrastructure
  - Existing test: `src/test/kotlin/.../client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt` — similar integration test pattern
  - Testcontainers config: `src/test/kotlin/.../config/TestContainersConfig.kt`, `TestContainersSupport.kt`
  - Logback ListAppender: `ch.qos.logback.core.read.ListAppender` — attach to root logger
  - Supported vs existing: `ContainersConfig.kt` — check what containers are already configured

  **Acceptance Criteria**:
  - [ ] `./gradlew test --tests "*RetrieveConvertedControllerV1TraceIdIntegrationTest*"` passes
  - [ ] Logs show same traceId across all layers
  - [ ] `./gradlew test jacocoTestReport` maintains coverage thresholds

  **QA Scenarios**:
  ```
  Scenario: TraceId consistent across layers in one request
    Tool: Bash
    Steps: run `./gradlew test --tests "*RetrieveConvertedControllerV1TraceIdIntegrationTest*shouldMaintainTraceIdAcrossLayers*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-6-integration-consistency.txt

  Scenario: All layer markers present in logs
    Tool: Bash
    Steps: run `./gradlew test --tests "*RetrieveConvertedControllerV1TraceIdIntegrationTest*shouldLogAllLayerMarkers*" 2>&1 | Select-String "PASSED"`
    Expected: output contains "PASSED"
    Evidence: .sisyphus/evidence/task-6-integration-markers.txt
  ```

  **Commit**: YES | Message: `test(logging): add end-to-end integration test for traceId propagation across layers` | Files: [src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1TraceIdIntegrationTest.kt]

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- 6 atomic commits, one per task
- Each commit includes implementation + tests
- Order: Wave 1 → Wave 2 → Wave 3 → Wave 4

## Success Criteria
```powershell
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport
```
All pass. No baseline changes. Coverage >= 90% overall, 100% domain.
