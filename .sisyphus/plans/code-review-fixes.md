# Code Review Fixes — WEX Transactions

## TL;DR
> **Summary**: Execute 30+ fixes across domain, validation, architecture tests, resilience, observability, exception handling, and test coverage based on comprehensive 8-phase code review.
> **Deliverables**: Updated DECISIONS.md + STRUCTURE.md, DB schema migration (18,6→18,2), USD-only enforcement, architecture test hardening, Resilience4j integration (CB + Bulkhead + RateLimiter), WireMock test infrastructure, unified validation, treasury robustness, exception handling improvements, observability fixes, and 7+ new test scenarios.
> **Effort**: Large
> **Parallel**: YES — 3 waves
> **Critical Path**: T2+T3 (Schema+Validation) → T7 (Resilience4j) → T10 (WireMock+StoreUseCase) → T13 (API Mock Replacement); T8 (Treasury) → T11 (Treasury Tests)

## Context
### Original Request
Executar 30+ itens de correção selecionados de revisão de código (8 fases) no projeto WEX Transactions.

### Interview Summary
Decisões técnicas confirmadas:
- Scale 2: DB schema migrado DECIMAL(18,6) → DECIMAL(18,2), manter scale 2 no domínio
- Rate limiting: Resilience4j RateLimiter
- Circuit breaker: Resilience4j CircuitBreaker + Bulkhead
- Mock replacement: WireMock para API tests
- Validation: Unificar em Bean Validation; remover duplicação do controller
- Spec ambiguities: Resolver e documentar em DECISIONS.md
- Package declaration: Ajustar STRUCTURE.md (não mover código)

### Metis Review
Gap analysis não foi concluída (erro). Guardrails e riscos identificados manualmente durante planejamento.

## Work Objectives
### Core Objective
Corrigir 30+ itens da revisão de código, organizados em 3 ondas: (1) Foundation — schemas, specs, validação; (2) Production Hardening — resiliência, observabilidade, exception handling; (3) Tests — cobertura de cenários faltantes.

### Deliverables
- DECISIONS.md atualizado com ambiguidades resolvidas
- STRUCTURE.md alinhado com pacote real
- DB schema DECIMAL(18,2) com migration editada
- USD-only enforcement em domínio + controller
- Testes arquiteturais sem passes vacuosos
- Validação unificada em Bean Validation, controller limpo
- GlobalExceptionHandler cobrindo FeignException + IllegalStateException + erros multi-campo
- Logging seguro (args resumidos, trace ID sanitizado)
- Resilience4j CircuitBreaker + Bulkhead + RateLimiter
- Parsing Treasury robusto (toBigDecimalOrNull, case-insensitive null)
- WireMock + testes de integração expandidos
- 7+ novos cenários de teste

### Definition of Done (verifiable conditions with commands)
```powershell
./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew detekt
./gradlew test
```
- All checks pass
- No tests skipped
- JaCoCo coverage >= 90% overall, 100% domain

### Must Have
- USD-only enforcement no store flow
- DB schema consistente com scale 2
- Testes arquiteturais falham quando escopo está vazio
- Resilience4j configurado e integrado
- WireMock substituindo mocks nos API tests
- Logging sem vazamento de dados sensíveis

### Must NOT Have (guardrails)
- NÃO mudar direção de dependência (infra→application→domain)
- NÃO remover validações de domínio existentes (apenas consolidar)
- NÃO adicionar retry/DLQ no evento (apenas logging)
- NÃO mover pacotes (ajustar apenas declaração STRUCTURE.md)
- NÃO adicionar actuator health (fora de escopo)
- NÃO adicionar SCA/CVE scanning (fora de escopo)
- NÃO alterar comportamento de cache existente

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- **Test decision**: Tests-after (projeto existente, não TDD puro)
- **Framework**: JUnit5 + Mockito (unit), RestAssured (API), Testcontainers (integration)
- **QA policy**: Every task has agent-executed scenarios
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves

**Wave 1 — Foundation (4 tasks, parallel):**
T1 (Spec/Docs), T2 (Schema+Scale), T3 (USD+Validation), T4 (Arch Tests)

**Wave 2 — Production Hardening (5 tasks, after Wave 1):**
T5 (Exception Handling), T6 (Observability), T7 (Resilience4j), T8 (Treasury), T9 (Persistence Guard)

**Wave 3 — Tests (4 tasks, after Wave 2):**
T10 (WireMock+StoreUseCase), T11 (Treasury+6mo Tests), T12 (Idempotency+422), T13 (API Mock Replacement)

### Dependency Matrix
| Task | Depends On | Blocks |
|------|-----------|--------|
| T1 (Spec/Docs) | — | — |
| T2 (Schema) | — | T7, T8 |
| T3 (USD+Validation) | — | T5, T7, T10 |
| T4 (Arch Tests) | — | — |
| T5 (Exception Handling) | T3 | — |
| T6 (Observability) | — | — |
| T7 (Resilience4j) | T2 | T10, T13 |
| T8 (Treasury) | T2 | T11 |
| T9 (Persistence Guard) | — | — |
| T10 (WireMock + StoreUseCase) | T3, T7 | T13 |
| T11 (Treasury + 6mo Tests) | T8 | — |
| T12 (Idempotency + 422) | T3 | — |
| T13 (API Mock Replacement) | T7, T10 | — |

### Agent Dispatch Summary
| Wave | Tasks | Categories |
|------|-------|-----------|
| 1 | T1..T4 (4) | quick, quick, unspecified-low, unspecified-high |
| 2 | T5..T9 (5) | unspecified-high, unspecified-low, unspecified-high, unspecified-high, unspecified-low |
| 3 | T10..T13 (4) | unspecified-high, unspecified-low, unspecified-low, unspecified-high |

