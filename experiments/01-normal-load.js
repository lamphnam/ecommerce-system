import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import {
  orderPayload,
  authHeaders,
  GATEWAY,
  ORDER_URL,
  login,
  think,
} from "./helpers.js";

/**
 * 01-normal-load.js
 *
 * Scenario: normal traffic baseline at steady state.
 * Sends orders via both the async (POST /api/orders) and sync
 * (POST /api/orders/sync) endpoints side by side so latency and
 * throughput can be compared under identical, moderate load.
 *
 * Collects: p50/p95/p99 latency, throughput, error rate per endpoint.
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/01-normal-load.js
 *
 * Prerequisites:
 *   - All services running (docker compose up + each service started)
 *   - At least one product seeded:
 *     curl -X POST http://localhost:8083/api/inventory/products \
 *       -H 'Content-Type: application/json' \
 *       -d '{"sku":"PROD-001","name":"Test Product","price":29.99,"stock":100000}'
 *   - A test user registered and JWT_TOKEN set, OR the login() helper works
 */

// ── Custom metrics ──
const asyncLatency = new Trend("async_order_latency", true);
const syncLatency = new Trend("sync_order_latency", true);
const asyncErrors = new Rate("async_order_errors");
const syncErrors = new Rate("sync_order_errors");

// ── Options ──
export const options = {
  scenarios: {
    normal_load: {
      executor: "constant-arrival-rate",
      rate: 10, // 10 requests/sec total
      timeUnit: "1s",
      duration: "60s",
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    async_order_latency: ["p(95)<500"], // async should be fast
    sync_order_latency: ["p(95)<5000"], // sync will be slower (expected)
    async_order_errors: ["rate<0.05"], // <5% errors
    sync_order_errors: ["rate<0.05"],
  },
};

export function setup() {
  // Attempt login; if it fails, tests run without auth (direct to service)
    const token = login();
    return { token };
}

export default function (data) {
  const headers = authHeaders(data.token);
  const body = orderPayload(1);

  // Alternate between async and sync on each iteration
  if (__ITER % 2 === 0) {
    // ── Async order ──
    const res = http.post(`${GATEWAY}/api/orders`, body, {
      headers,
      tags: { endpoint: "async" },
    });
    asyncLatency.add(res.timings.duration);
    asyncErrors.add(res.status >= 400 ? 1 : 0);
    check(res, {
      "async: status 202": (r) => r.status === 202,
    });
  } else {
    // ── Sync order ──
    const res = http.post(`${GATEWAY}/api/orders/sync`, body, {
      headers,
      tags: { endpoint: "sync" },
    });
    syncLatency.add(res.timings.duration);
    syncErrors.add(res.status >= 400 ? 1 : 0);
    check(res, {
      "sync: status 200": (r) => r.status === 200,
    });
  }

  think(50, 200);
}
