# BancoLuso, S.A. — Real-Time Payment Status API

A Spring Boot service that replaces the payments nightly batch reconciliation job with a real-time REST API.  
Processes payment events from the external payments platform and exposes live payment status to branch operators.
---
## How to Run

### Prerequisites

| Tool | Version |
|------|---------|
| Docker | 20+     |
| Docker Compose | 2+      |

### Start the application
```bash
mkdir newfolder (creates a new folder with name (newfolder). you can choose any other name)
cd newfolder
git clone https://github.com/michael-iskander-java/real-time-payment-atatus-api.git
cd real-time-payment-atatus-api
git checkout master
docker compose up --build
```

The API will be available at **http://localhost:8080**. 
The first startup takes a couple of minutes while Maven downloads dependencies inside the build container.

### Verify the service is up

```bash
curl http://localhost:8080/actuator/health
```

### Example requests

**Ingest a payment event:**
```bash
curl.exe -s -X POST "http://localhost:8080/v1/payments/events" -H "Content-Type: application/json" -d "{\"referenceId\":\"TXN-2026-0001234\",\"amount\":1500.00,\"currency\":\"EUR\",\"debtorName\":\"João Silva\",\"debtorIban\":\"PT50000201231234567890154\",\"creditorIban\":\"PT50000201239876543210154\",\"valueDate\":\"2026-04-14\",\"status\":\"PENDING\",\"eventTimestamp\":\"2026-04-14T09:23:00Z\"}"
```

**Query payment status:**
```bash
curl.exe -s "http://localhost:8080/v1/payments/TXN-2026-0001234/status"
```

**list payments with pagination:**
```bash
curl.exe -s "http://localhost:8080/v1/payments?page=0&size=20"
```

## Architecture Decisions

### 1. Kafka as ingest buffer (KRaft mode — no Zookeeper)

The `POST /v1/payments/events` endpoint does not write to the database directly. Instead of it validates the request and publishes it to the `payments.events` Kafka topic. A `PaymentEventConsumer` picks up the message and calls the existing `PaymentService.ingestEvent()`.

Kafka runs in **KRaft mode** (Kafka 3.7), which removes Zookeeper dependency entirely for fewer containers and simpler operations on production.

Benefits of the async ingest path:
- The HTTP response (202) is returned as soon as Kafka acknowledges the message, not after the DB write. It makes lower latency for the payments platform.
- Broker absorbs spikes. If the database is slow, messages queue in Kafka rather than backing up HTTP threads.
- Consumer can scale independently without affecting the API.

### 2. Message key = referenceId

The producer sets the Kafka message key to `referenceId`. Kafka guarantees that all messages with the same key land on the same partition, so per-payment ordering is preserved end-to-end.

### 3. Retry + Dead-Letter Topic

The consumer is configured with a `DefaultErrorHandler` (wait 1 second between retries and maximum 3 retries). Records that exhaust retries are forwarded to `payments.events.DLT`.

### 4. Idempotency via database UNIQUE constraint and application

Payment uniqueness is enforced at two levels:

- **Application layer** (`PaymentService`): a `SELECT` by `referenceId` drives the three-way branch (new / update / ignore) before any write.
- **Database layer** (`UNIQUE` constraint on `reference_id`): the constraint acts as a safety net for concurrent requests that both pass the application-level check simultaneously. 
A constraint violation on insert means another thread won the race; the caller can safely retry.

### 5. Immutable financial fields

The `Payment` entity exposes `setStatus` and `setEventTimestamp` via Lombok's `@Setter`, but **not** the financial fields (`amount`, `currency`, `debtorIban`, `creditorIban`,`debtorName`, `valueDate`). 
Those fields are set only through the `Payment.create(...)` factory method and have no setters.

### 6. Flyway for schema management

Schema changes are managed by Flyway versioned migration scripts, giving reproducible and auditable schema history safe for production upgrades.

### 7. Creating an index on `referenceId`

This keeps status endpoint queries highly efficient through direct indexed lookups.

### 8. Testcontainers for integration tests

All integration tests use Testcontainers: real PostgreSQL + real Kafka (KRaft via `ConfluentKafkaContainer`). Awaitility handles assertions on the async consumer path cleanly.

---

## What I Would Improve With More Time

### Concurrency safety
The current `SELECT` then `INSERT/UPDATE` sequence has a narrow race window under high parallel load for the same `referenceId`. 
We would use PostgreSQL's `INSERT ... ON CONFLICT DO UPDATE ... WHERE` (upsert) executed as a single atomic statement.

### Optimistic locking
Add a `@Version` column to `Payment` so that concurrent updates to the same row fail fast with an `OptimisticLockException` rather than silently overwriting each other.

### Event sourcing / audit log
Create `payment_audit_trail` table that records every inbound event (including duplicates and stale ones) before applying any business logic.

### Input validation enhancements
Add IBAN checksum validation and currency code validation against an ISO 4217 list.

### Dead-Letter Topic (DLT) handler
Replace the console-only DLT logging with a Kafka JDBC Sink Connector that streams payments.events.DLT directly into a `dlt_events` PostgreSQL table. This gives operations a queryable audit log of all processing failures for alerting and replay.

### Applying CQRS pattern
Creating new PostgreSQL table for data retrieval (Queries). We can achieve this by creating a database trigger on `payment` table.

### Swagger
Use Swagger for documenting APIs.

### Authentication & authorisation
The API is currently not authenticated nor authorized. We should add mutual TLS and OAuth2 Authentication and Authorization. So only the payments platform can call `POST /events`, and only branch operator systems can call `GET /status` and `GET /payments`.

### Metrics and observability
Expose Micrometer metrics (event ingestion rate, duplicate rate, stale rate, latency percentiles) to a Prometheus/Grafana stack so operations can alert on anomalies.

### CI pipeline
Add a GitHub Actions that runs `mvn verify` (unit + integration tests) on every push, builds the Docker image, and publishes the built artifact to Nexus.