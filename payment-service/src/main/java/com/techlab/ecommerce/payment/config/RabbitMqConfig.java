package com.techlab.ecommerce.payment.config;

import org.springframework.context.annotation.Configuration;

/**
 * Payment Service exchange/queue/binding declarations.
 *
 * <p>Populated in <strong>Phase 4</strong> of the implementation roadmap.
 *
 * <ul>
 *   <li>Publishes to: {@code payment.exchange}, {@code analytics.exchange}.</li>
 *   <li>Consumes from queue {@code payment.process.q}
 *       (bound to {@code payment.exchange} with routing key {@code payment.requested}),
 *       with DLQ {@code payment.process.dlq}.</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {
}
