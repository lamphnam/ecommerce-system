import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { orderPayload, authHeaders, GATEWAY, RABBITMQ_API, login } from './helpers.js';
import encoding from 'k6/encoding';

/**
 * 07-recovery.js
 *
 * Scenario: simulate payment-service downtime and recovery.
 *
 * Phase 1 (0-30s):   Send async orders — payment-service is running normally.
 * Phase 2 (30-60s):  Stop payment-service manually; orders still accepted (202)
 *                     but messages queue up in RabbitMQ.
 * Phase 3 (60-90s):  Restart payment-service; queued messages drain and orders
 *                     reach terminal states.
 *
 * This script only sends orders and monitors queue depth. You must manually
 * stop/start payment-service at the indicated times.
 *
 * Run:
 *   k6 run --env GATEWAY_URL=http://localhost:8080 experiments/07-recovery.js
 *
 * Manual steps during the test:
 *   At ~30s: stop payment-service (Ctrl+C or docker stop)
 *   At ~60s: restart payment-service
 */

const orderLatency  = new Trend('order_create_latency', true);
const orderErrors   = new Rate('order_errors');
const queueDepth    = new Trend('payment_queue_depth');

export const options = {
    scenarios: {
        order_flow: {
            executor: 'constant-arrival-rate',
            rate: 5,
            timeUnit: '1s',
            duration: '90s',
            preAllocatedVUs: 15,
            maxVUs: 30,
        },
        queue_monitor: {
            executor: 'constant-vus',
            vus: 1,
            duration: '120s',      // monitor a bit longer than order flow
            exec: 'monitorQueue',
        },
    },
};

export function setup() {
    const token = login();
    console.log('=== RECOVERY TEST ===');
    console.log('At ~30s: STOP payment-service');
    console.log('At ~60s: RESTART payment-service');
    console.log('Watch the payment.process.q depth rise and fall.');
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);
    const body = orderPayload(1);

    const res = http.post(`${GATEWAY}/api/orders`, body, { headers });
    orderLatency.add(res.timings.duration);
    orderErrors.add(res.status >= 400 ? 1 : 0);

    check(res, {
        'order: 202 accepted': (r) => r.status === 202,
    });

    sleep(0.05);
}

export function monitorQueue() {
    const queues = ['payment.process.q', 'inventory.reserve.q'];

    for (const qName of queues) {
        const res = http.get(`${RABBITMQ_API}/queues/%2F/${qName}`, {
            headers: {
                'Authorization': 'Basic ' + encoding.b64encode('techlab:techlab'),
            },
        });
        if (res.status === 200) {
            const q = JSON.parse(res.body);
            const depth = q.messages || 0;
            if (qName === 'payment.process.q') {
                queueDepth.add(depth);
            }
            console.log(`[${new Date().toISOString()}] ${qName}: ${depth} msgs`);
        }
    }

    sleep(3);
}
