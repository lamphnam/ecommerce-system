package com.techlab.ecommerce.order.config;

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
 * Order Service RabbitMQ topology.
 *
 * <h3>Publishes to (saga orchestrator)</h3>
 * <ul>
 *   <li>{@code order.exchange} — {@code order.created}, {@code order.confirmed}, {@code order.failed}, {@code order.cancelled}</li>
 *   <li>{@code payment.exchange} — {@code payment.requested}</li>
 *   <li>{@code inventory.exchange} — {@code inventory.release.requested} (compensation)</li>
 *   <li>{@code notification.exchange} — {@code notification.requested}</li>
 * </ul>
 *
 * <p>Domain events automatically reach analytics-service via wildcard bindings
 * on {@code order.exchange}; do not publish a duplicate {@code analytics.event}
 * message. See {@link com.techlab.ecommerce.common.messaging.constants.ExchangeNames}
 * for the analytics strategy.
 *
 * <h3>Consumes (saga reactions)</h3>
 * <ul>
 *   <li>{@code order.inventory-reserved.q} ← {@code inventory.exchange / inventory.reserved}</li>
 *   <li>{@code order.inventory-failed.q}   ← {@code inventory.exchange / inventory.failed}</li>
 *   <li>{@code order.payment-succeeded.q}  ← {@code payment.exchange / payment.succeeded}</li>
 *   <li>{@code order.payment-failed.q}     ← {@code payment.exchange / payment.failed}</li>
 * </ul>
 *
 * <p>Each consumer queue dead-letters into its source exchange's DLX with the DLQ
 * name as routing key. Exchange/queue declarations are idempotent in RabbitMQ,
 * so it doesn't matter which service boots first.
 */
@Configuration
public class RabbitMqConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange";
    private static final String DLK_ARG = "x-dead-letter-routing-key";

    @Bean
    Declarables orderExchanges() {
        return new Declarables(
                ExchangeBuilder.topicExchange(ExchangeNames.ORDER).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.PAYMENT).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.INVENTORY).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.NOTIFICATION).durable(true).build(),
                // DLXes for the queues this service consumes from
                ExchangeBuilder.topicExchange(ExchangeNames.PAYMENT_DLX).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.INVENTORY_DLX).durable(true).build());
    }

    @Bean
    Declarables orderConsumerQueues() {
        return new Declarables(
                QueueBuilder.durable(QueueNames.ORDER_INVENTORY_RESERVED)
                        .withArgument(DLX_ARG, ExchangeNames.INVENTORY_DLX)
                        .withArgument(DLK_ARG, QueueNames.ORDER_INVENTORY_RESERVED_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.ORDER_INVENTORY_RESERVED_DLQ).build(),

                QueueBuilder.durable(QueueNames.ORDER_INVENTORY_FAILED)
                        .withArgument(DLX_ARG, ExchangeNames.INVENTORY_DLX)
                        .withArgument(DLK_ARG, QueueNames.ORDER_INVENTORY_FAILED_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.ORDER_INVENTORY_FAILED_DLQ).build(),

                QueueBuilder.durable(QueueNames.ORDER_PAYMENT_SUCCEEDED)
                        .withArgument(DLX_ARG, ExchangeNames.PAYMENT_DLX)
                        .withArgument(DLK_ARG, QueueNames.ORDER_PAYMENT_SUCCEEDED_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.ORDER_PAYMENT_SUCCEEDED_DLQ).build(),

                QueueBuilder.durable(QueueNames.ORDER_PAYMENT_FAILED)
                        .withArgument(DLX_ARG, ExchangeNames.PAYMENT_DLX)
                        .withArgument(DLK_ARG, QueueNames.ORDER_PAYMENT_FAILED_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.ORDER_PAYMENT_FAILED_DLQ).build());
    }

    @Bean
    Declarables orderBindings() {
        return new Declarables(
                // Primary bindings
                new Binding(QueueNames.ORDER_INVENTORY_RESERVED, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY, RoutingKeys.INVENTORY_RESERVED, null),
                new Binding(QueueNames.ORDER_INVENTORY_FAILED, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY, RoutingKeys.INVENTORY_FAILED, null),
                new Binding(QueueNames.ORDER_PAYMENT_SUCCEEDED, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT, RoutingKeys.PAYMENT_SUCCEEDED, null),
                new Binding(QueueNames.ORDER_PAYMENT_FAILED, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT, RoutingKeys.PAYMENT_FAILED, null),

                // DLQ bindings (DLX -> DLQ)
                new Binding(QueueNames.ORDER_INVENTORY_RESERVED_DLQ, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY_DLX, QueueNames.ORDER_INVENTORY_RESERVED_DLQ, null),
                new Binding(QueueNames.ORDER_INVENTORY_FAILED_DLQ, DestinationType.QUEUE,
                        ExchangeNames.INVENTORY_DLX, QueueNames.ORDER_INVENTORY_FAILED_DLQ, null),
                new Binding(QueueNames.ORDER_PAYMENT_SUCCEEDED_DLQ, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT_DLX, QueueNames.ORDER_PAYMENT_SUCCEEDED_DLQ, null),
                new Binding(QueueNames.ORDER_PAYMENT_FAILED_DLQ, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT_DLX, QueueNames.ORDER_PAYMENT_FAILED_DLQ, null));
    }
}
