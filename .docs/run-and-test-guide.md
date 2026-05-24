# Run and Test Guide

Focused operator runbook for local build, stack startup, verification, and teardown.

## 1) Prerequisites

Ensure these are installed and available in your shell:

- Java 25
- Docker
- Docker Compose V2

## 2) Build the Image

From repository root, build the application image:

```bash
docker build -t wex-transactions:test .
```

Expected result: image `wex-transactions:test` is created successfully.

## 3) Start the Stack

Start services in detached mode:

```bash
docker compose up -d
```

Expected result: application and PostgreSQL services start.

## 4) Inspect Services

Run these checks in order.

1. Confirm both services are running:

```bash
docker compose ps
```

2. Verify Spring Boot startup logs:

```bash
docker compose logs app
```

Look for successful application startup markers (for example, startup completion and no fatal errors).

3. Verify PostgreSQL readiness logs:

```bash
docker compose logs postgres
```

Look for readiness markers indicating the database is accepting connections.

## 5) Run Verification

Run local quality and test commands (in this order):

```bash
./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat
./gradlew ktlintMainSourceSetCheck ktlintTestSourceSetCheck
./gradlew detekt
./gradlew test
```

Expected result: all commands complete without failures.

## 6) Teardown

Stop stack and remove volumes:

```bash
docker compose down -v
```

Expected result: services stop and local volumes are removed.

## 7) API Endpoints

### Store a Purchase
```bash
curl -X POST http://localhost:8080/api/v1/purchases \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Laptop charger",
    "transactionAmount": 49.99,
    "transactionCurrency": "USD",
    "transactionDate": "2025-05-22T12:00:00",
    "targetCurrency": "BRL"
  }'
```

Expected response (HTTP 201):
```json
{
  "id": 1,
  "description": "Laptop charger",
  "transactionAmount": 49.99,
  "transactionCurrency": "USD",
  "transactionDate": "2025-05-22T12:00:00Z",
  "targetCurrency": "BRL",
  "exchangeRate": 5.1234,
  "convertedAmount": 256.07,
  "createdAt": "2025-05-22T10:30:00Z"
}
```

### Retrieve Converted Purchase
```bash
curl "http://localhost:8080/api/v1/purchases/1/converted?targetCurrency=EUR"
```

Expected response (HTTP 200):
```json
{
  "purchaseId": 1,
  "description": "Laptop charger",
  "transactionDate": "2025-05-22T12:00:00Z",
  "originalUsdAmount": 49.99,
  "exchangeRateUsed": 0.92,
  "convertedAmount": 45.99,
  "targetCurrency": "EUR",
  "createdAt": "2025-05-22T10:30:00Z"
}
```

## 8) Failure Playbook

### Wrong DB credentials

Symptoms: app cannot connect to database.

Actions:
1. Check `DB_PASSWORD` environment variable value.
2. Set the correct value.
3. Restart stack with corrected configuration.

### Port conflict

Symptoms: startup fails because port binding is denied.

Actions:
1. Check whether port `8080` or `5432` is already in use.
2. Stop the conflicting process/service.
3. Start stack again.

### Stale volume

Symptoms: persistent data/state causes unexpected behavior.

Action:

```bash
docker compose down -v
```

Then start the stack again.

### Docker daemon not running

Symptoms: Docker commands fail to connect to daemon.

Action: start Docker Desktop, then rerun stack commands.

## 9) Reference

Environment variables used by the application:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/wex_transactions` | PostgreSQL JDBC URL used by Spring datasource. |
| `DB_USERNAME` | `postgres` | Database username for datasource authentication. |
| `DB_PASSWORD` | `postgres` | Database password for datasource authentication. |