## TODOs

- [ ] T1. Resolve Spec Ambiguities + Update Documentation

  **What to do**:
  - Update `.specs/features/wex-tech-challenge/DECISIONS.md`:
    - Resolve item 17: Treasury endpoint — document `/rates_of_exchange` como canonical endpoint
    - Resolve item 18: 6-month arithmetic — documentar calendar-month subtraction (`minusMonths(6)`) como algoritmo usado
    - Resolve item 29: HTTP status mapping — documentar mapeamento: 400 (validation/IllegalArgument), 404 (PurchaseNotFound), 422 (RateUnavailable), 500 (infra errors)
    - Update `UNRESOLVED` register: mover resolved items para resolved section
  - Update `.specs/codebase/STRUCTURE.md`:
    - Current: `infra/persistence/**` (target)
    - Change to: `infra/adapter/persistence/**` (actual package)
  - Document API contract details:
    - Item 46: Adicionar nota em DECISIONS.md que `targetCurrency` é query parameter **obrigatório** (required conversion selector)
    - Item 49: Adicionar nota em DECISIONS.md que list/collection/delete estão out of scope

  **Must NOT do**: Não alterar código fonte. Apenas documentação.

  **Recommended Agent Profile**:
  - Category: `quick` - Documentation updates only, no code changes.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: none | Blocked By: none

  **References**:
  - Spec: `.specs/features/wex-tech-challenge/DECISIONS.md` — update UNRESOLVED items
  - Spec: `.specs/codebase/STRUCTURE.md` — fix package declaration
  - Context: `.specs/features/wex-tech-challenge/context.md` — source of unresolved items

  **Acceptance Criteria**:
  - [ ] `grep "UNRESOLVED" .specs/features/wex-tech-challenge/DECISIONS.md` — resolved items removed from UNRESOLVED
  - [ ] `grep "infra/adapter/persistence" .specs/codebase/STRUCTURE.md` — declaration matches actual package
  - [ ] `grep "targetCurrency" .specs/features/wex-tech-challenge/DECISIONS.md` — documented as required selector

  **QA Scenarios**:
  ```
  Scenario: Verify resolved items in DECISIONS.md
    Tool: Bash
    Steps: grep for "UNRESOLVED" and verify Treasury endpoint, 6-month, HTTP status are removed
    Expected: Each resolved item appears in resolved section, not in UNRESOLVED register
    Evidence: .sisyphus/evidence/task-T1-decisions-resolved.txt

  Scenario: Verify STRUCTURE.md alignment
    Tool: Bash
    Steps: grep "infra/adapter/persistence" on STRUCTURE.md
    Expected: Path matches actual package layout
    Evidence: .sisyphus/evidence/task-T1-structure-aligned.txt
  ```

  **Commit**: YES | Message: `docs(spec): resolve spec ambiguities and align STRUCTURE.md with actual layout` | Files: `.specs/features/wex-tech-challenge/DECISIONS.md`, `.specs/codebase/STRUCTURE.md`

- [ ] T2. DB Schema Migration DECIMAL(18,6) → DECIMAL(18,2) + Cent-Rounding

  **What to do**:
  - Edit `src/main/resources/db/migration/V1__initial_schema.sql`:
    - Change `transaction_amount DECIMAL(18,6)` → `DECIMAL(18,2)`
    - Change `exchange_rate DECIMAL(18,6)` → `DECIMAL(18,2)`
    - Change `converted_amount DECIMAL(18,6)` → `DECIMAL(18,2)`
  - Edit `src/main/kotlin/.../infra/adapter/persistence/PurchaseJpaEntity.kt`:
    - Change `@Column(precision = 18, scale = 6)` → `scale = 2` for all 3 monetary fields
  - Keep `ExchangeRate.SCALE = 2` (já está correto)
  - Keep `CONVERSION_SCALE = 2` em ambos use cases (já está correto)
  - Add purchase amount cent-rounding enforcement in `Purchase.kt`:
    - Add validation no `init` block: `require(transactionAmount.scale() <= 2)` ou `require(transactionAmount.stripTrailingZeros().scale() <= 2)`
  - Add cent-rounding in `StorePurchaseUseCaseImpl.kt`:
    - After validation: `command.transactionAmount.setScale(2, RoundingMode.HALF_UP)` before using
  - Fix currency validation message (item 47):
    - In `StorePurchaseRequest.kt`: change message `"Invalid currency code: INVALID"` to `"Invalid currency code"` (sem placeholder literal)

  **Must NOT do**: Não alterar lógica de conversão ou arredondamento além do especificado. Não alterar ExchangeRate.SCALE.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` - Schema + domain changes with clear spec.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: T7, T9 | Blocked By: none

  **References**:
  - Schema: `src/main/resources/db/migration/V1__initial_schema.sql` — editar precision DECIMAL(18,6)→(18,2)
  - Entity: `src/main/kotlin/.../infra/adapter/persistence/PurchaseJpaEntity.kt` — editar scale=6→2
  - Domain: `src/main/kotlin/.../domain/model/Purchase.kt:23` — adicionar scale validation
  - UseCase: `src/main/kotlin/.../application/service/purchase/StorePurchaseUseCaseImpl.kt` — forçar setScale(2) no amount
  - DTO: `src/main/kotlin/.../domain/port/inbound/purchase/model/StorePurchaseRequest.kt` — fix currency message

  **Acceptance Criteria**:
  - [ ] `grep "DECIMAL(18,2)" src/main/resources/db/migration/V1__initial_schema.sql` — 3 campos com 18,2
  - [ ] `grep "scale = 2" PurchaseJpaEntity.kt` — 3 campos com scale 2
  - [ ] `grep "Invalid currency code" StorePurchaseRequest.kt` — mensagem não contém "INVALID" literal
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify DB schema migrated
    Tool: Bash
    Steps: grep DECIMAL on V1__initial_schema.sql
    Expected: All 3 monetary fields use DECIMAL(18,2)
    Evidence: .sisyphus/evidence/task-T2-schema-migrated.txt

  Scenario: Verify cent-rounding enforced
    Tool: Bash
    Steps: Search for setScale or scale check in Purchase.kt and StorePurchaseUseCaseImpl.kt
    Expected: Both files have cent-rounding enforcement
    Evidence: .sisyphus/evidence/task-T2-cent-rounding.txt
  ```

  **Commit**: YES | Message: `fix(db): migrate DECIMAL(18,6) to DECIMAL(18,2) and enforce cent-rounding` | Files: `src/main/resources/db/migration/V1__initial_schema.sql`, `src/main/kotlin/.../infra/adapter/persistence/PurchaseJpaEntity.kt`, `src/main/kotlin/.../domain/model/Purchase.kt`, `src/main/kotlin/.../application/service/purchase/StorePurchaseUseCaseImpl.kt`, `src/main/kotlin/.../domain/port/inbound/purchase/model/StorePurchaseRequest.kt`

