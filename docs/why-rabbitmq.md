# Why RabbitMQ

## Problem

This system processes e-commerce orders through multiple business steps: order persistence, stock reservation, payment handling, notification dispatch, and analytics capture. The expected operating range is roughly 3000–5000 orders per day with projected 3–5x growth, while the team size remains small. The platform therefore needs a messaging backbone that supports reliable asynchronous workflows, simple routing between bounded services, dead-letter handling, and operational simplicity without imposing unnecessary platform complexity.

## Options

### RabbitMQ
- Mature broker with strong routing semantics (`topic`, DLQ, retry patterns)
- Good fit for work-queue and saga-style event choreography
- Straightforward local development and lightweight operations
- Native fit for the current Spring AMQP implementation

### Kafka
- Strong at high-throughput event streaming, partitioned logs, and replay-heavy analytics
- Excellent for very large scale event platforms
- Operationally heavier for a small team
- More complex than needed for the current request/compensation workflow

### ActiveMQ
- Traditional broker with JMS heritage
- Supports queues/topics and enterprise integration patterns
- Less aligned with the current Spring AMQP + topic-routing design than RabbitMQ
- Lower ecosystem momentum for this exact architecture choice compared with RabbitMQ or Kafka

## Decision matrix

| Criterion | RabbitMQ | Kafka | ActiveMQ |
|---|---|---|---|
| Saga/workflow routing | Strong | Moderate | Strong |
| Fine-grained routing keys | Strong | Weak | Moderate |
| DLQ handling | Strong | Moderate | Strong |
| Small-team operability | Strong | Weak | Moderate |
| Fit for 3k–25k orders/day | Strong | Strong | Moderate |
| Spring Boot integration for current codebase | Strong | Moderate | Moderate |
| Analytics fan-out needs | Strong enough | Strong | Moderate |
| Replay/stream retention at large scale | Moderate | Strong | Weak |

## Supporting evidence

The actual implementation already uses RabbitMQ in ways that match the system’s needs:
- Domain-specific topic exchanges: `order.exchange`, `payment.exchange`, `inventory.exchange`, `notification.exchange`
- Dedicated consumer queues per workflow step, such as `inventory.reserve.q`, `payment.process.q`, and `order.payment-failed.q`
- DLQ topology for every primary consumer queue via paired `*.dlx` and `*.dlq`
- Idempotent event consumption with `processed_events` tables in every consumer service
- Analytics fan-in implemented with wildcard bindings from all domain exchanges into `analytics.events.q`
- Manual acknowledgement listeners so business failures and poison-message handling can be controlled explicitly

This is a strong architectural fit for the actual business process:
- Order processing is a multi-step saga, not just a firehose stream
- Compensation matters (`inventory.release.requested`)
- Point-to-point workflow semantics matter more than partitioned log replay
- The system needs practical operations and clear failure isolation more than extreme-scale streaming primitives

The observed load-test behavior also supports the design choice. In the real normal-load baseline, async order creation delivered `115.8ms` p95 latency versus `740.81ms` for the synchronous baseline, while both remained functionally correct. That demonstrates the concrete user-facing benefit of queue-backed decoupling in this codebase: the client request is not blocked on downstream payment execution.

## Conclusion

RabbitMQ is the best fit for this system because it matches both the current scale and the actual workflow shape. The business problem is an orchestrated, failure-aware order saga with compensation and per-step routing — not a high-volume event streaming platform that requires Kafka’s log abstraction. For a small team expecting moderate growth, RabbitMQ provides the right balance of routing power, operational simplicity, dead-letter support, and clean Spring Boot integration. Given the existing code, topology, and measured async baseline improvement, RabbitMQ is the most appropriate choice for the current architecture.
