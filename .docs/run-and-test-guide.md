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

Expected result: application, PostgreSQL, and Redis services start.

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

4. Verify Redis readiness logs:

```bash
docker compose logs redis
```

Look for readiness markers indicating Redis is accepting connections.

Optional live Treasury smoke test (not part of default CI):

```bash
RUN_LIVE_TREASURY_TESTS=true ./gradlew test --tests "com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.TreasuryLiveSmokeTest"
```

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
    "transactionCurrency": "United-States-Dollar",
    "transactionDate": "2025-05-22T12:00:00Z"
  }'
```

Expected response (HTTP 201):
```json
{
  "id": 1,
  "description": "Laptop charger",
  "transactionAmount": 49.99,
  "transactionCurrency": "United-States-Dollar",
  "transactionDate": "2025-05-22T12:00:00Z",
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
  "transactionAmount": 49.99,
  "transactionCurrency": "United-States-Dollar",
  "exchangeRateUsed": 0.92,
  "convertedAmount": 45.99,
  "targetCurrency": "Brazil-Real",
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
1. Check whether port `8080, 5432 or 6379` is already in use.
2. Stop the conflicting process/service.
3. Start stack again.

### Stale volume

Symptoms: persistent data/state causes unexpected behavior.

Action:

```bash
docker compose down -v
```

Then start the stack again.

### Flyway checksum mismatch after migration edits

Symptoms: app fails on startup with `Migration checksum mismatch for migration version N`.

Cause: a Flyway migration file changed after it was already applied to a persistent database volume.

Actions (pick one):

1. **Local/dev reset (destructive):**
   ```bash
   docker compose down -v
   docker compose up -d
   ```
2. **Repair existing schema history (keeps data):**
   ```bash
   docker compose exec app java -jar /app/app.jar --spring.flyway.repair=true
   ```
   Or run Flyway repair against the same JDBC URL/credentials used by the app.
3. **Production:** never edit applied migrations; add a new `V2__...sql` migration instead.

### Docker daemon not running

Symptoms: Docker commands fail to connect to daemon.

Action: start Docker Desktop, then rerun stack commands.

### Redis connection failure

Symptoms: app cannot connect to Redis, cache operations fail.

Actions:
1. Verify Redis container running: `docker compose ps | grep redis`
2. Check Redis logs: `docker compose logs redis`
3. Verify port: `docker compose port redis 6379` returns `0.0.0.0:6379`
4. Test ping: `docker compose exec redis redis-cli ping` returns `PONG`
5. Restart: `docker compose restart redis`

## 9) Reference

Environment variables used by the application:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/wex_transactions` | PostgreSQL JDBC URL used by Spring datasource. |
| `DB_USERNAME` | `postgres` | Database username for datasource authentication. |
| `DB_PASSWORD` | `postgres` | Database password for datasource authentication. |
| `REDIS_HOST` | `redis` | Redis service hostname (Docker network name). |
| `REDIS_PORT` | `6379` | Redis service port. |