- [ ] T3. USD-Only Enforcement + Validation Unification

  **What to do**:
  - Item 1 (USD-only):
    - Em `StorePurchaseRequest.kt`: mudar `@Pattern(regexp = "^[A-Za-z]{3}$")` em `transactionCurrency` para `@Pattern(regexp = "^USD$", message = "Transaction currency must be USD")`
    - Em `StorePurchaseUseCaseImpl.kt`: após `TargetCurrency(command.transactionCurrency)`, adicionar `require(sourceCurrency.code == "USD") { "Only USD purchases are supported" }`
    - Em `PurchaseControllerV1.kt`: remover a validação manual duplicada (a Bean Validation + use case cuidam)
  - Item 26 (Unify validation):
    - Remover método `validate()` inteiro do `PurchaseControllerV1.kt` (a validação está no DTO via Bean Validation + no use case)
    - Remover imports não usados após a limpeza
  - Item 41 (Controller cleanup):
    - Controller não deve mais importar `Purchase`, `TransactionDate` ou `BigDecimal`
    - Controller deve apenas: receber request validado → chamar port → retornar response

  **Must NOT do**: Não remover validação do use case (require statements). Não remover validação de domínio (Purchase.kt init). A validação de `TransactionDate` no controller é substituída pela validação no use case.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` - Multiple coordinated file changes with clear spec.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: T5, T7, T11 | Blocked By: none

  **References**:
  - DTO: `src/main/kotlin/.../domain/port/inbound/purchase/model/StorePurchaseRequest.kt` — currency regex → USD-only
  - UseCase: `src/main/kotlin/.../application/service/purchase/StorePurchaseUseCaseImpl.kt` — add USD guard
  - Controller: `src/main/kotlin/.../infra/client/purchase/PurchaseControllerV1.kt` — remove validate() + clean imports
  - Evidence: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt` — existing tests should still pass (some may need update for new error message)

  **Acceptance Criteria**:
  - [ ] `grep "regexp" StorePurchaseRequest.kt` — transactionCurrency usa "^USD$"
  - [ ] `grep "validate\\(" PurchaseControllerV1.kt` — método removido
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify USD-only enforced in DTO
    Tool: Bash
    Steps: grep for transactionCurrency regex pattern in StorePurchaseRequest.kt
    Expected: @Pattern(regexp = "^USD$")
    Evidence: .sisyphus/evidence/task-T3-usd-dto.txt

  Scenario: Verify controller validate() removed
    Tool: Bash
    Steps: grep for "fun validate" in PurchaseControllerV1.kt
    Expected: No matches
    Evidence: .sisyphus/evidence/task-T3-controller-clean.txt
  ```

  **Commit**: YES | Message: `feat(purchase): enforce USD-only purchases and unify validation in Bean Validation` | Files: `src/main/kotlin/.../domain/port/inbound/purchase/model/StorePurchaseRequest.kt`, `src/main/kotlin/.../application/service/purchase/StorePurchaseUseCaseImpl.kt`, `src/main/kotlin/.../infra/client/purchase/PurchaseControllerV1.kt`

- [ ] T4. Fix Architecture Tests — Eliminate Vacuous Passes

  **What to do**:
  - Edit `src/test/kotlin/.../architecture/DependencyDirectionTest.kt`:
    - Replace `if (domainClasses.isEmpty()) return` → `assertThat(domainClasses).isNotEmpty()` (for each layer)
    - Remove `.allowEmptyShould(true)` de todas as regras
    - Fix all 3 test methods similarly
  - Edit `src/test/kotlin/.../architecture/ControllerBoundaryTest.kt`:
    - Replace early return `if (!infraClientExists) return` → `assertThat(infraClientExists).isTrue()`
  - Edit `src/test/kotlin/.../architecture/UseCaseOwnershipTest.kt`:
    - Replace `if (useCaseImpls.isEmpty()) return` → `assertThat(useCaseImpls).isNotEmpty()`
    - Replace `if (domainClasses.isEmpty()) return` e `if (useCaseInDomain.isEmpty()) return` → assertNotEmpty
    - Replace `if (infraClasses.isEmpty()) return` e `if (useCaseInInfra.isEmpty()) return` → assertNotEmpty
  - Edit `src/test/kotlin/.../architecture/VacuousGuardTest.kt`:
    - Replace document-only assertions with executable assertions on expected package sizes
  - Add `import org.assertj.core.api.Assertions.assertThat` onde necessário

  **Must NOT do**: Não alterar a lógica de produção. Não remover os testes — apenas fortalecer as asserções.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Requires understanding of ArchUnit patterns and assertion strengthening.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: none | Blocked By: none

  **References**:
  - Current: `src/test/kotlin/.../architecture/DependencyDirectionTest.kt:20-21` — early return pattern
  - Current: `src/test/kotlin/.../architecture/ControllerBoundaryTest.kt:19-27` — early return pattern
  - Current: `src/test/kotlin/.../architecture/UseCaseOwnershipTest.kt:20-23` — early return pattern
  - Current: `src/test/kotlin/.../architecture/VacuousGuardTest.kt:7-18` — doc-only assertion
  - Pattern: `ArchitectureTest.kt` — base class with importClasses() helper

  **Acceptance Criteria**:
  - [ ] `grep "if.*isEmpty().*return" src/test/kotlin/.../architecture/DependencyDirectionTest.kt` — no early returns
  - [ ] `grep "allowEmptyShould" src/test/kotlin/.../architecture/DependencyDirectionTest.kt` — no allowEmptyShould
  - [ ] `./gradlew test --tests "*architecture*"` passa

  **QA Scenarios**:
  ```
  Scenario: Verify no early returns in architecture tests
    Tool: Bash
    Steps: grep for early return pattern in all architecture test files
    Expected: No matches for isEmpty+return pattern
    Evidence: .sisyphus/evidence/task-T4-arch-fixed.txt
  ```

  **Commit**: YES | Message: `test(arch): eliminate vacuous passes in architecture tests` | Files: `src/test/kotlin/.../architecture/DependencyDirectionTest.kt`, `src/test/kotlin/.../architecture/ControllerBoundaryTest.kt`, `src/test/kotlin/.../architecture/UseCaseOwnershipTest.kt`, `src/test/kotlin/.../architecture/VacuousGuardTest.kt`

- [ ] T5. Exception Handling — Infra Exceptions + ProblemDetails + Sanitization

  **What to do**:
  - Item 20 (infra exceptions): No `GlobalExceptionHandler.kt`:
    - Adicionar handler para `FeignException`: mapear para 503 (Service Unavailable) com ProblemDetail
    - Adicionar handler para `IllegalStateException`: mapear para 422 (Unprocessable Entity) com ProblemDetail
    - Adicionar handler para `Exception` genérico: mapear para 500 com mensagem segura (não vazar stack trace)
  - Item 28 (multi-field errors): No `handleMethodArgumentNotValidException`:
    - Alterar para coletar TODOS os `fieldErrors` e incluir em extension property `errors` no ProblemDetail
    - Usar `fieldErrors.joinToString` ou structured list no body
  - Item 52 (sanitize error response):
    - No handler de `IllegalArgumentException`: usar mensagem genérica segura ao invés de ex.message quando contiver input do usuário
    - Em `TransactionDate.kt`: mudar a mensagem de erro para não incluir o input raw, ou mover raw input para log separado
  - Add tests in `GlobalExceptionHandlerTest.kt`:
    - Test for FeignException → 503
    - Test for IllegalStateException → 422
    - Test for multi-field validation → all errors in response
    - Test for safe error messages

  **Must NOT do**: Não quebrar handlers existentes para DomainException, PurchaseNotFoundException, RateUnavailableException. Não vazar stack traces.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` — Well-defined exception mapping, clear expected behavior.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: none | Blocked By: T2, T3

  **References**:
  - Handler: `src/main/kotlin/.../infra/client/error/GlobalExceptionHandler.kt` — adicionar handlers
  - Test: `src/test/kotlin/.../infra/client/error/GlobalExceptionHandlerTest.kt` — adicionar cenários
  - Domain: `src/main/kotlin/.../domain/model/TransactionDate.kt:39` — raw input in error message
  - Pattern: RFC 7807 ProblemDetails — usar `ProblemDetail.forStatusAndDetail()` + extension properties

  **Acceptance Criteria**:
  - [ ] `grep "FeignException" GlobalExceptionHandler.kt` — handler exists
  - [ ] `grep "IllegalStateException" GlobalExceptionHandler.kt` — handler exists
  - [ ] `grep "errors" ProblemDetail` ou equivalent — multi-field error handling
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify FeignException handler
    Tool: Bash
    Steps: Search GlobalExceptionHandler.kt for FeignException handling
    Expected: Handler exists mapping to 503 with ProblemDetail
    Evidence: .sisyphus/evidence/task-T5-feign-handler.txt

  Scenario: Verify multi-field validation
    Tool: Bash
    Steps: Search GlobalExceptionHandler.kt for fieldErrors collection
    Expected: Multiple field errors are collected, not just first
    Evidence: .sisyphus/evidence/task-T5-multi-field.txt
  ```

  **Commit**: YES | Message: `fix(api): improve exception handling with infra error mapping and multi-field ProblemDetails` | Files: `src/main/kotlin/.../infra/client/error/GlobalExceptionHandler.kt`, `src/main/kotlin/.../domain/model/TransactionDate.kt`, `src/test/kotlin/.../infra/client/error/GlobalExceptionHandlerTest.kt`

- [ ] T6. Observability — Event Logging + Safe Logging + Trace ID Sanitization

  **What to do**:
  - Item 9 (event failure logging — no retry+DLQ):
    - Em `RetrieveConvertedUseCaseImpl.kt` (método `publishToCache`): adicionar `logger.warn(...)` com traceId e detalhes do evento no `runCatching {}.onFailure {}`
    - Em `RedisExchangeRateEventAdapter.kt` (método `publish`): o logger.warn já existe, mas adicionar traceId e structured fields
  - Item 21 (safe argument logging):
    - Em `ApplicationFlowLoggingAspect.kt`: alterar `args` logging para mostrar apenas resumo seguro (args count + types, ou toString truncado)
    - Manter full args logging para DEBUG level
  - Item 22 (trace ID sanitization):
    - Em `TraceIdFilter.kt`: validar header `X-Trace-Id` recebido contra regex `^[a-zA-Z0-9\\-]{1,64}$` antes de aceitar
    - Se inválido, gerar novo traceId ao invés de aceitar o header
  - Add/update tests:
    - `ApplicationFlowLoggingAspectTest.kt` — verify safe args at INFO level
    - `TraceIdFilterTest.kt` — verify invalid trace ID is rejected and new one generated

  **Must NOT do**: Não adicionar retry/DLQ. Não remover logging existente — apenas ajustar níveis e conteúdo. Não quebrar trace correlation existente.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Multiple file changes across observability concerns.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: none | Blocked By: none

  **References**:
  - Event: `src/main/kotlin/.../application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt:74-85` — publishToCache failure
  - Event: `src/main/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapter.kt` — existing warn log
  - Logging: `src/main/kotlin/.../infra/logging/ApplicationFlowLoggingAspect.kt:29-33` — full args
  - Trace: `src/main/kotlin/.../infra/logging/TraceIdFilter.kt:38-40` — unsanitized header
  - Test: `src/test/kotlin/.../infra/logging/ApplicationFlowLoggingAspectTest.kt`
  - Test: `src/test/kotlin/.../infra/logging/TraceIdFilterTest.kt`

  **Acceptance Criteria**:
  - [ ] `grep "logger.warn\\|logger.error" RetrieveConvertedUseCaseImpl.kt` — logging added on publish failure
  - [ ] `grep "args=" ApplicationFlowLoggingAspect.kt` — não loga args completos em INFO
  - [ ] `grep "TRACE_ID_HEADER\\|X-Trace-Id" TraceIdFilter.kt` — has validation before accepting
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify trace ID sanitization
    Tool: Bash
    Steps: Search TraceIdFilter.kt for validation pattern
    Expected: External trace ID header is validated before acceptance
    Evidence: .sisyphus/evidence/task-T6-trace-validated.txt

  Scenario: Verify safe logging
    Tool: Bash
    Steps: Read ApplicationFlowLoggingAspect.kt args logging
    Expected: Full args only at DEBUG, summary at INFO
    Evidence: .sisyphus/evidence/task-T6-safe-logging.txt
  ```

  **Commit**: YES | Message: `fix(obs): improve observability with safe logging, trace sanitization, and event failure logging` | Files: `src/main/kotlin/.../application/service/retrieveConverted/RetrieveConvertedUseCaseImpl.kt`, `src/main/kotlin/.../infra/adapter/event/RedisExchangeRateEventAdapter.kt`, `src/main/kotlin/.../infra/logging/ApplicationFlowLoggingAspect.kt`, `src/main/kotlin/.../infra/logging/TraceIdFilter.kt`, relevant test files

