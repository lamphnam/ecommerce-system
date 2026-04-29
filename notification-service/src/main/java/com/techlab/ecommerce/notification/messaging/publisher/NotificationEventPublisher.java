package com.techlab.ecommerce.notification.messaging.publisher;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.notification.entity.Notification;
import com.techlab.ecommerce.notification.messaging.EventEnvelopeFactory;
import com.techlab.ecommerce.notification.messaging.payload.NotificationFailedPayload;
import com.techlab.ecommerce.notification.messaging.payload.NotificationSentPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Typed publisher for Notification Service domain events. It publishes only to
 * notification.exchange; analytics-service observes notification.exchange via
 * wildcard binding, so no duplicate analytics.event message is emitted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventEnvelopeFactory envelopeFactory;

    public void publishNotificationSent(EventEnvelope<?> inbound, Notification notification) {
        NotificationSentPayload payload = NotificationSentPayload.builder()
                .notificationId(notification.getId())
                .userId(notification.getUserId())
                .orderId(notification.getOrderId())
                .channel(notification.getChannel())
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .build();
        EventEnvelope<NotificationSentPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.NOTIFICATION_SENT, payload, inbound);
        send(RoutingKeys.NOTIFICATION_SENT, envelope);
    }

    public void publishNotificationFailed(EventEnvelope<?> inbound, Notification notification, String reason) {
        NotificationFailedPayload payload = NotificationFailedPayload.builder()
                .notificationId(notification.getId())
                .userId(notification.getUserId())
                .orderId(notification.getOrderId())
                .channel(notification.getChannel())
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .reason(reason)
                .build();
        EventEnvelope<NotificationFailedPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.NOTIFICATION_FAILED, payload, inbound);
        send(RoutingKeys.NOTIFICATION_FAILED, envelope);
    }

    private <T> void send(String routingKey, EventEnvelope<T> envelope) {
        log.debug("Publishing {} (eventId={}, correlationId={}) → {}",
                envelope.getEventType(), envelope.getEventId(), envelope.getCorrelationId(), ExchangeNames.NOTIFICATION);
        rabbitTemplate.convertAndSend(ExchangeNames.NOTIFICATION, routingKey, envelope);
    }
}
