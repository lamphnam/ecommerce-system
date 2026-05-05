# Experiments — Async vs Sync Performance Comparison

This directory contains k6 load test scripts that prove asynchronous RabbitMQ messaging outperforms synchronous REST under load, failure, and spike conditions.

## Prerequisites

1. **Infrastructure running:**
   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```

2. **All services running** (order, payment, inventory, notification, analytics, api-gateway).

3. **Seed test data** — at least one product with high stock:
   ```bash
   curl -X POST http://localhost:8083/api/inventory/products \
     -H 'Content-Type: application/json' \
     -d '{"sku":"PROD-001","name":"Test Product","price":29.99,"stock":1000000}'
   ```

4. **Register a test user** (if auth is required):
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"email":"test@techlab.com","password":"password123","name":"Test User"}'
   ```

5. **Install k6:** https://k6.io/docs/getting-started/installation/

## Environment Variables

### k6 script variables

| Variable | Default | Description |
|---|---|---|
| `GATEWAY_URL` | `http://localhost:8080` | API Gateway base URL |
| `ORDER_SERVICE_URL` | `http://localhost:8081` | Direct order-service URL (bypass gateway) |
| `RABBITMQ_API` | `http://localhost:15672/api` | RabbitMQ Management API |

### Service toggles (set on the service, not on k6)

| Variable | Service | Default | Description |
|---|---|---|---|
| `PAYMENT_LATENCY_MS` | payment-service | `500` | Simulated payment processing delay |
| `PAYMENT_FAILURE_RATE` | payment-service | `0.0` | Probability of payment decline (0.0–1.0) |
| `NOTIFICATION_LATENCY_MS` | notification-service | `200` | Simulated notification send delay |
| `NOTIFICATION_FAILURE_RATE` | notification-service | `0.0` | Probability of notification failure (0.0–1.0) |

To change a toggle, restart the service with the env var:
```bash
PAYMENT_LATENCY_MS=2500 ./mvnw -pl payment-service spring-boot:run
```

## Test Scripts

### 01 — Normal Load Baseline
```bash
k6 run experiments/01-normal-load.js
```
Steady 10 req/s for 60s. Alternates between async (`POST /api/orders`) and sync (`POST /api/orders/sync`). Compares p50/p95/p99 latency side by side.

**Expected:** Async p95 < 500ms; sync p95 ~500-1000ms (blocked on payment simulator latency).

### 02 — Spike Test
```bash
k6 run experiments/02-spike-test.js
```
Ramps from 5 → 200 VUs, sustains for 60s, then drops back. Tests how each mode handles a sudden concurrency jump.

**Expected:** Async maintains sub-1s p95; sync starts timing out or seeing thread pool exhaustion.

### 03 — Slow Provider
```bash
# First: restart payment-service with high latency
PAYMENT_LATENCY_MS=2500 ./mvnw -pl payment-service spring-boot:run

# Then:
k6 run experiments/03-slow-provider.js
```
Payment provider takes 2.5s per call. Async returns 202 in <100ms; sync is blocked for the full 2.5s+.

**Expected:** Async p95 < 500ms; sync p50 > 2500ms.

### 04 — Provider Failure
```bash
# First: restart payment-service with 30% failure rate
PAYMENT_FAILURE_RATE=0.3 ./mvnw -pl payment-service spring-boot:run

# Then:
k6 run experiments/04-provider-failure.js
```
30% of payments fail. Async still returns 202 immediately (saga handles failure asynchronously). Sync propagates the failure directly to the caller.

**Expected:** Async error rate ~0%; sync error rate ~15% (only sync orders get immediate failure). After the test, check DLQ counts and order statuses.

### 05 — Idempotency Verification
```bash
k6 run experiments/05-idempotency.js
```
Each VU sends the same `Idempotency-Key` header 5 times. Verifies that only one order is created per key, for both async and sync endpoints.

**Expected:** `idempotency_failures` rate = 0.

### 06 — DLQ Behaviour
```bash
# First: restart payment-service with 100% failure
PAYMENT_FAILURE_RATE=1.0 ./mvnw -pl payment-service spring-boot:run

# Then:
k6 run experiments/06-dlq-behaviour.js
```
All payments fail. Messages exhaust retries and land in the DLQ. The script polls RabbitMQ Management API to show DLQ depth climbing.

**Expected:** Orders accepted (202), all eventually reach FAILED status. DLQ messages accumulate. System remains healthy.

### 07 — Recovery (Manual)
```bash
k6 run experiments/07-recovery.js
```
Requires manual intervention during the test:
- **At ~30s:** Stop payment-service (`Ctrl+C` or `docker stop`)
- **At ~60s:** Restart payment-service

The script sends orders continuously and monitors `payment.process.q` depth. When payment-service is down, messages queue up. On restart, they drain.

**Expected:** Orders accepted throughout (async decoupling). Queue depth spikes during outage, then drains to 0 after restart. All orders eventually reach terminal state.

## Collecting Results

k6 outputs summary statistics automatically. For JSON output:
```bash
k6 run --out json=results/01-normal-load.json experiments/01-normal-load.js
```

For CSV:
```bash
k6 run --out csv=results/01-normal-load.csv experiments/01-normal-load.js
```

## Key Metrics to Compare

| Metric | Async (Expected) | Sync (Expected) |
|---|---|---|
| p50 latency | < 100ms | ~ payment latency (500ms+) |
| p95 latency | < 300ms | 1-3x payment latency |
| p99 latency | < 500ms | Can spike to timeout |
| Error rate (normal) | ~0% | ~0% |
| Error rate (30% fail) | ~0% (saga handles) | ~15% (direct failure) |
| Error rate (spike) | < 5% | 10-30% (thread exhaustion) |
| Recovery time | Instant (messages buffer) | N/A (requests fail) |
