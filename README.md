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
├── docker/                  Docker Compose for infra (Postgres + RabbitMQ)
└── docs/                    assignment brief
```

## Modules at a glance

| Module                | Port | Database             | DB Host Port |
| --------------------- | ---- | -------------------- | ------------ |
| `api-gateway`         | 8080 | `gateway_db`         | 5437         |
| `order-service`       | 8081 | `order_db`           | 5432         |
| `payment-service`     | 8082 | `payment_db`         | 5433         |
| `inventory-service`   | 8083 | `inventory_db`       | 5434         |
| `notification-service`| 8084 | `notification_db`    | 5435         |
| `analytics-service`   | 8085 | `analytics_db`       | 5436         |

## Prerequisites

- JDK 17+
- Docker Desktop (for PostgreSQL + RabbitMQ containers)
- Maven Wrapper (already provided at the repo root)

---

## Quick start — local manual run

### 1. Start infrastructure (Postgres + RabbitMQ)

```powershell
docker compose -f docker/docker-compose.yml up -d
```

This starts **7 containers**: 6 PostgreSQL instances (one per service) + 1 RabbitMQ with the delayed-message plugin.

Wait until all containers are healthy:

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

RabbitMQ Management UI: [http://localhost:15672](http://localhost:15672) (`techlab`/`techlab`)

### 2. Build everything

```powershell
.\mvnw clean install -DskipTests
```

Or build + test:

```powershell
.\mvnw -B test
```

### 3. Run each service

Open **6 separate terminals** (or run them in the background). Each service is started with:

```powershell
# api-gateway (port 8080)
.\mvnw -pl api-gateway spring-boot:run

# order-service (port 8081)
.\mvnw -pl order-service spring-boot:run

# payment-service (port 8082)
.\mvnw -pl payment-service spring-boot:run

# inventory-service (port 8083)
.\mvnw -pl inventory-service spring-boot:run

# notification-service (port 8084)
.\mvnw -pl notification-service spring-boot:run

# analytics-service (port 8085)
.\mvnw -pl analytics-service spring-boot:run
```

### 4. Verify health

```powershell
# Gateway
Invoke-RestMethod http://localhost:8080/actuator/health

# All downstream services
@(8081,8082,8083,8084,8085) | % { "$_ : $((Invoke-RestMethod http://localhost:$_/actuator/health).status)" }
```

All should return `{"status":"UP"}`.

### 5. Stop infrastructure

```powershell
docker compose -f docker/docker-compose.yml stop
```

To remove containers and volumes completely:

```powershell
docker compose -f docker/docker-compose.yml down -v
```

---

## Centralized Swagger UI

The **api-gateway** serves as the single Swagger UI hub for all services.

### Open Swagger UI

**URL:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Use the **"Select a definition"** dropdown at the top to switch between:

| Dropdown entry       | Proxied from                                 |
| -------------------- | -------------------------------------------- |
| Gateway (Auth)       | gateway's own `/v3/api-docs/gateway`         |
| Order Service        | order-service via `/api-docs/order-service`   |
| Payment Service      | payment-service via `/api-docs/payment-service` |
| Inventory Service    | inventory-service via `/api-docs/inventory-service` |
| Notification Service | notification-service via `/api-docs/notification-service` |
| Analytics Service    | analytics-service via `/api-docs/analytics-service` |

> **Note:** Downstream service docs are proxied through the gateway. If a downstream service is not running, its docs will fail to load, but the gateway itself remains functional.

### Public paths (no JWT required)

These paths bypass JWT validation:

- `/api/auth/**` — registration and login
- `/swagger-ui/**` — Swagger UI static assets
- `/v3/api-docs/**` — gateway's own OpenAPI docs
- `/api-docs/**` — proxied downstream service docs
- `/actuator/health`, `/actuator/info` — health checks

### How to use JWT in Swagger

1. Open [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
2. Select **"Gateway (Auth)"** from the dropdown
3. Expand `POST /api/auth/register` and execute:
   ```json
   {
     "username": "demo@example.com",
     "password": "password123",
     "displayName": "Demo User"
   }
   ```
4. Copy the `token` from the response
5. Switch to any other service (e.g. **Order Service**) in the dropdown
6. Click **"Authorize"** (🔒 icon) → paste the token as `Bearer <token>`
7. Now all protected endpoints can be called with the JWT

> **Important:** The "Servers" dropdown in each service's docs will show the downstream service's direct URL (e.g. `http://localhost:8081`). When calling APIs through the gateway during a demo, use the gateway URL (`http://localhost:8080`) with the `/api/orders/**`, `/api/payments/**`, etc. path prefixes instead.

---

## Build commands

```powershell
# Build everything
.\mvnw clean install

# Build a single module (with its dependencies)
.\mvnw -pl order-service -am clean install

# Run tests only
.\mvnw -B test

# Run tests for a single module
.\mvnw -B -pl api-gateway -am test
```

## What is wired

- Multi-module Maven structure with shared `techlab-common`.
- Unified response envelope (`ApiResponse`), exception hierarchy and `GlobalExceptionHandler`.
- `BaseEntity` (id / createdAt / updatedAt / @Version) with JPA auditing.
- RabbitMQ Jackson converter and a `RabbitTemplate` with publisher confirms.
- `EventEnvelope`, exchange / queue / routing-key constants.
- `UserContextFilter` (downstream services trust gateway-injected `X-User-*` headers).
- `CorrelationIdFilter` (X-Request-Id + SLF4J MDC for correlated logs).
- API Gateway with JWT validation and route forwarding for the 5 services.
- Centralized Swagger UI at gateway for all microservices.
- `application.yml` per service with Postgres + RabbitMQ + Flyway placeholders.
- `application-test.yml` per service to keep unit tests independent of Docker.
