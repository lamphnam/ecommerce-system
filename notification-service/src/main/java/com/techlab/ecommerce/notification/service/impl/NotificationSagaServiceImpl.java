package com.techlab.ecommerce.notification.service.impl;

import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.common.util.JsonUtils;
import com.techlab.ecommerce.notification.entity.Notification;
import com.techlab.ecommerce.notification.enums.NotificationChannel;
import com.techlab.ecommerce.notification.enums.NotificationErrorMessage;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import com.techlab.ecommerce.notification.exception.NotificationException;
import com.techlab.ecommerce.notification.messaging.payload.inbound.NotificationRequestedPayload;
import com.techlab.ecommerce.notification.messaging.publisher.NotificationEventPublisher;
import com.techlab.ecommerce.notification.repository.NotificationRepository;
import com.techlab.ecommerce.notification.service.IdempotentEventProcessor;
import com.techlab.ecommerce.notification.service.NotificationSagaService;
import com.techlab.ecommerce.notification.simulator.NotificationDeliveryResult;
import com.techlab.ecommerce.notification.simulator.NotificationProviderSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notification side effects are intentionally non-critical. Simulated delivery
 * failures persist FAILED, publish {@code notification.failed}, and ack the
 * inbound message so the order saga is never blocked by email/push problems.
 * Unexpected infrastructure/programming failures escape the transaction; the
 * manual ack wrapper nacks without requeue so RabbitMQ dead-letters the message.
 *
 * <p>Phase-8 simplification: events are published inside the transaction. A
 * transactional outbox is the production-grade next improvement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSagaServiceImpl implements NotificationSagaService {

    private static final NotificationChannel DEFAULT_CHANNEL = NotificationChannel.EMAIL;

    private final IdempotentEventProcessor idempotency;
    private final NotificationRepository notificationRepository;
    private final NotificationProviderSimulator simulator;
    private final NotificationEventPublisher publisher;

    @Override
    @Transactional
    public void handleNotificationRequested(EventEnvelope<NotificationRequestedPayload> event) {
        idempotency.markProcessed(event, QueueNames.NOTIFICATION_SEND);

        NotificationRequestedPayload payload = requirePayload(event);
        Notification notification = buildNotification(event, payload);
        notification = notificationRepository.saveAndFlush(notification);

        NotificationDeliveryResult result = simulator.send(notification);
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        if (result.isSuccessful()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setLastError(null);
            notification.setSentAt(Instant.now());
            notification = notificationRepository.saveAndFlush(notification);
            publisher.publishNotificationSent(event, notification);
            log.info("Notification {} SENT channel={} recipient={} orderId={}",
                    notification.getId(), notification.getChannel(), notification.getRecipient(), notification.getOrderId());
        } else {
            String reason = nonBlankOrDefault(result.getFailureReason(), "Notification delivery failed");
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError(reason);
            notification = notificationRepository.saveAndFlush(notification);
            publisher.publishNotificationFailed(event, notification, reason);
            log.info("Notification {} FAILED channel={} recipient={} reason={}",
                    notification.getId(), notification.getChannel(), notification.getRecipient(), reason);
        }
    }

    private Notification buildNotification(EventEnvelope<NotificationRequestedPayload> event,
                                           NotificationRequestedPayload payload) {
        Long userId = payload.getUserId() != null ? payload.getUserId() : event.getUserId();
        String type = nonBlankOrDefault(payload.getType(), "GENERIC_NOTIFICATION");
        String summary = nonBlankOrDefault(payload.getSummary(), "Notification requested");
        NotificationChannel channel = chooseChannel(type);
        String recipient = resolveRecipient(channel, userId);

        Map<String, Object> payloadData = new LinkedHashMap<>();
        payloadData.put("userId", userId);
        payloadData.put("orderId", payload.getOrderId());
        payloadData.put("type", type);
        payloadData.put("summary", summary);

        return Notification.builder()
                .requestEventId(event.getEventId())
                .userId(userId)
                .orderId(payload.getOrderId())
                .recipient(recipient)
                .channel(channel)
                .template(type)
                .type(type)
                .body(summary)
                .payloadData(JsonUtils.toJson(payloadData))
                .status(NotificationStatus.QUEUED)
                .attemptCount(0)
                .build();
    }

    private static NotificationRequestedPayload requirePayload(EventEnvelope<NotificationRequestedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new NotificationException(NotificationErrorMessage.INVALID_NOTIFICATION_EVENT,
                    "Inbound notification event has no payload");
        }
        return event.getPayload();
    }

    private static NotificationChannel chooseChannel(String type) {
        String normalized = type == null ? "" : type.toUpperCase();
        if (normalized.contains("SMS")) {
            return NotificationChannel.SMS;
        }
        if (normalized.contains("PUSH")) {
            return NotificationChannel.PUSH;
        }
        return DEFAULT_CHANNEL;
    }

    private static String resolveRecipient(NotificationChannel channel, Long userId) {
        String suffix = userId == null ? "unknown" : userId.toString();
        return switch (channel) {
            case EMAIL -> "user-" + suffix + "@example.local";
            case PUSH -> "push-token-user-" + suffix;
            case SMS -> "+84000000" + suffix;
        };
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
