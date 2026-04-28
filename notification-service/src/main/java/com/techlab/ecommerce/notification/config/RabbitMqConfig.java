package com.techlab.ecommerce.notification.config;

import org.springframework.context.annotation.Configuration;

/**
 * Notification Service exchange/queue/binding declarations.
 *
 * <p>Populated in <strong>Phase 4</strong> of the implementation roadmap.
 *
 * <ul>
 *   <li>Publishes to: {@code analytics.exchange} (delivery status events).</li>
 *   <li>Consumes from queue {@code notification.send.q}
 *       (bound to {@code notification.exchange} with routing key {@code notification.requested}),
 *       with DLQ {@code notification.send.dlq}.</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {
}
