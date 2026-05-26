[![CI](https://github.com/CharlesLuxinger/wex-transactions/actions/workflows/ci.yml/badge.svg)](https://github.com/CharlesLuxinger/wex-transactions/actions/workflows/ci.yml)
[![Coverage](https://github.com/CharlesLuxinger/wex-transactions/blob/main/badges/jacoco.svg)](https://github.com/CharlesLuxinger/wex-transactions/actions/workflows/ci.yml)
[![Branches](https://github.com/CharlesLuxinger/wex-transactions/blob/main/badges/branches.svg)](https://github.com/CharlesLuxinger/wex-transactions/actions/workflows/ci.yml)

# WEX - Transactions

## Project Overview
REST API for currency-exchange transactions using Spring Boot + Kotlin.
It provides transactional flows backed by PostgreSQL with quality gates for formatting, static analysis, and coverage.

## Prerequisites
- Java 25
- Docker 24+
- Docker Compose V2

## Quick Start with Docker
```bash
docker build -t wex-transactions:test .
docker compose up -d
docker compose ps
docker compose logs app
docker compose down -v
```

> For a detailed step-by-step runbook, including service inspection, test execution, and failure recovery, see [docs/run-and-test-guide.md](.docs/run-and-test-guide.md).

## Local Development Without Docker
1. Clone the repository:
   ```bash
   git clone https://github.com/CharlesLuxinger/wex-transactions.git
   cd wex-transactions
   ```
2. Format code:
   ```bash
   ./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
   ```
3. Check formatting:
   ```bash
   ./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
   ```
4. Run static analysis:
   ```bash
   ./gradlew detekt
   ```
5. Run tests:
   ```bash
   ./gradlew test
   ```

## Verification Commands
### CI Verification
```bash
./gradlew --no-daemon ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew --no-daemon detekt
./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification
```

### Local Verification
```bash
./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew detekt
./gradlew test
```

## Troubleshooting
- **Port conflicts (8080/5432):** Stop the process using the port, then rerun `docker compose up -d`.
- **Stale volumes/state issues:** Run `docker compose down -v` and start again.
- **Credential issues:** Check `DB_USERNAME` and `DB_PASSWORD` values, then restart services.

## Cleanup / Teardown
- Stop containers and remove volumes:
  ```bash
  docker compose down -v
  ```
- Optionally remove local image:
  ```bash
  docker image rm wex-transactions:test
  ```
