# inventory-service

Inventory Service for the Techlab e-commerce platform. Owns product stock, performs atomic stock reservations, and emits inventory events.

## Port

`8083`

## Database

PostgreSQL `inventory_db` on port `5434`.

## REST APIs

- `POST /api/inventory/products` — create a product / seed initial stock.
- `GET /api/inventory/products/{id}` — read product stock/details.
- `PATCH /api/inventory/products/{id}/stock` — set stock for local smoke tests.

## Events

**Consumes**:

- `inventory.reserve.q` ← `order.exchange / order.created`
- `inventory.release.q` ← `inventory.exchange / inventory.release.requested`

**Publishes** (domain events only, to `inventory.exchange`):

- `inventory.reserved` — all order items were reserved.
- `inventory.failed` — at least one item could not be reserved.
- `inventory.released` — compensation released existing reservations for an order.

> Analytics observes these events automatically through the wildcard binding on `inventory.exchange`. **Do not** publish a duplicate `analytics.event` message.

## Concurrency and idempotency

- Stock reservation uses a single atomic `UPDATE products SET stock = stock - :qty WHERE id = :productId AND stock >= :qty` per order item to avoid overselling under concurrent requests.
- If a later item fails, previously decremented items in the same handler are added back before publishing `inventory.failed`.
- Inbound messages insert into `processed_events` first. A duplicate `event_id` raises `DuplicateProcessedEventException`; the listener acks and skips it.
- Manual ack policy: success → `basicAck`; duplicate → `basicAck`; unexpected error → `basicNack(requeue=false)` so the message goes to the DLQ.

## Run locally

```powershell
..\mvnw -pl inventory-service -am spring-boot:run
```
