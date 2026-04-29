# order-service

Order Service for the Techlab e-commerce platform. Owns the lifecycle of an order from creation to confirmation/cancellation; orchestrates the saga across payment and inventory.

## Port

`8081`

## Database

PostgreSQL `order_db`. Migrations under `src/main/resources/db/migration` (Flyway).

## Status

Phase 5 — saga orchestrator implemented. The `order.created` flow runs end-to-end through Order Service; downstream services (Inventory, Payment, Notification) follow in Phase 6+.

## REST APIs

All endpoints read the current user from the gateway-injected headers (`X-User-Id`, etc.) via `UserContextFilter`. Responses use the unified `ApiResponse` envelope.

| Method | Path                          | Behaviour                                                                                                    |
| ------ | ----------------------------- | ------------------------------------------------------------------------------------------------------------ |
| POST   | `/api/orders`                 | Persist a new `PENDING` order, publish `order.created`, return **202 Accepted**. Honours `Idempotency-Key` header. |
| GET    | `/api/orders/{id}`            | Read one order owned by the caller (404 if not theirs).                                                      |
| GET    | `/api/orders?status=&page=&size=` | List the caller's orders, optionally filtered by status. Pageable.                                       |

## Saga lifecycle

```
PENDING ──► INVENTORY_RESERVED ──► CONFIRMED
   │                │                 ▲
   │                ▼                 │
   └─────────► FAILED ◄───────────────┘
                  ▲
   (manual) CANCELLED
```

| Inbound queue                       | Saga reaction                                                                  |
| ----------------------------------- | ------------------------------------------------------------------------------ |
| `order.inventory-reserved.q`        | `PENDING → INVENTORY_RESERVED`, publishes `payment.requested`.                 |
| `order.inventory-failed.q`          | `→ FAILED` (with reason), publishes `notification.requested` (`ORDER_FAILED`). |
| `order.payment-succeeded.q`         | `INVENTORY_RESERVED → CONFIRMED`, publishes `notification.requested` (`ORDER_CONFIRMED`). |
| `order.payment-failed.q`            | `→ FAILED` (with reason), publishes `inventory.release.requested` (compensation) + `notification.requested` (`PAYMENT_FAILED`). |

## Events

**Publishes**: `order.created` (to `order.exchange`); `payment.requested` (to `payment.exchange`); `inventory.release.requested` (to `inventory.exchange`, compensation only); `notification.requested` (to `notification.exchange`).

**Consumes**: `inventory.reserved`, `inventory.failed`, `payment.succeeded`, `payment.failed`.

> Analytics observes these events automatically through wildcard bindings on the domain exchanges. **Do not** publish a duplicate `analytics.event` message.

Every event is wrapped in `EventEnvelope<T>` with `eventId`, `eventType`, `sourceService=order-service`, `occurredAt`, `version`, `correlationId` (from MDC `X-Request-Id`), `userId`, and the payload.

## Idempotency

**HTTP** — `POST /api/orders` accepts an `Idempotency-Key` header. The unique index `(user_id, idempotency_key)` makes a replay return the previously-created order instead of duplicating.

**Messaging** — every inbound saga event is recorded in `processed_events` (PK = `eventId`) inside the same transaction that updates the order. A duplicate broker delivery hits the unique constraint, the transaction rolls back, and the listener acks-and-skips. Listeners use **manual ack** so the broker is told about success only after the DB commit; on unexpected failure the message is `basicNack`-ed with `requeue=false` and routed to the queue's DLQ via `x-dead-letter-exchange`.

## Database

PostgreSQL `order_db`. Migrations under `src/main/resources/db/migration` (Flyway). `V1__init_orders.sql` creates `orders`, `order_items`, `processed_events`. Tests use H2 in PostgreSQL mode with Hibernate `create-drop`.

## Package layout

```
com.techlab.ecommerce.order
├── OrderServiceApplication
├── config        (JpaConfig, ModelMapperConfig, OpenApiConfig, RabbitMqConfig)
├── controller    (OrderController)
├── service       (OrderService, OrderSagaService, IdempotentEventProcessor)
│   └── impl      (OrderServiceImpl, OrderSagaServiceImpl)
├── repository    (OrderRepository, ProcessedEventRepository)
├── entity        (Order, OrderItem, ProcessedEvent)
├── dto/{request,response}
├── mapper        (OrderMapper, extends SuperConverter)
├── messaging
│   ├── EventEnvelopeFactory
│   ├── publisher (OrderEventPublisher)
│   ├── listener  (4 saga listeners + MessageAckHandler)
│   └── payload   (outbound + inbound payload DTOs)
├── enums         (OrderStatus, OrderErrorMessage)
└── exception     (OrderException)
```

Cross-cutting code (response envelope, exception handler, base entity, event envelope, queue/exchange constants, security context filter, RabbitMQ message converter) lives in `techlab-common`.

## Run locally

```powershell
..\mvnw -pl order-service -am spring-boot:run
```
