package com.techlab.ecommerce.notification.config;

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
 * Notification Service RabbitMQ topology.
 *
 * <h3>Publishes to</h3>
 * <ul>
 *   <li>{@code notification.exchange} — {@code notification.sent}, {@code notification.failed}
 *       (delivery status emitted after handling each {@code notification.requested})</li>
 * </ul>
 *
 * <p>Delivery-status events reach analytics-service via the wildcard binding on
 * {@code notification.exchange}; do not publish a duplicate
 * {@code analytics.event} message. See
 * {@link com.techlab.ecommerce.common.messaging.constants.ExchangeNames} for the
 * analytics strategy.
 *
 * <h3>Consumes</h3>
 * <ul>
 *   <li>{@code notification.send.q} ← {@code notification.exchange / notification.requested}</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange";
    private static final String DLK_ARG = "x-dead-letter-routing-key";

    @Bean
    Declarables notificationExchanges() {
        return new Declarables(
                ExchangeBuilder.topicExchange(ExchangeNames.NOTIFICATION).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.NOTIFICATION_DLX).durable(true).build());
    }

    @Bean
    Declarables notificationConsumerQueues() {
        return new Declarables(
                QueueBuilder.durable(QueueNames.NOTIFICATION_SEND)
                        .withArgument(DLX_ARG, ExchangeNames.NOTIFICATION_DLX)
                        .withArgument(DLK_ARG, QueueNames.NOTIFICATION_SEND_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.NOTIFICATION_SEND_DLQ).build());
    }

    @Bean
    Declarables notificationBindings() {
        return new Declarables(
                new Binding(QueueNames.NOTIFICATION_SEND, DestinationType.QUEUE,
                        ExchangeNames.NOTIFICATION, RoutingKeys.NOTIFICATION_REQUESTED, null),
                new Binding(QueueNames.NOTIFICATION_SEND_DLQ, DestinationType.QUEUE,
                        ExchangeNames.NOTIFICATION_DLX, QueueNames.NOTIFICATION_SEND_DLQ, null));
    }
}
