# STRUCTURE

Current top-level:
- `src/main/kotlin/com/charlesluxinger/wex_transactions/Application.kt`
- test architecture scaffold in `src/test/kotlin/.../architecture/**`

Required target structure:
- `domain/**`
- `application/service/**`
- `infra/client/**`
- `infra/adapter/persistence/**`
- `infra/adapter/cache/**`
- `infra/adapter/event/**`
- `infra/adapter/external/**`
- `infra/logging/**`
- `domain/port/inbound/**`
- `domain/port/outbound/**`