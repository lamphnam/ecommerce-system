package com.techlab.ecommerce.order.config;

import org.springframework.context.annotation.Configuration;

/**
 * Order Service exchange/queue/binding declarations.
 *
 * <p>Populated in <strong>Phase 4</strong> of the implementation roadmap (RabbitMQ infra).
 * For now this class only documents what will be declared here:
 *
 * <ul>
 *   <li>Publishes to: {@code order.exchange}, {@code payment.exchange},
 *       {@code inventory.exchange}, {@code notification.exchange}, {@code analytics.exchange}.</li>
 *   <li>Consumes from queues:
 *       {@code order.inventory-reserved.q},
 *       {@code order.inventory-failed.q},
 *       {@code order.payment-succeeded.q},
 *       {@code order.payment-failed.q}.</li>
 *   <li>Each consumer queue is bound with x-dead-letter-exchange to
 *       {@code <name>.exchange.dlx} and a sibling {@code *.dlq} for poison messages.</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {
}
