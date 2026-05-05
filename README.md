# Techlab E-commerce — Async Microservices Demo

Spring Boot 3.5 multi-module microservices demo for an e-commerce order saga built around RabbitMQ. The system compares the real asynchronous workflow (`POST /api/orders`) with a synchronous baseline (`POST /api/orders/sync`) and includes k6 experiments plus architecture/report documentation.

## Overview

This repository contains:
- a shared library: `techlab-common`
- an API gateway: `api-gateway`
- five domain services: order, inventory, payment, notification, analytics
- RabbitMQ-based event choreography with DLQ support
- k6 performance/behavior experiments under `experiments/`
- generated report/docs under `docs/`

## Repository layout

```text
techlab/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── techlab-common/
├── api-gateway/
├── order-service/
├── payment-service/
├── inventory-service/
├── notification-service/
├── analytics-service/
├── docker/
├── experiments/
├── results/
└── docs/
```

## Services and ports

| Module | App Port | Database | DB Host Port |
|---|---:|---|---:|
| `api-gateway` | 8080 | `gateway_db` | 5437 |
| `order-service` | 8081 | `order_db` | 5432 |
| `payment-service` | 8082 | `payment_db` | 5433 |
| `inventory-service` | 8083 | `inventory_db` | 5434 |
| `notification-service` | 8084 | `notification_db` | 5435 |
| `analytics-service` | 8085 | `analytics_db` | 5436 |

## Architecture summary

- **Gateway-authenticated edge**: JWT is validated only in `api-gateway`.
- **Downstream trust model**: services rely on gateway-injected `X-User-*` headers.
- **Async order path**: order creation returns `202 Accepted`, then saga steps continue via RabbitMQ.
- **Sync comparison path**: `POST /api/orders/sync` performs direct service-to-service calls for experiment comparison.
- **Message choreography**: order, inventory, payment, and notification services communicate through topic exchanges.
- **Failure handling**: each consumer queue has a dead-letter queue; idempotency is enforced through `processed_events` tables.
- **Analytics fan-in**: analytics consumes wildcard-bound events from all domain exchanges.

For the fuller architecture write-up, see:
- `docs/architecture.md`
- `docs/message-flow.md`
- `docs/database.md`
- `docs/why-rabbitmq.md`

## Prerequisites

- JDK 17+
- Docker Desktop
- k6 (for experiments)

Use the Maven Wrapper from the repo root:
- Windows: `mvnw.cmd`
- Unix-like shells: `./mvnw`

## Start infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d
```

This starts RabbitMQ plus one PostgreSQL instance per service.

RabbitMQ Management UI:
- URL: `http://localhost:15672`
- Username: `techlab`
- Password: `techlab`

Stop infrastructure:

```bash
docker compose -f docker/docker-compose.yml stop
```

Teardown including volumes:

```bash
docker compose -f docker/docker-compose.yml down -v
```

## Build and test

Build everything:

```bash
./mvnw clean install -DskipTests
```

Run all tests:

```bash
./mvnw -B test
```

Build one module with required dependencies:

```bash
./mvnw -pl order-service -am clean install
```

Run tests for one module:

```bash
./mvnw -B -pl api-gateway -am test
```

## Run services locally

Start each service from the repo root in a separate terminal:

```bash
./mvnw -pl api-gateway spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl payment-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
./mvnw -pl analytics-service spring-boot:run
```

## Health checks

Gateway:

```bash
curl http://localhost:8080/actuator/health
```

Downstream services:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

## Swagger / OpenAPI

The gateway exposes centralized Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

Public paths that bypass JWT:
- `/api/auth/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/api-docs/**`
- `/actuator/health`
- `/actuator/info`

## Main business flow

### Async production-style flow
1. Client calls `POST /api/orders`.
2. Order service persists the order and returns `202 Accepted`.
3. `order.created` is published.
4. Inventory reserves stock and emits `inventory.reserved` or `inventory.failed`.
5. Order service reacts and may request payment.
6. Payment emits `payment.succeeded` or `payment.failed`.
7. Order service confirms the order or triggers compensation and notification.

### Sync experiment baseline
1. Client calls `POST /api/orders/sync`.
2. Order service calls inventory and payment over HTTP.
3. The response is returned only after downstream processing completes.

## k6 experiments

Experiment scripts live in `experiments/`.

Available scenarios:
- `01-normal-load.js`
- `02-spike-test.js`
- `03-slow-provider.js`
- `04-provider-failure.js`
- `05-idempotency.js`
- `06-dlq-behaviour.js`
- `07-recovery.js`

Example run:

```bash
k6 run experiments/01-normal-load.js
```

The executed outputs currently available in `results/` are:
- `results/01-normal-load.txt`
- `results/02-spike-test.txt`
- `results/03-slow-provider.txt`
- `results/04-provider-failure.txt`
- `results/05-idempotency.txt`
- `results/06-dlq-behaviour.txt`
- `results/07-recovery.txt`
- plus matching `.json` artifacts for each scenario

## Do we have k6 performance/report evidence?

Yes.

This repository already contains both:
1. **raw k6 output artifacts** in `results/`
2. **a written experiment report** in `docs/experiment-report.md`

### What is directly evidenced by the saved runs

- **Normal load**: real measured baseline comparing async vs sync path
- **Spike test**: real executed run showing availability preserved but latency degradation under spike load
- **Idempotency**: real executed run showing duplicate suppression behavior
- **DLQ behaviour**: real executed run showing dead-letter routing evidence
- **Recovery**: real executed run with queue-depth observation

### Important qualification

Some scenario files were executed, but their intended fault preconditions were not fully applied during the measured run:
- `03-slow-provider`
- `04-provider-failure`
- `07-recovery`

For the exact interpretation of what is real measured evidence versus projected behavior, use:
- `docs/experiment-report.md`

## Key docs

- `docs/architecture.md` — module roles, gateway flow, async vs sync path, RabbitMQ topology
- `docs/database.md` — per-service schema and ER diagrams
- `docs/message-flow.md` — Mermaid sequence diagrams for saga and compensation flows
- `docs/experiment-report.md` — k6 run interpretation and measured results
- `docs/why-rabbitmq.md` — technology-choice rationale

## Notes

- Tests use H2 in PostgreSQL compatibility mode and disable RabbitMQ auto-configuration.
- Flyway is the schema source of truth for production-like runtime configuration.
- The analytics service already receives wildcard-bound domain events; domain services should not publish duplicate analytics-specific events.
- The sync order path exists only for experiment comparison; the async RabbitMQ flow is the intended architecture.
