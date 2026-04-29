# payment-service

Payment Service for the Techlab e-commerce platform. Consumes payment requests from the order saga and reports back success/failure asynchronously.

## Port

`8082`

## Database

PostgreSQL `payment_db` on port `5433`.

## REST APIs

- `GET /api/payments/{id}` — read one payment with attempt history.
- `GET /api/payments?orderId=&page=&size=` — list payments, optionally filtered by order id.

## Events

**Consumes**:

- `payment.process.q` ← `payment.exchange / payment.requested`

**Publishes** (domain events only, to `payment.exchange`):

- `payment.succeeded` — simulated provider approved the charge.
- `payment.failed` — simulated provider declined the charge or request validation failed.

> Analytics observes these events automatically through the wildcard binding on `payment.exchange`. **Do not** publish a duplicate `analytics.event` message.

## Simulator knobs (experiment plan)

- `payment.simulator.latency-ms` / `PAYMENT_LATENCY_MS` — fake provider latency.
- `payment.simulator.failure-rate` / `PAYMENT_FAILURE_RATE` — `0.0`–`1.0` decline probability.

`failure-rate=0.0` is deterministic success, `failure-rate=1.0` is deterministic decline; intermediate values are random and useful for traffic/failure experiments.

## Idempotency and DLQ behavior

- Inbound `payment.requested` inserts into `processed_events` first.
- Duplicate `event_id` raises `DuplicateProcessedEventException`; the listener `basicAck`s and skips.
- Business/provider decline persists `FAILED`, publishes `payment.failed`, and `basicAck`s.
- Unexpected errors escape the transactional handler; the listener `basicNack(requeue=false)` so RabbitMQ routes the message to `payment.process.dlq`.
- `payment_attempts` records attempt number, status, provider latency, and error message for experiments.

## Run locally

```powershell
..\mvnw -pl payment-service -am spring-boot:run
```
