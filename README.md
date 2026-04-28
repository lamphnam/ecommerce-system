# Techlab E-commerce — Microservices Demo (RabbitMQ)

Multi-module Spring Boot 3.5 project that demonstrates an asynchronous, message-queue-driven e-commerce backend for the Techlab intern assignment.

## Repository layout

```
techlab/
├── pom.xml                  parent POM (BOM, dependency mgmt, modules)
├── mvnw / mvnw.cmd / .mvn/  Maven wrapper (single one for the whole repo)
├── techlab-common/          shared DTOs, exceptions, base entity, event envelope, security filters
├── api-gateway/             Spring Cloud Gateway MVC, JWT validation, header propagation
├── order-service/
├── payment-service/
├── inventory-service/
├── notification-service/
├── analytics-service/
└── docs/                    assignment brief
```

## Modules at a glance

| Module                | Port | Database (target)            | Notes                                  |
| --------------------- | ---- | ---------------------------- | -------------------------------------- |
| `api-gateway`         | 8080 | —                            | JWT validation, route to 5 services    |
| `order-service`       | 8081 | `order_db` @ 5432            | Order saga orchestrator                |
| `payment-service`     | 8082 | `payment_db` @ 5433          | Simulated payment provider             |
| `inventory-service`   | 8083 | `inventory_db` @ 5434        | Atomic stock reservation               |
| `notification-service`| 8084 | `notification_db` @ 5435     | Email/push (mocked to logs)            |
| `analytics-service`   | 8085 | `analytics_db` @ 5436        | Fan-out consumer of all events         |

## Prerequisites

- JDK 17+
- Docker Desktop (for PostgreSQL + RabbitMQ — added in a later phase)
- Maven Wrapper (already provided at the repo root)

## Build everything

```powershell
.\mvnw clean install
```

## Build a single module (with its dependencies)

```powershell
.\mvnw -pl order-service -am clean install
```

## Run a single service locally

```powershell
.\mvnw -pl order-service -am spring-boot:run
```

## What is in this template

Everything compiles to an empty Spring Boot application skeleton — **no business logic yet**. What is wired:

- Multi-module Maven structure with shared `techlab-common`.
- Unified response envelope (`ApiResponse`), exception hierarchy and `GlobalExceptionHandler`.
- `BaseEntity` (id / createdAt / updatedAt / @Version) with JPA auditing.
- RabbitMQ Jackson converter and a `RabbitTemplate` with publisher confirms.
- `EventEnvelope`, exchange / queue / routing-key constants.
- `UserContextFilter` (downstream services trust gateway-injected `X-User-*` headers).
- `CorrelationIdFilter` (X-Request-Id + SLF4J MDC for correlated logs).
- API Gateway with JWT validation and route forwarding for the 5 services.
- `application.yml` per service with Postgres + RabbitMQ + Flyway placeholders.
- `application-test.yml` per service to keep unit tests independent of Docker.

