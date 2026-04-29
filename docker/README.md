# Local Infrastructure (Phase 4)

This stack provisions everything the services need to run **except** the services themselves:

- **RabbitMQ 3.13** with the `rabbitmq_delayed_message_exchange`, `rabbitmq_management`, and `rabbitmq_prometheus` plugins enabled (custom image built from `./rabbitmq/Dockerfile`).
- **5 PostgreSQL 16 instances**, one per service, each on its own host port.

The Spring Boot apps run on the host via `mvnw spring-boot:run` for the dev loop. The full bundled stack (services + infra in one compose file) is delivered in Phase 11.

## Layout

```
docker/
├── docker-compose.yml
├── README.md
└── rabbitmq/
    ├── Dockerfile
    └── enabled_plugins
```

## Ports (host -> container)

| Service              | Host port | Notes                       |
| -------------------- | --------- | --------------------------- |
| RabbitMQ AMQP        | 5672      | broker                      |
| RabbitMQ Management  | 15672     | Web UI (http://localhost:15672) |
| RabbitMQ Prometheus  | 15692     | scrape target               |
| postgres-order       | 5432      | db `order_db`               |
| postgres-payment     | 5433      | db `payment_db`             |
| postgres-inventory   | 5434      | db `inventory_db`           |
| postgres-notification| 5435      | db `notification_db`        |
| postgres-analytics   | 5436      | db `analytics_db`           |

These match the port assumptions in each service's `application.yml`.

## Default credentials

Defined inline in `docker-compose.yml` via shell defaults (`${VAR:-fallback}`):

| Variable        | Default    | Used by         |
| --------------- | ---------- | --------------- |
| `DB_USER`       | `techlab`  | all 5 Postgres  |
| `DB_PASS`       | `techlab`  | all 5 Postgres  |
| `RABBITMQ_USER` | `techlab`  | RabbitMQ        |
| `RABBITMQ_PASS` | `techlab`  | RabbitMQ        |

The five services default to **DB user/pass `techlab/techlab`** and **RabbitMQ user/pass `techlab/techlab`** in their `application.yml`. Either:

- Override the broker creds when starting a service (recommended):
  ```powershell
  $env:RABBITMQ_USER="techlab"; $env:RABBITMQ_PASS="techlab"; .\mvnw -pl order-service spring-boot:run
  ```
- Or override the compose values:
  ```powershell
  $env:RABBITMQ_USER="techlab"; $env:RABBITMQ_PASS="techlab"; docker compose -f docker/docker-compose.yml up -d
  ```

## Usage

From the repo root.

**Start the stack:**

```powershell
docker compose -f docker/docker-compose.yml up -d --build
```

The `--build` is needed once so the custom RabbitMQ image is built (delayed-message plugin downloaded from GitHub). Subsequent `up -d` calls reuse the cached image.

**Watch healthchecks:**

```powershell
docker compose -f docker/docker-compose.yml ps
```

All 6 containers should show `healthy` within ~30s. The healthcheck for Postgres uses `pg_isready`; RabbitMQ uses `rabbitmq-diagnostics ping`.

**Tail logs:**

```powershell
docker compose -f docker/docker-compose.yml logs -f rabbitmq
```

**Stop the stack:**

```powershell
docker compose -f docker/docker-compose.yml down
```

**Reset all data (drops volumes — destructive):**

```powershell
docker compose -f docker/docker-compose.yml down -v
```

## Verifying the delayed-message plugin

1. Open http://localhost:15672 (login with `techlab/techlab` by default).
2. Go to **Admin -> Plugins** and confirm `rabbitmq_delayed_message_exchange` is `enabled`.
3. Or via the CLI:
   ```powershell
   docker exec -it techlab-rabbitmq rabbitmq-plugins list
   ```

## Smoke test (after Phase 5+ adds business logic)

Once at least one service is wired up:

1. Start the stack.
2. `cd ..` and run `.\mvnw -pl order-service spring-boot:run`.
3. The first time the service connects, it declares its exchanges/queues/bindings (`RabbitMqConfig`). You should see all `*.exchange`, `*.exchange.dlx`, `order.inventory-*.q`, `order.payment-*.q`, and the matching `.dlq` queues appear in the management UI.
