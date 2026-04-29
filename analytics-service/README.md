# analytics-service

Analytics Service for the Techlab e-commerce platform. Fan-out consumer of every business event in the system; never blocks the critical user flow.

## Port

`8085`

## Database

PostgreSQL `analytics_db` on port `5436`.

## REST APIs

- `GET /api/analytics/events?eventType=&sourceService=&aggregateId=&userId=&from=&to=&page=&size=` — list observed events with optional filters.
- `GET /api/analytics/events/{id}` — read one stored event.
- `GET /api/analytics/summary/orders` — simple order-saga event counts.

## Events

**Publishes**: nothing.

**Consumes**: every business event via `analytics.events.q`. The queue is bound with the `#` wildcard to each **domain exchange** so it receives the producing service's normal domain events directly:

- `order.exchange` (order lifecycle)
- `payment.exchange` (payment lifecycle)
- `inventory.exchange` (stock lifecycle)
- `notification.exchange` (delivery status)
- `analytics.exchange` *(reserved)* — owned by this service for any future analytics-internal events; domain services must **not** publish here.

This avoids duplicate analytics records: each domain event is captured exactly once, by its source exchange. There is no separate `analytics.event` fan-out from producers.

## Storage and idempotency

- Stores generic `EventEnvelope` metadata plus `payload_json`; Analytics does not depend on producer payload classes.
- Best-effort `aggregateId` extraction checks payload fields in this order: `orderId`, `paymentId`, `productId`, `notificationId`.
- Inbound messages insert into `processed_events` first. Duplicate `event_id` raises `DuplicateProcessedEventException`; the listener `basicAck`s and skips.
- Unexpected persistence/processing errors are not swallowed; the listener `basicNack(requeue=false)` so RabbitMQ routes the message to `analytics.events.dlq`.

## Run locally

```powershell
..\mvnw -pl analytics-service -am spring-boot:run
```
