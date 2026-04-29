# notification-service

Notification Service for the Techlab e-commerce platform. Consumes notification commands and delivers email/push (mocked to logs in the demo).

## Port

`8084`

## Database

PostgreSQL `notification_db` on port `5435`.

## REST APIs

- `GET /api/notifications/{id}` — read one notification record.
- `GET /api/notifications?status=&page=&size=` — list notifications, optionally filtered by `QUEUED`, `SENT`, or `FAILED`.

## Events

**Consumes**:

- `notification.send.q` ← `notification.exchange / notification.requested`

**Publishes** (domain events only, to `notification.exchange`):

- `notification.sent` — simulated delivery succeeded.
- `notification.failed` — simulated delivery failed.

> Delivery-status events reach analytics-service automatically through the wildcard binding on `notification.exchange`. **Do not** publish a duplicate `analytics.event` message.

## Simulator knobs (experiment plan)

- `notification.simulator.latency-ms` / `NOTIFICATION_LATENCY_MS` — fake delivery latency.
- `notification.simulator.failure-rate` / `NOTIFICATION_FAILURE_RATE` — `0.0`–`1.0` failure probability.

`failure-rate=0.0` is deterministic success, `failure-rate=1.0` is deterministic failure; intermediate values are random and useful for traffic/failure experiments.

## Idempotency and DLQ behavior

- Inbound `notification.requested` inserts into `processed_events` first.
- Duplicate `event_id` raises `DuplicateProcessedEventException`; the listener `basicAck`s and skips.
- Simulated delivery failure persists `FAILED`, publishes `notification.failed`, and `basicAck`s.
- Unexpected errors escape the transactional handler; the listener `basicNack(requeue=false)` so RabbitMQ routes the message to `notification.send.dlq`.

## Run locally

```powershell
..\mvnw -pl notification-service -am spring-boot:run
```
