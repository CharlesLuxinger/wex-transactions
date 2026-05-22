# CONVENTIONS

Naming:
- Controllers: `<Feature>ControllerV1`
- Inbound ports: `<Feature>CommandPort` / `<Feature>QueryPort`
- Outbound ports: `<Feature>RepositoryPort` / `<Feature>ClientPort`
- Adapters: `<Feature><Tech>Adapter` (never `*PortImpl`)
- Use cases: `<UseCase>Impl`

Code style:
- ktlint_official
- 4-space indent
- max 120 chars
- package-name rule disabled

Testing naming:
- `*Test.kt` for unit
- `*IT.kt` for integration