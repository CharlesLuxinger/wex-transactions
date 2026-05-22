# ARCHITECTURE

Target architecture is Hexagonal + DDD per `AGENTS.md`.

Rules:
- Dependency direction: `infra -> application -> domain`
- Domain is framework-agnostic
- Controllers call inbound ports/use cases only
- UseCase implementations belong to `application/service/**`

Current state:
- Only `Application.kt` exists in production code.
- Architecture tests already exist to enforce governance when code is added.