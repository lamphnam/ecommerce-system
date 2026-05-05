import http from "k6/http";
import encoding from "k6/encoding";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

/**
 * helpers.js — shared utilities for all k6 experiment scripts.
 *
 * Usage: import { GATEWAY, orderPayload, authHeaders, checkOk } from './helpers.js';
 */

// ── Base URLs ──
export const GATEWAY = __ENV.GATEWAY_URL || "http://localhost:8080";
export const ORDER_URL = __ENV.ORDER_SERVICE_URL || "http://localhost:8081";
export const PAYMENT_URL = __ENV.PAYMENT_URL || "http://localhost:8082";
export const INVENTORY_URL = __ENV.INVENTORY_URL || "http://localhost:8083";
export const RABBITMQ_API = __ENV.RABBITMQ_API || "http://localhost:15672/api";

// ── Auth ──
// In a real test, generate a JWT via POST /api/auth/login first.
// For local experiments, call setup() or pass JWT_TOKEN env var.
export function authHeaders(token) {
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}

// ── Payloads ──
let _orderCounter = 0;
export function orderPayload(productId) {
  _orderCounter++;
  const pid = productId || 3;
  return JSON.stringify({
    currency: "USD",
    items: [
      {
        productId: pid,
        quantity: 1,
        unitPrice: 29.99,
      },
    ],
  });
}

export function multiItemPayload(productIds) {
  const items = (productIds || [1, 2]).map((pid) => ({
    productId: pid,
    quantity: 1,
    unitPrice: 19.99,
  }));
  return JSON.stringify({ currency: "USD", items });
}

// ── Assertions ──
export function checkOk(res, label) {
  return check(res, {
    [`${label} status 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
}

export function checkAccepted(res, label) {
  return check(res, {
    [`${label} status 202`]: (r) => r.status === 202,
  });
}

// ── Login helper ──
// Returns a JWT token string; call in setup().
export function login(username, password) {
  const payload = JSON.stringify({
    username: username || __ENV.K6_USERNAME || "testuser",
    password: password || __ENV.K6_PASSWORD || "password123",
  });
  const res = http.post(`${GATEWAY}/api/auth/login`, payload, {
    headers: { "Content-Type": "application/json" },
  });
  if (res.status !== 200) {
    console.error(`Login failed: ${res.status} ${res.body}`);
    return null;
  }
  const body = JSON.parse(res.body);
  return body.data?.accessToken || null;
}

// ── RabbitMQ management API helpers ──
export function rabbitQueueDepth(queueName) {
  const res = http.get(`${RABBITMQ_API}/queues/%2F/${queueName}`, {
    headers: {
      Authorization: "Basic " + encoding.b64encode("techlab:techlab"),
    },
  });
  if (res.status !== 200) return -1;
  const q = JSON.parse(res.body);
  return q.messages || 0;
}

// ── Throttled sleep ──
export function think(minMs, maxMs) {
  const ms = Math.random() * ((maxMs || 500) - (minMs || 100)) + (minMs || 100);
  sleep(ms / 1000);
}
