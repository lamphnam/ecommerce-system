# api-gateway

Spring Cloud Gateway MVC for the Techlab e-commerce platform. Single edge entrypoint that:

1. Validates the inbound JWT (HS256, secret in `gateway.jwt.secret`).
2. Strips the JWT and forwards `X-User-Id`, `X-User-Name`, `X-User-Role`, and `X-Request-Id` headers to downstream services.
3. Routes by path prefix to the 5 backend services.

Downstream services do **not** validate JWT; they trust the gateway boundary via `UserContextFilter` from `techlab-common`.

## Port

`8080`

## Routes

| Path prefix         | Target                  |
| ------------------- | ----------------------- |
| `/api/orders/**`        | `http://localhost:8081` (order-service) |
| `/api/payments/**`      | `http://localhost:8082` (payment-service) |
| `/api/inventory/**`     | `http://localhost:8083` (inventory-service) |
| `/api/notifications/**` | `http://localhost:8084` (notification-service) |
| `/api/analytics/**`     | `http://localhost:8085` (analytics-service) |

## Public paths (no JWT required)

`/api/auth/`, `/v3/api-docs`, `/swagger-ui`, `/actuator/health`, `/actuator/info`.

## JWT claim mapping

| JWT claim | Header injected        |
| --------- | ---------------------- |
| `uid`     | `X-User-Id`            |
| `sub`     | `X-User-Name`          |
| `role`    | `X-User-Role`          |

Override claim names via `gateway.jwt.user-id-claim` / `username-claim` / `role-claim`.

## Run locally

```powershell
$env:JWT_SECRET="change-me-please-this-is-a-development-only-jwt-secret-32+chars"
..\mvnw -pl api-gateway -am spring-boot:run
```