- [ ] T7. Resilience4j — CircuitBreaker + Bulkhead + RateLimiter

  **What to do**:
  - Add Resilience4j dependency to `build.gradle.kts`:
    - `implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")`
    - `implementation("io.github.resilience4j:resilience4j-feign")`
  - Item 23 (CircuitBreaker + Bulkhead):
    - Add `@CircuitBreaker(name = "treasury-rates", fallbackMethod = "fallback")` no método do use case ou adapter
    - Add `@Bulkhead(name = "treasury-rates", type = Bulkhead.Type.SEMAPHORE, maxConcurrentCalls = 5)`
    - Configurar no `application.yaml`:
      ```yaml
      resilience4j.circuitbreaker:
        instances:
          treasury-rates:
            sliding-window-size: 10
            minimum-number-of-calls: 5
            failure-rate-threshold: 50
            wait-duration-in-open-state: 30s
      resilience4j.bulkhead:
        instances:
          treasury-rates:
            max-concurrent-calls: 5
            max-wait-duration: 500ms
      ```
  - Item 14 (RateLimiter):
    - Add `@RateLimiter(name = "api-purchases")` no controller ou use case
    - Configurar no `application.yaml`:
      ```yaml
      resilience4j.ratelimiter:
        instances:
          api-purchases:
            limit-for-period: 100
            limit-refresh-period: 1s
            timeout-duration: 0
      ```
  - Add tests:
    - Test circuit breaker opens after failures
    - Test bulkhead rejects when max concurrent reached
    - Test rate limiter rejects above threshold

  **Must NOT do**: Não remover retry existente no `TreasuryFeignConfig.kt` (keep retry + add CB). Não alterar timeout Feign config.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Library integration with config and code changes. Needs skills: clean-ddd-hexagonal.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: T11, T13 | Blocked By: none

  **References**:
  - Config: `build.gradle.kts:42-52` — add resilience4j dependencies
  - Config: `src/main/resources/application.yaml` — add CB/BH/RL config
  - Adapter: `src/main/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt` — add @CircuitBreaker @Bulkhead
  - Controller: `src/main/kotlin/.../infra/client/purchase/PurchaseControllerV1.kt` — add @RateLimiter

  **Acceptance Criteria**:
  - [ ] `grep "resilience4j" build.gradle.kts` — dependencies added
  - [ ] `grep "circuitbreaker" application.yaml` — CB config present
  - [ ] `grep "ratelimiter" application.yaml` — RL config present
  - [ ] `grep "CircuitBreaker" ExchangeRateTreasuryAdapter.kt` — annotation present
  - [ ] `./gradlew test` passa (dependências resolvidas)

  **QA Scenarios**:
  ```
  Scenario: Verify Resilience4j dependencies
    Tool: Bash
    Steps: grep for resilience4j in build.gradle.kts
    Expected: circuitbreaker and resilience4j-feign dependencies present
    Evidence: .sisyphus/evidence/task-T7-resilience-deps.txt

  Scenario: Verify configuration
    Tool: Bash
    Steps: grep for resilience4j section in application.yaml
    Expected: circuitbreaker + bulkhead + ratelimiter configs present
    Evidence: .sisyphus/evidence/task-T7-resilience-config.txt
  ```

  **Commit**: YES | Message: `feat(resilience): add Resilience4j CircuitBreaker, Bulkhead, and RateLimiter` | Files: `build.gradle.kts`, `src/main/resources/application.yaml`, `src/main/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt`, `src/main/kotlin/.../infra/client/purchase/PurchaseControllerV1.kt`

