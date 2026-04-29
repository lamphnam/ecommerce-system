# api-gateway

Spring Cloud Gateway MVC for the Techlab e-commerce platform. Single edge entrypoint that:

1. Provides small demo auth APIs (`/api/auth/register`, `/api/auth/login`) backed by the gateway DB.
2. Issues and validates JWTs (HS256, secret in `gateway.jwt.secret`).
3. Strips the JWT and forwards `X-User-Id`, `X-User-Name`, `X-User-Role`, and `X-Request-Id` headers to downstream services.
4. Routes by path prefix to the 5 backend services.

Downstream services do **not** validate JWT; they trust the gateway boundary via `UserContextFilter` from `techlab-common`.

## Port

`8080`

## Database

PostgreSQL `gateway_db` on port `5437` stores demo gateway users only. This is not a full user-profile service.

## Auth APIs

- `POST /api/auth/register` — create a demo user, BCrypt-hash password, default role `USER`, return JWT.
- `POST /api/auth/login` — verify password and return JWT.

Example:

```powershell
$body = @{ username="demo@example.local"; password="secret123"; displayName="Demo User" } | ConvertTo-Json
$auth = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType application/json -Body $body
$token = $auth.data.accessToken
Invoke-RestMethod -Headers @{ Authorization="Bearer $token" } -Uri http://localhost:8080/api/orders
```

## Routes

| Path prefix             | Default target              | Property / env var override                   |
| ----------------------- | --------------------------- | --------------------------------------------- |
| `/api/orders/**`        | `http://localhost:8081`     | `gateway.services.order-url` / `ORDER_SERVICE_URL` |
| `/api/payments/**`      | `http://localhost:8082`     | `gateway.services.payment-url` / `PAYMENT_SERVICE_URL` |
| `/api/inventory/**`     | `http://localhost:8083`     | `gateway.services.inventory-url` / `INVENTORY_SERVICE_URL` |
| `/api/notifications/**` | `http://localhost:8084`     | `gateway.services.notification-url` / `NOTIFICATION_SERVICE_URL` |
| `/api/analytics/**`     | `http://localhost:8085`     | `gateway.services.analytics-url` / `ANALYTICS_SERVICE_URL` |

URLs come from `GatewayProperties.Services` (bound to `gateway.services.*`). Local dev keeps the localhost defaults; docker-compose / Kubernetes set the env vars to in-cluster service addresses.

## Public paths (no JWT required)

`/api/auth/`, `/v3/api-docs`, `/swagger-ui`, `/actuator/health`, `/actuator/info`.

## JWT claim mapping

| JWT claim | Header injected        |
| --------- | ---------------------- |
| `uid`     | `X-User-Id`            |
| `sub`     | `X-User-Name`          |
| `role`    | `X-User-Role`          |

Override claim names via `gateway.jwt.user-id-claim` / `username-claim` / `role-claim`. Token lifetime comes from `gateway.jwt.expiration-seconds` / `JWT_EXPIRATION_SECONDS`.

## Run locally

```powershell
# Start docker/docker-compose.yml first so gateway_db exists on localhost:5437.
$env:JWT_SECRET="change-me-please-this-is-a-development-only-jwt-secret-32+chars"
..\mvnw -pl api-gateway -am spring-boot:run
```
