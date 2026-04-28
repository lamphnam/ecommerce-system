package com.techlab.ecommerce.inventory.config;

import org.springframework.context.annotation.Configuration;

/**
 * Inventory Service exchange/queue/binding declarations.
 *
 * <p>Populated in <strong>Phase 4</strong> of the implementation roadmap.
 *
 * <ul>
 *   <li>Publishes to: {@code inventory.exchange}, {@code analytics.exchange}.</li>
 *   <li>Consumes from queues:
 *       <ul>
 *         <li>{@code inventory.reserve.q} bound to {@code order.exchange}
 *             with routing key {@code order.created} (DLQ {@code inventory.reserve.dlq}).</li>
 *         <li>{@code inventory.release.q} bound to {@code inventory.exchange}
 *             with routing key {@code inventory.release.requested}
 *             (DLQ {@code inventory.release.dlq}).</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {
}
