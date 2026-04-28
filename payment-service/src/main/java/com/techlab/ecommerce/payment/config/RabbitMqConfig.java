package com.techlab.ecommerce.payment.config;

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
 * Payment Service RabbitMQ topology.
 *
 * <h3>Publishes to</h3>
 * <ul>
 *   <li>{@code payment.exchange} — {@code payment.succeeded}, {@code payment.failed}</li>
 *   <li>{@code analytics.exchange} — {@code analytics.event}</li>
 * </ul>
 *
 * <h3>Consumes</h3>
 * <ul>
 *   <li>{@code payment.process.q} ← {@code payment.exchange / payment.requested}</li>
 * </ul>
 */
@Configuration
public class RabbitMqConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange";
    private static final String DLK_ARG = "x-dead-letter-routing-key";

    @Bean
    Declarables paymentExchanges() {
        return new Declarables(
                ExchangeBuilder.topicExchange(ExchangeNames.PAYMENT).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.ANALYTICS).durable(true).build(),
                ExchangeBuilder.topicExchange(ExchangeNames.PAYMENT_DLX).durable(true).build());
    }

    @Bean
    Declarables paymentConsumerQueues() {
        return new Declarables(
                QueueBuilder.durable(QueueNames.PAYMENT_PROCESS)
                        .withArgument(DLX_ARG, ExchangeNames.PAYMENT_DLX)
                        .withArgument(DLK_ARG, QueueNames.PAYMENT_PROCESS_DLQ)
                        .build(),
                QueueBuilder.durable(QueueNames.PAYMENT_PROCESS_DLQ).build());
    }

    @Bean
    Declarables paymentBindings() {
        return new Declarables(
                new Binding(QueueNames.PAYMENT_PROCESS, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT, RoutingKeys.PAYMENT_REQUESTED, null),
                new Binding(QueueNames.PAYMENT_PROCESS_DLQ, DestinationType.QUEUE,
                        ExchangeNames.PAYMENT_DLX, QueueNames.PAYMENT_PROCESS_DLQ, null));
    }
}
