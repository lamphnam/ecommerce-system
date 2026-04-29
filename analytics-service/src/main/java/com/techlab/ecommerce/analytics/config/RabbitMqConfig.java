package com.techlab.ecommerce.analytics.config;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Analytics Service RabbitMQ topology.
 *
 * <p>Pure fan-out consumer. Analytics is collected directly from the
 * <em>domain exchanges</em> via {@code #} wildcard bindings, so every business
 * event recorded by Order/Payment/Inventory/Notification flows into
 * {@code analytics.events.q} without any extra publish on the producing side.
 * Domain services therefore must <strong>not</strong> emit a separate
 * {@code analytics.event} message for the same fact — see
 * {@link com.techlab.ecommerce.common.messaging.constants.ExchangeNames} for the
 * full strategy.
 *
 * <p>{@code analytics.exchange} is owned by this service and reserved for future
 * analytics-internal events (re-emitted aggregates, replays). The wildcard
 * binding from it is kept so that, if such events are introduced later, no
 * topology change is required. Producers never block on this queue (manual ack
 * + dedicated thread pool will be wired up in Phase 9).
 *
 * <h3>Publishes</h3>
 * Nothing.
 *
 * <h3>Consumes</h3>
 * <ul>
 *   <li>{@code analytics.events.q} ← {@code order.exchange / #} (domain)</li>
 *   <li>{@code analytics.events.q} ← {@code payment.exchange / #} (domain)</li>
 *   <li>{@code analytics.events.q} ← {@code inventory.exchange / #} (domain)</li>
 *   <li>{@code analytics.events.q} ← {@code notification.exchange / #} (domain)</li>
 *   <li>{@code analytics.events.q} ← {@code analytics.exchange / #} (reserved, internal)</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange";
    private static final String DLK_ARG = "x-dead-letter-routing-key";
    private static final String CATCH_ALL = "#";

    @Bean
    Declarables analyticsExchanges() {
        return new Declarables(
                ExchangeBuilder.topicExchange(ExchangeNames.ORDER).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.PAYMENT).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.INVENTORY).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.NOTIFICATION).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.ANALYTICS).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.ANALYTICS_DLX).durable(true).build());
    }

    @Bean
    Declarables analyticsConsumerQueues() {
        return new Declarables(
                QueueBuilder.durable(QueueNames.ANALYTICS_EVENTS)
                        .withArgument(DLX_ARG, ExchangeNames.ANALYTICS_DLX)
                        .withArgument(DLK_ARG, QueueNames.ANALYTICS_EVENTS_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.ANALYTICS_EVENTS_DLQ).build());
    }

    @Bean
    Declarables analyticsBindings() {
        return new Declarables(
                new Binding(QueueNames.ANALYTICS_EVENTS, DestinationType.QUEUE,
                        ExchangeNames.ORDER, CATCH_ALL, null),
                new Binding(QueueNames.ANALYTICS_EVENTS, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT, CATCH_ALL, null),
                new Binding(QueueNames.ANALYTICS_EVENTS, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY, CATCH_ALL, null),
                new Binding(QueueNames.ANALYTICS_EVENTS, DestinationType.QUEUE,
                        ExchangeNames.NOTIFICATION, CATCH_ALL, null),
                new Binding(QueueNames.ANALYTICS_EVENTS, DestinationType.QUEUE,
                        ExchangeNames.ANALYTICS, CATCH_ALL, null),

                new Binding(QueueNames.ANALYTICS_EVENTS_DLQ, DestinationType.QUEUE,
                        ExchangeNames.ANALYTICS_DLX, QueueNames.ANALYTICS_EVENTS_DLQ, null));
    }
}
