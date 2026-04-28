package com.techlab.ecommerce.inventory.config;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Inventory Service RabbitMQ topology.
 *
 * <h3>Publishes to</h3>
 * <ul>
 *   <li>{@code inventory.exchange} — {@code inventory.reserved}, {@code inventory.failed}, {@code inventory.released}</li>
 *   <li>{@code analytics.exchange} — {@code analytics.event}</li>
 * </ul>
 *
 * <h3>Consumes</h3>
 * <ul>
 *   <li>{@code inventory.reserve.q} ← {@code order.exchange / order.created} (forward)</li>
 *   <li>{@code inventory.release.q} ← {@code inventory.exchange / inventory.release.requested} (compensation)</li>
 * </ul>
 *
 * <p>{@code inventory.reserve.q} dead-letters into {@code order.exchange.dlx} because its
 * source is the Order exchange. {@code inventory.release.q} dead-letters into
 * {@code inventory.exchange.dlx}.
 */
@Configuration
public class RabbitMqConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange";
    private static final String DLK_ARG = "x-dead-letter-routing-key";

    @Bean
    Declarables inventoryExchanges() {
        return new Declarables(
                ExchangeBuilder.topicExchange(ExchangeNames.ORDER).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.INVENTORY).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.ANALYTICS).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.ORDER_DLX).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.INVENTORY_DLX).durable(true).build());
    }

    @Bean
    Declarables inventoryConsumerQueues() {
        return new Declarables(
                QueueBuilder.durable(QueueNames.INVENTORY_RESERVE)
                        .withArgument(DLX_ARG, ExchangeNames.ORDER_DLX)
                        .withArgument(DLK_ARG, QueueNames.INVENTORY_RESERVE_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.INVENTORY_RESERVE_DLQ).build(),

                QueueBuilder.durable(QueueNames.INVENTORY_RELEASE)
                        .withArgument(DLX_ARG, ExchangeNames.INVENTORY_DLX)
                        .withArgument(DLK_ARG, QueueNames.INVENTORY_RELEASE_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.INVENTORY_RELEASE_DLQ).build());
    }

    @Bean
    Declarables inventoryBindings() {
        return new Declarables(
                new Binding(QueueNames.INVENTORY_RESERVE, DestinationType.QUEUE,
                        ExchangeNames.ORDER, RoutingKeys.ORDER_CREATED, null),
                new Binding(QueueNames.INVENTORY_RELEASE, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY, RoutingKeys.INVENTORY_RELEASE_REQUESTED, null),

                new Binding(QueueNames.INVENTORY_RESERVE_DLQ, DestinationType.QUEUE,
                        ExchangeNames.ORDER_DLX, QueueNames.INVENTORY_RESERVE_DLQ, null),
                new Binding(QueueNames.INVENTORY_RELEASE_DLQ, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY_DLX, QueueNames.INVENTORY_RELEASE_DLQ, null));
    }
}