- [ ] T8. Treasury Robustness + DTO Naming Consistency

  **What to do**:
  - Item 24 (Treasury parsing):
    - Em `TreasuryExchangeRateResponse.kt`:
      - `isAvailable()`: mudar para `value.trim().let { it != "null" && it.isNotBlank() && it != "-" && it != "N/A" }` (case-insensitive null check)
      - `rate`: mudar de `exchangeRate.toBigDecimal()` para `exchangeRate.toBigDecimalOrNull()` com fallback
      - Adicionar `runCatching` no acesso ao `rate` para evitar NumberFormatException
    - Em `ExchangeRateTreasuryAdapter.kt`:
      - Após `hasValidExchangeRate`, adicionar verificação extra se o rate é parseável com `runCatching`
  - Item 27 (DTO naming consistency):
    - Em `StorePurchaseResponse.kt`: verificar se `exchangeRate` vs `RetrieveConvertedResponse.exchangeRateUsed` — documentar diferença ou renomear para consistência
    - Decisão: renomear `StorePurchaseResponse.exchangeRate` → manter como está (é o rate usado na criação). Adicionar comentário/nota explicando diferença.
    - Se o campo `transactionAmount` no response do create é o mesmo que `originalUsdAmount` no retrieve, considerar alias se aplicável.
  - Add tests:
    - `TreasuryExchangeRateResponseTest.kt` — test `isAvailable` with "NULL", " null ", "-", "N/A", empty, valid values
    - `ExchangeRateTreasuryAdapterTest.kt` — add test for malformed exchange_rate value

  **Must NOT do**: Não alterar o comportamento de fallback quando o rate é válido. Não quebrar parsing de rates válidos.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` — Well-scoped parsing improvements with clear test cases.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: T11 | Blocked By: T2

  **References**:
  - Response: `src/main/kotlin/.../infra/adapter/external/treasury/TreasuryExchangeRateResponse.kt:37-43` — isAvailable + rate
  - Adapter: `src/main/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt:38-40` — guard usage
  - DTOs: `StorePurchaseResponse.kt`, `RetrieveConvertedResponse.kt` — naming comparison
  - Test: `src/test/kotlin/.../infra/adapter/external/treasury/TreasuryRateRecordTest.kt` — existing tests

  **Acceptance Criteria**:
  - [ ] `grep "toBigDecimalOrNull" TreasuryExchangeRateResponse.kt` — safer parsing
  - [ ] `grep "caseInsensitive\|lowercase\|trim" TreasuryExchangeRateResponse.kt` — null check case-insensitive
  - [ ] `grep "exchangeRate\|originalUsdAmount"` nos DTO response — naming documented
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify robust parsing
    Tool: Bash
    Steps: Search TreasuryExchangeRateResponse.kt for toBigDecimalOrNull
    Expected: Rate parsing uses toBigDecimalOrNull with fallback
    Evidence: .sisyphus/evidence/task-T8-robust-parsing.txt
  ```

  **Commit**: YES | Message: `fix(treasury): robust parsing with case-insensitive null check and safe BigDecimal conversion` | Files: `src/main/kotlin/.../infra/adapter/external/treasury/TreasuryExchangeRateResponse.kt`, `src/main/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapter.kt`, response DTOs

