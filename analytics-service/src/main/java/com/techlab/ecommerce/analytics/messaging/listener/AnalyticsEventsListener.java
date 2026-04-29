package com.techlab.ecommerce.analytics.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.analytics.service.AnalyticsIngestionService;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AnalyticsEventsListener {

    private final AnalyticsIngestionService ingestionService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = QueueNames.ANALYTICS_EVENTS)
    public void onAnalyticsEvent(EventEnvelope<Object> event,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                 @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
                                 @Header(value = AmqpHeaders.RECEIVED_EXCHANGE, required = false) String exchangeName)
            throws IOException {
        ackHandler.handle(event, channel, deliveryTag, routingKey,
                envelope -> ingestionService.handleEvent(envelope, routingKey, exchangeName));
    }
}
