# order-service

Order Service for the Techlab e-commerce platform. Owns the lifecycle of an order from creation to confirmation/cancellation; orchestrates the saga across payment and inventory.

## Port

`8081`

## Database

PostgreSQL `order_db`. Migrations under `src/main/resources/db/migration` (Flyway).

## REST APIs (target)

- `POST /api/orders` — create order, returns `202 Accepted` with `PENDING` status.
- `GET /api/orders/{id}` — read order.
- `GET /api/orders?userId=&status=` — list.

## Events

**Publishes**: `order.created`, `order.confirmed`, `order.failed`, `order.cancelled` (to `order.exchange`); `payment.requested` (to `payment.exchange`); `inventory.release.requested` (to `inventory.exchange`, compensation); `notification.requested` (to `notification.exchange`).

**Consumes**: `inventory.reserved`, `inventory.failed`, `payment.succeeded`, `payment.failed`.

> Analytics observes these events automatically through wildcard bindings on the domain exchanges. **Do not** publish a duplicate `analytics.event` message.

## Package layout (target)

```
com.techlab.ecommerce.order
├── OrderServiceApplication
├── config        (JpaConfig, ModelMapperConfig, OpenApiConfig, RabbitMqConfig)
├── controller    (REST controllers)
├── service[/impl](business interfaces + implementations)
├── repository    (Spring Data JPA)
├── entity        (JPA entities, extend BaseEntity)
├── dto/{request,response}
├── mapper        (extends SuperConverter)
├── messaging
│   ├── publisher
│   └── listener
├── enums         (OrderErrorMessage and friends)
└── exception     (service-specific only)
```

Cross-cutting code (response envelope, exception handler, base entity, event envelope, queue/exchange constants, security context filter) lives in `techlab-common`.

## Run locally

```powershell
..\mvnw -pl order-service -am spring-boot:run
```