- [ ] T9. Persistence Toggle Startup Guard

  **What to do**:
  - Item 44 (startup guard):
    - Em `PersistenceConfig.kt`: adicionar `@ConditionalOnProperty` com `matchIfMissing = true` (já existe)
    - Adicionar uma validação em `Application.kt` ou em `PersistenceConfig.kt` que verifica se `app.persistence.enabled` é `false` em perfil diferente de `test` e bloqueia startup com mensagem clara
    - Ou adicionar um `@PostConstruct` em `PersistenceConfig` que verifica o profile ativo quando persistence está disabled

  **Must NOT do**: Não alterar o comportamento default (enabled=true). Não quebrar testes existentes.

  **Recommended Agent Profile**:
  - Category: `quick` — Single file, simple guard condition.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: none | Blocked By: none

  **References**:
  - Config: `src/main/kotlin/.../config/PersistenceConfig.kt` — add startup guard

  **Acceptance Criteria**:
  - [ ] `grep "enabled\|guard\|fail" PersistenceConfig.kt` — startup validation exists
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify persistence guard
    Tool: Bash
    Steps: Read PersistenceConfig.kt for startup validation
    Expected: Guard prevents boot when persistence disabled outside test profile
    Evidence: .sisyphus/evidence/task-T9-persistence-guard.txt
  ```

  **Commit**: YES | Message: `fix(config): add startup guard for persistence toggle outside test profile` | Files: `src/main/kotlin/.../config/PersistenceConfig.kt`

- [ ] T10. WireMock Setup + StorePurchase Use-Case Tests

  **What to do**:
  - Item 31 (WireMock):
    - Add WireMock dependency to `build.gradle.kts`: `testImplementation("org.wiremock:wiremock:3.12.1")`
    - Create `src/test/kotlin/.../config/WireMockConfig.kt`:
      - TestConfiguration com bean WireMockServer
      - Integrar com Testcontainers / DynamicPropertySource
      - Substituir `@MockitoBean ExchangeRateClientPort` nos API tests por WireMock stub
    - Atualizar `PurchaseControllerV1Test.kt`:
      - Remover `StubExchangeRateClientConfig`
      - Usar WireMock para stubar resposta do Treasury
    - Atualizar `RetrieveConvertedControllerV1IntegrationTest.kt`:
      - Remover `@MockitoBean exchangeRateClientPort`
      - Usar WireMock para stubar respostas do Treasury
  - Item 13 (StorePurchase use-case tests):
    - Em `StorePurchaseUseCaseImplTest.kt`:
      - Adicionar teste de sucesso: verificar que `purchaseRepositoryPort.save()` é chamado com Purchase correto
      - Adicionar teste que verifica chamada a `exchangeRateClientPort.fetchRate()`
      - Adicionar teste que verifica cálculo de `convertedAmount`
      - Adicionar teste com `transactionCurrency != "USD"` que verifica falha (após item 1)
    - Criar cenários de teste com Mockito `verify` para port calls

  **Must NOT do**: Não remover Testcontainers — WireMock é adicional. Não alterar produção.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Test infrastructure setup + new test scenarios.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: T13 | Blocked By: T3, T5, T7

  **References**:
  - Current: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt` — replace mocks
  - Current: `src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt` — replace mocks
  - Current: `src/test/kotlin/.../application/service/purchase/StorePurchaseUseCaseImplTest.kt` — add success tests
  - Config: `src/test/kotlin/.../config/AbstractRestApiIntegrationTest.kt` — test base
  - WireMock docs: WireMock 3.x JUnit5 extension with Spring Boot

  **Acceptance Criteria**:
  - [ ] `grep "wiremock" build.gradle.kts` — dependency present
  - [ ] `grep "WireMockServer\\|WireMockExtension" src/test/` — WireMock config exists
  - [ ] `grep "@MockitoBean" RetrieveConvertedControllerV1IntegrationTest.kt` — no MockitoBeans for ports
  - [ ] `StorePurchaseUseCaseImplTest.kt` has success test with port call verification
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify WireMock dependency
    Tool: Bash
    Steps: grep wiremock in build.gradle.kts
    Expected: testImplementation("org.wiremock:wiremock:...")
    Evidence: .sisyphus/evidence/task-T10-wiremock-deps.txt

  Scenario: Verify mock-free API tests
    Tool: Bash
    Steps: grep @MockitoBean in RetrieveConvertedControllerV1IntegrationTest.kt
    Expected: No matches for port-related MockitoBeans
    Evidence: .sisyphus/evidence/task-T10-no-mocks.txt
  ```

  **Commit**: YES | Message: `test: add WireMock test infrastructure and StorePurchase use-case success tests` | Files: `build.gradle.kts`, `src/test/kotlin/.../config/WireMockConfig.kt`, `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt`, `src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt`, `src/test/kotlin/.../application/service/purchase/StorePurchaseUseCaseImplTest.kt`

- [ ] T11. Treasury Failure Matrix + 6-Month Boundary Tests

  **What to do**:
  - Item 11 (Treasury failure matrix):
    - Em `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterHttpIntegrationTest.kt`:
      - Adicionar teste: timeout/socket failure → retry behavior
      - Adicionar teste: 429 Too Many Requests → RetryableException
      - Adicionar teste: 503 Service Unavailable → RetryableException
      - Adicionar teste: 500 Internal Server Error → FeignException (não retry)
    - Em `src/test/kotlin/.../infra/adapter/external/treasury/TreasuryFeignConfigTest.kt`:
      - Adicionar teste para 504 Gateway Timeout (já existe no decoder, testar explicitamente)
    - Em `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterTest.kt`:
      - Adicionar teste: malformed exchange_rate value ("", "-", "N/A", texto)
      - Adicionar teste: malformed record_date
      - Adicionar teste: múltiplos records, verificar que o mais recente (sorted) é usado
      - Adicionar teste: descrição que não corresponde a nenhuma moeda
  - Item 50 (6-month boundary):
    - Em `ExchangeRateTreasuryAdapterTest.kt`:
      - Adicionar teste: `rateDate = purchaseDate.minusMonths(6).minusDays(1)` → retorna null (fora da janela)

  **Must NOT do**: Não alterar lógica de produção. Apenas adicionar testes.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` — Test-only additions with clear scenarios.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: none | Blocked By: T8

  **References**:
  - Test: `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterHttpIntegrationTest.kt` — add failure scenarios
  - Test: `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterTest.kt` — add malformed + boundary tests
  - Test: `src/test/kotlin/.../infra/adapter/external/treasury/TreasuryFeignConfigTest.kt` — add 504 test
  - Production: `ExchangeRateTreasuryAdapter.kt:60` — PAGE_SIZE = 10_000
  - Production: `ExchangeRateTreasuryAdapter.kt:27` — minusMonths(6)

  **Acceptance Criteria**:
  - [ ] `ExchangeRateTreasuryAdapterHttpIntegrationTest.kt` has 429, 503, 504, 500 tests
  - [ ] `ExchangeRateTreasuryAdapterTest.kt` has malformed rate + boundary tests
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify 6-month outside window test
    Tool: Bash
    Steps: Search ExchangeRateTreasuryAdapterTest.kt for minusDays(1)
    Expected: Test with rateDate = purchaseDate.minusMonths(6).minusDays(1) asserts null
    Evidence: .sisyphus/evidence/task-T11-six-month-window.txt
  ```

  **Commit**: YES | Message: `test(treasury): add failure matrix, malformed data, and 6-month boundary tests` | Files: `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterHttpIntegrationTest.kt`, `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterTest.kt`, `src/test/kotlin/.../infra/adapter/external/treasury/TreasuryFeignConfigTest.kt`

- [ ] T12. Idempotency + Rate-Unavailable Integration Tests

  **What to do**:
  - Item 12 (idempotency/duplicate):
    - Em `PurchaseControllerV1Test.kt` ou novo arquivo:
      - Adicionar teste: POST idêntico duas vezes → verificar comportamento (atualmente cria duplicata, documentar)
      - Adicionar teste: POST com mesmo payload após sucesso → verificar resposta
    - Documentar em DECISIONS.md que idempotency não é implementada (comportamento atual)
  - Item 30 (rate-unavailable 422):
    - Em `RetrieveConvertedControllerV1IntegrationTest.kt`:
      - Adicionar teste: purchase existe, mas rate não disponível → 422 + ProblemDetail
    - Configurar WireMock para retornar empty data do Treasury quando necessário
    - Ou usar `@MockitoBean` existente até WireMock estar pronto (T10)

  **Must NOT do**: Não implementar idempotency — apenas testar comportamento atual e documentar.

  **Recommended Agent Profile**:
  - Category: `unspecified-low` — Test-only additions, no production changes.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: none | Blocked By: T3

  **References**:
  - Test: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt` — add duplicate test
  - Test: `src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt` — add 422 test
  - Production: `RetrieveConvertedUseCaseImpl.kt:40` — RateUnavailableException
  - Handler: `GlobalExceptionHandler.kt` — 422 mapping

  **Acceptance Criteria**:
  - [ ] Duplicate POST test exists
  - [ ] 422 integration test for rate-unavailable exists
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify duplicate POST test
    Tool: Bash
    Steps: Search PurchaseControllerV1Test.kt for duplicate submission scenario
    Expected: Test with identical second POST exists
    Evidence: .sisyphus/evidence/task-T12-duplicate-test.txt
  ```

  **Commit**: YES | Message: `test(api): add duplicate submission and rate-unavailable 422 integration tests` | Files: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt`, `src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt`, `.specs/features/wex-tech-challenge/DECISIONS.md`

