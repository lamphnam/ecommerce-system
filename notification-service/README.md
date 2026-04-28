# notification-service

Notification Service for the Techlab e-commerce platform. Consumes notification commands and delivers email/push (mocked to logs in the demo).

## Port

`8084`

## Database

PostgreSQL `notification_db` on port `5435`.

## REST APIs (target)

- `GET /api/notifications/{id}` (admin/debug) — read a notification record.

## Events

**Publishes**: `analytics.event` (delivery status).

**Consumes**: `notification.requested`.

## Run locally

```powershell
..\mvnw -pl notification-service -am spring-boot:run
```
