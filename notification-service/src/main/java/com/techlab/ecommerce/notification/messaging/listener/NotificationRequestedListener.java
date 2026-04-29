package com.techlab.ecommerce.notification.messaging.listener;

import com.rabbitmq.client.Channel;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.notification.messaging.payload.inbound.NotificationRequestedPayload;
import com.techlab.ecommerce.notification.service.NotificationSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NotificationRequestedListener {

    private final NotificationSagaService sagaService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = QueueNames.NOTIFICATION_SEND)
    public void onNotificationRequested(EventEnvelope<NotificationRequestedPayload> event,
                                        Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        ackHandler.handle(event, channel, deliveryTag,
                RoutingKeys.NOTIFICATION_REQUESTED, sagaService::handleNotificationRequested);
    }
}
