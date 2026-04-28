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

**Publishes**: `order.created`, `payment.requested`, `inventory.release.requested`, `notification.requested`, `analytics.event`, `order.confirmed`, `order.failed`, `order.cancelled`.

**Consumes**: `inventory.reserved`, `inventory.failed`, `payment.succeeded`, `payment.failed`.

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
