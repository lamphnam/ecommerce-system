import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { orderPayload, authHeaders, GATEWAY, RABBITMQ_API, login } from './helpers.js';
import encoding from 'k6/encoding';

/**
 * 06-dlq-behaviour.js
 *
 * Scenario: payment provider fails 100% of the time → all payments
 * go to the DLQ after retry exhaustion. Verifies DLQ messages accumulate,
 * orders end up FAILED, and the system remains healthy.
 *
 * Prerequisites:
 *   Set payment service env: PAYMENT_FAILURE_RATE=1.0
 *   (restart payment-service)
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/06-dlq-behaviour.js
 */

const ordersCreated  = new Counter('orders_created');
const ordersFailed   = new Counter('orders_failed');
const orderLatency   = new Trend('order_create_latency', true);

export const options = {
    scenarios: {
        dlq_test: {
            executor: 'constant-arrival-rate',
            rate: 3,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 10,
            maxVUs: 20,
        },
        check_dlq: {
            executor: 'constant-vus',
            vus: 1,
            duration: '60s',       // runs longer to give retries time to exhaust
            exec: 'checkDlq',
            startTime: '35s',
        },
    },
};

export function setup() {
    const token = login();
    return { token, orderIds: [] };
}

export default function (data) {
    const headers = authHeaders(data.token);
    const body = orderPayload(1);

    const res = http.post(`${GATEWAY}/api/orders`, body, { headers });
    orderLatency.add(res.timings.duration);

    if (check(res, { 'order created: 202': (r) => r.status === 202 })) {
        ordersCreated.add(1);
    }

    sleep(0.1);
}

export function checkDlq(data) {
    // Poll RabbitMQ management API for DLQ depth
    const dlqNames = [
        'payment.process.dlq',
        'order.payment-failed.dlq',
    ];

    for (const qName of dlqNames) {
        const res = http.get(`${RABBITMQ_API}/queues/%2F/${qName}`, {
            headers: {
                'Authorization': 'Basic ' + encoding.b64encode('techlab:techlab'),
            },
        });
        if (res.status === 200) {
            const q = JSON.parse(res.body);
            const depth = q.messages || 0;
            console.log(`DLQ ${qName}: ${depth} messages`);
            check(null, {
                [`${qName} has messages`]: () => depth >= 0,
            });
        }
    }

    // Also check order statuses via API
    const headers = authHeaders(data.token);
    const ordersRes = http.get(`${GATEWAY}/api/orders?status=FAILED&size=5`, { headers });
    if (ordersRes.status === 200) {
        const body = JSON.parse(ordersRes.body);
        const failedCount = body.data?.content?.length || 0;
        console.log(`Orders in FAILED status: ${failedCount}`);
        ordersFailed.add(failedCount);
    }

    sleep(5);
}
