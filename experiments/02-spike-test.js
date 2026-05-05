import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { orderPayload, authHeaders, GATEWAY, login, think } from './helpers.js';

/**
 * 02-spike-test.js
 *
 * Scenario: traffic spike from low → high → sustain → ramp down.
 * Tests how async and sync endpoints behave when concurrency jumps
 * from 5 VUs to 200 VUs for 60 seconds, then drops back.
 *
 * Key metrics: does async maintain low p95 under spike?
 * Does sync start timing out or returning errors?
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/02-spike-test.js
 */

const asyncLatency = new Trend('async_order_latency', true);
const syncLatency  = new Trend('sync_order_latency', true);
const asyncErrors  = new Rate('async_order_errors');
const syncErrors   = new Rate('sync_order_errors');

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { duration: '10s', target: 5 },     // warm up
                { duration: '10s', target: 200 },    // spike up
                { duration: '60s', target: 200 },    // sustain peak
                { duration: '10s', target: 5 },      // ramp down
                { duration: '10s', target: 5 },      // cool down
            ],
        },
    },
    thresholds: {
        async_order_latency: ['p(95)<1000'],
        sync_order_latency:  ['p(95)<15000'],
        async_order_errors:  ['rate<0.10'],
        sync_order_errors:   ['rate<0.30'],   // sync expected to struggle
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
        check(res, { 'async: 2xx': (r) => r.status >= 200 && r.status < 300 });
    } else {
        const res = http.post(`${GATEWAY}/api/orders/sync`, body, {
            headers,
            tags: { endpoint: 'sync' },
        });
        syncLatency.add(res.timings.duration);
        syncErrors.add(res.status >= 400 ? 1 : 0);
        check(res, { 'sync: 2xx': (r) => r.status >= 200 && r.status < 300 });
    }

    think(20, 100);
}
