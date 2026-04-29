# payment-service

Payment Service for the Techlab e-commerce platform. Consumes payment requests from the order saga and reports back success/failure asynchronously.

## Port

`8082`

## Database

PostgreSQL `payment_db` on port `5433`.

## REST APIs (target)

- `GET /api/payments/{id}` — read payment.
- `GET /api/payments?orderId=` — list by order.

## Events

**Publishes**: `payment.succeeded`, `payment.failed` (to `payment.exchange`).

**Consumes**: `payment.requested`.

> Analytics observes these events automatically through the wildcard binding on `payment.exchange`. **Do not** publish a duplicate `analytics.event` message.

## Simulator knobs (experiment plan)

- `payment.simulator.latency-ms` — fake provider latency.
- `payment.simulator.failure-rate` — `0.0`–`1.0` to simulate flaky provider.

## Run locally

```powershell
..\mvnw -pl payment-service -am spring-boot:run
```
