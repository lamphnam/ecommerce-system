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

**Consumes**: every business event via `analytics.events.q` (wildcard bindings to `order.exchange`, `payment.exchange`, `inventory.exchange`, `notification.exchange`, `analytics.exchange`).

## Run locally

```powershell
..\mvnw -pl analytics-service -am spring-boot:run
```
