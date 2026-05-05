import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { orderPayload, authHeaders, GATEWAY, login, think } from './helpers.js';

/**
 * 03-slow-provider.js
 *
 * Scenario: payment provider has high latency (2-3 seconds).
 * Demonstrates that async orders return 202 instantly while sync
 * orders are blocked waiting for the slow provider.
 *
 * Prerequisites:
 *   Set payment service env: PAYMENT_LATENCY_MS=2500
 *   (restart payment-service with this env var)
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/03-slow-provider.js
 */

const asyncLatency = new Trend('async_order_latency', true);
const syncLatency  = new Trend('sync_order_latency', true);
const asyncErrors  = new Rate('async_order_errors');
const syncErrors   = new Rate('sync_order_errors');

export const options = {
    scenarios: {
        slow_provider: {
            executor: 'constant-arrival-rate',
            rate: 5,
            timeUnit: '1s',
            duration: '60s',
            preAllocatedVUs: 30,
            maxVUs: 100,
        },
    },
    thresholds: {
        // Async should still be sub-500ms (just DB + publish)
        async_order_latency: ['p(95)<500'],
        // Sync will be 2.5s+ (blocked on payment)
        sync_order_latency:  ['p(50)>1000'],
        async_order_errors:  ['rate<0.05'],
    },
};

export function setup() {
    const token = login();
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);
    const body = orderPayload(1);

    if (__ITER % 2 === 0) {
        const res = http.post(`${GATEWAY}/api/orders`, body, {
            headers,
            tags: { endpoint: 'async' },
        });
        asyncLatency.add(res.timings.duration);
        asyncErrors.add(res.status >= 400 ? 1 : 0);
        check(res, { 'async: 202': (r) => r.status === 202 });
    } else {
        const res = http.post(`${GATEWAY}/api/orders/sync`, body, {
            headers,
            tags: { endpoint: 'sync' },
            timeout: '30s',
        });
        syncLatency.add(res.timings.duration);
        syncErrors.add(res.status >= 400 ? 1 : 0);
        check(res, { 'sync: 2xx': (r) => r.status >= 200 && r.status < 300 });
    }

    think(50, 200);
}
