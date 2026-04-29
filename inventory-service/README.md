# inventory-service

Inventory Service for the Techlab e-commerce platform. Owns product stock, performs atomic stock reservations, and emits inventory events.

## Port

`8083`

## Database

PostgreSQL `inventory_db` on port `5434`.

## REST APIs (target)

- `GET /api/inventory/products/{id}` — read stock.
- `POST /api/inventory/products` (admin) — create product.
- `PATCH /api/inventory/products/{id}/stock` (admin) — adjust stock.

## Events

**Publishes**: `inventory.reserved`, `inventory.failed`, `inventory.released` (to `inventory.exchange`).

**Consumes**: `order.created` (reserve), `inventory.release.requested` (compensation).

> Analytics observes these events automatically through the wildcard binding on `inventory.exchange`. **Do not** publish a duplicate `analytics.event` message.

## Concurrency

Stock decrements use a single atomic `UPDATE ... WHERE stock >= :qty` to avoid race conditions; admin updates use optimistic `@Version` locking.

## Run locally

```powershell
..\mvnw -pl inventory-service -am spring-boot:run
```
