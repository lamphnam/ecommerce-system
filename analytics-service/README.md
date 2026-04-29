# analytics-service

Analytics Service for the Techlab e-commerce platform. Fan-out consumer of every business event in the system; never blocks the critical user flow.

## Port

`8085`

## Database

PostgreSQL `analytics_db` on port `5436`.

## REST APIs (target)

- `GET /api/analytics/events?type=&from=&to=` (admin) — list events.
- `GET /api/analytics/summary/orders` (admin) — pre-aggregated summary.

## Events

**Publishes**: nothing.

**Consumes**: every business event via `analytics.events.q`. The queue is bound with the `#` wildcard to each **domain exchange** so it receives the producing service's normal domain events directly:

- `order.exchange` (order lifecycle)
- `payment.exchange` (payment lifecycle)
- `inventory.exchange` (stock lifecycle)
- `notification.exchange` (delivery status)
- `analytics.exchange` *(reserved)* — owned by this service for any future analytics-internal events; domain services must **not** publish here.

This avoids duplicate analytics records: each domain event is captured exactly once, by its source exchange. There is no separate `analytics.event` fan-out from producers.

## Run locally

```powershell
..\mvnw -pl analytics-service -am spring-boot:run
```
