package com.techlab.ecommerce.analytics.config;

import org.springframework.context.annotation.Configuration;

/**
 * Analytics Service exchange/queue/binding declarations.
 *
 * <p>Populated in <strong>Phase 4</strong> of the implementation roadmap.
 *
 * <ul>
 *   <li>Publishes nothing.</li>
 *   <li>Consumes from queue {@code analytics.events.q}, fan-out from
 *       {@code order.exchange}, {@code payment.exchange}, {@code inventory.exchange},
 *       {@code notification.exchange}, {@code analytics.exchange} via wildcard bindings (#).</li>
 *   <li>Dedicated thread pool with high prefetch so analytics never blocks the critical path.</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {
}
