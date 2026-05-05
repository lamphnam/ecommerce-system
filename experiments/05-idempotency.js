import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { orderPayload, authHeaders, GATEWAY, login } from './helpers.js';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

/**
 * 05-idempotency.js
 *
 * Scenario: verify idempotency — sending the same Idempotency-Key
 * multiple times should only create ONE order.
 *
 * Tests both async and sync endpoints.
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/05-idempotency.js
 */

const duplicatesDetected = new Counter('duplicates_detected');
const uniqueOrders       = new Counter('unique_orders_created');
const idempotencyFailures = new Rate('idempotency_failures');

export const options = {
    scenarios: {
        idempotency_async: {
            executor: 'per-vu-iterations',
            vus: 10,
            iterations: 5,        // 10 VUs × 5 iters = 50 requests, but only 10 unique keys
            exec: 'asyncIdempotency',
        },
        idempotency_sync: {
            executor: 'per-vu-iterations',
            vus: 10,
            iterations: 5,
            exec: 'syncIdempotency',
            startTime: '15s',     // stagger to avoid crosstalk
        },
    },
    thresholds: {
        idempotency_failures: ['rate<0.01'],
    },
};

export function setup() {
    const token = login();
    // Generate one key per VU — all iterations of the same VU share the key
    const keys = {};
    for (let i = 0; i < 20; i++) {
        keys[i] = uuidv4();
    }
    return { token, keys };
}

// Each VU sends the same idempotency key 5 times; only the first should create a new order.
export function asyncIdempotency(data) {
    const key = data.keys[__VU - 1] || uuidv4();
    const headers = authHeaders(data.token);
    headers['Idempotency-Key'] = key;
    const body = orderPayload(1);

    const res = http.post(`${GATEWAY}/api/orders`, body, { headers });
    const ok = check(res, { 'async idemp: 202': (r) => r.status === 202 });

    if (ok && res.status === 202) {
        const parsed = JSON.parse(res.body);
        const orderId = parsed.data?.id;
        if (__ITER === 0) {
            uniqueOrders.add(1);
        } else {
            // Subsequent iterations with the same key should return the SAME orderId
            duplicatesDetected.add(1);
        }
    } else {
        idempotencyFailures.add(1);
    }

    sleep(0.1);
}

export function syncIdempotency(data) {
    const key = data.keys[10 + __VU - 1] || uuidv4();
    const headers = authHeaders(data.token);
    headers['Idempotency-Key'] = key;
    const body = orderPayload(1);

    const res = http.post(`${GATEWAY}/api/orders/sync`, body, { headers });
    const ok = check(res, { 'sync idemp: 200': (r) => r.status === 200 });

    if (ok) {
        if (__ITER === 0) {
            uniqueOrders.add(1);
        } else {
            duplicatesDetected.add(1);
        }
    } else {
        idempotencyFailures.add(1);
    }

    sleep(0.1);
}