- [ ] T13. API Test Mock Replacement (WireMock)

  **What to do**:
  - Completar substituição dos mocks `@MockitoBean` nos API tests por WireMock (se não concluído em T10)
  - Verificar todos os testes em `PurchaseControllerV1Test.kt` e `RetrieveConvertedControllerV1IntegrationTest.kt`:
    - Remover `StubExchangeRateClientConfig`
    - Remover `@MockitoBean ExchangeRateClientPort`
    - Configurar WireMock stub para cada cenário
  - Garantir que `ExchangeRateTreasuryAdapterHttpIntegrationTest.kt` ainda usa MockWebServer (mantido separado)
  - Remover imports não usados após refatoração

  **Must NOT do**: Não quebrar testes existentes durante a migração. Não remover `ExchangeRateTreasuryAdapterHttpIntegrationTest.kt`.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Test refactoring across multiple API test files.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: none | Blocked By: T6, T10

  **References**:
  - Current: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt:21` — StubExchangeRateClientConfig
  - Current: `src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt:35` — @MockitoBean
  - WireMock: `src/test/kotlin/.../config/WireMockConfig.kt` — created in T10
  - MockWebServer: `src/test/kotlin/.../infra/adapter/external/treasury/ExchangeRateTreasuryAdapterHttpIntegrationTest.kt` — keep as-is

  **Acceptance Criteria**:
  - [ ] `grep \"@MockitoBean\" RetrieveConvertedControllerV1IntegrationTest.kt` — no matches
  - [ ] `grep \"StubExchangeRateClientConfig\" PurchaseControllerV1Test.kt` — no matches
  - [ ] `grep \"WireMock\" RetrieveConvertedControllerV1IntegrationTest.kt` — WireMock used instead
  - [ ] `./gradlew test` passa

  **QA Scenarios**:
  ```
  Scenario: Verify no more @MockitoBean for ExchangeRateClient
    Tool: Bash
    Steps: grep @MockitoBean in both API test files
    Expected: No @MockitoBean for ExchangeRateClientPort
    Evidence: .sisyphus/evidence/task-T13-mock-free.txt
  ```

  **Commit**: YES | Message: `test(api): replace @MockitoBean mocks with WireMock stubs in API integration tests` | Files: `src/test/kotlin/.../infra/client/purchase/PurchaseControllerV1Test.kt`, `src/test/kotlin/.../infra/client/retrieveConverted/RetrieveConvertedControllerV1IntegrationTest.kt`

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Cada task faz commit atômico com mensagem conventional commit
- Ordem dos commits segue ordem das tasks (T1 → T13)
- Nenhum commit quebra o build (`./gradlew test` deve passar em cada commit)
- Branch: `fix/code-review-fixes-2026-05-25`

## Success Criteria
- [ ] `./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck` — sem erros
- [ ] `./gradlew detekt` — sem erros
- [ ] `./gradlew test jacocoTestReport` — todos verdes
- [ ] JaCoCo coverage >= 90% overall, 100% domain
- [ ] Nenhum teste arquitetural passa com escopo vazio
- [ ] USD-only enforcement ativo em store flow
- [ ] DB schema DECIMAL(18,2) consistente com domínio scale-2
- [ ] Resilience4j (CB + Bulkhead + RL) integrado e testado
- [ ] WireMock substitui mocks nos API tests
- [ ] Spec ambiguities resolvidas em DECISIONS.md
