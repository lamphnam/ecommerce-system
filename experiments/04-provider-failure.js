import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { orderPayload, authHeaders, GATEWAY, login, think } from './helpers.js';

/**
 * 04-provider-failure.js
 *
 * Scenario: payment provider fails 30% of the time.
 * Demonstrates async resilience (retry queues, DLQ) vs sync failures
 * that propagate directly to the caller.
 *
 * Prerequisites:
 *   Set payment service env: PAYMENT_FAILURE_RATE=0.3
 *   (restart payment-service with this env var)
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/04-provider-failure.js
 */

const asyncLatency = new Trend('async_order_latency', true);
const syncLatency  = new Trend('sync_order_latency', true);
const asyncErrors  = new Rate('async_order_errors');
const syncErrors   = new Rate('sync_order_errors');
const syncFailures = new Counter('sync_payment_failures');

export const options = {
    scenarios: {
        failure_test: {
            executor: 'constant-arrival-rate',
            rate: 8,
            timeUnit: '1s',
            duration: '60s',
            preAllocatedVUs: 20,
            maxVUs: 50,
        },
    },
    thresholds: {
        async_order_latency: ['p(95)<500'],
        async_order_errors:  ['rate<0.05'],   // async itself shouldn't error
        // Sync errors expected (30% payment failure → ~15% of sync orders fail)
        sync_order_errors:   ['rate<0.50'],
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
        });
        syncLatency.add(res.timings.duration);
        const isError = res.status >= 400;
        syncErrors.add(isError ? 1 : 0);
        if (isError) syncFailures.add(1);

        // Sync returns 200 with FAILED status OR CONFIRMED
        if (res.status === 200) {
            const body = JSON.parse(res.body);
            const orderStatus = body.data?.status;
            check(res, {
                'sync: order has terminal status': () =>
                    orderStatus === 'CONFIRMED' || orderStatus === 'FAILED',
            });
        }
    }

    think(50, 200);
}
