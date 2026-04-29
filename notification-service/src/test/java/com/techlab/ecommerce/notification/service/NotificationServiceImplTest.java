package com.techlab.ecommerce.notification.service;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.notification.entity.Notification;
import com.techlab.ecommerce.notification.enums.NotificationChannel;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import com.techlab.ecommerce.notification.exception.DuplicateProcessedEventException;
import com.techlab.ecommerce.notification.messaging.payload.NotificationFailedPayload;
import com.techlab.ecommerce.notification.messaging.payload.NotificationSentPayload;
import com.techlab.ecommerce.notification.messaging.payload.inbound.NotificationRequestedPayload;
import com.techlab.ecommerce.notification.repository.NotificationRepository;
import com.techlab.ecommerce.notification.simulator.NotificationDeliveryResult;
import com.techlab.ecommerce.notification.simulator.NotificationProviderSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceImplTest {

    private static final long USER_ID = 42L;

    @Autowired
    private NotificationSagaService sagaService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationProviderSimulator simulator;

    @AfterEach
    void resetMocks() {
        reset(rabbitTemplate, simulator);
    }

    @Test
    void handleNotificationRequested_success_createsNotificationAndPublishesNotificationSent() {
        EventEnvelope<NotificationRequestedPayload> event = notificationRequestedEvent(1001L, "ORDER_CONFIRMED", "corr-sent");
        when(simulator.send(any(Notification.class)))
                .thenReturn(NotificationDeliveryResult.success(15L));

        sagaService.handleNotificationRequested(event);

        Notification notification = notificationRepository.findByRequestEventId(event.getEventId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getLastError()).isNull();
        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.getRecipient()).isEqualTo("user-" + USER_ID + "@example.local");

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ExchangeNames.NOTIFICATION), eq(RoutingKeys.NOTIFICATION_SENT), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(RoutingKeys.NOTIFICATION_SENT);
        assertThat(sent.getSourceService()).isEqualTo("notification-service");
        assertThat(sent.getCorrelationId()).isEqualTo("corr-sent");
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        assertThat(sent.getPayload()).isInstanceOf(NotificationSentPayload.class);
        NotificationSentPayload payload = (NotificationSentPayload) sent.getPayload();
        assertThat(payload.getNotificationId()).isEqualTo(notification.getId());
        assertThat(payload.getOrderId()).isEqualTo(1001L);
        assertThat(payload.getUserId()).isEqualTo(USER_ID);
        assertThat(payload.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(payload.getType()).isEqualTo("ORDER_CONFIRMED");
    }

    @Test
    void handleNotificationRequested_failedSimulation_createsFailedNotificationAndPublishesNotificationFailed() {
        EventEnvelope<NotificationRequestedPayload> event = notificationRequestedEvent(1002L, "PAYMENT_FAILED", "corr-failed");
        when(simulator.send(any(Notification.class)))
                .thenReturn(NotificationDeliveryResult.failed("Provider timeout", 30L));

        sagaService.handleNotificationRequested(event);

        Notification notification = notificationRepository.findByRequestEventId(event.getEventId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getLastError()).isEqualTo("Provider timeout");
        assertThat(notification.getSentAt()).isNull();

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ExchangeNames.NOTIFICATION), eq(RoutingKeys.NOTIFICATION_FAILED), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(RoutingKeys.NOTIFICATION_FAILED);
        assertThat(sent.getCorrelationId()).isEqualTo("corr-failed");
        assertThat(sent.getPayload()).isInstanceOf(NotificationFailedPayload.class);
        NotificationFailedPayload payload = (NotificationFailedPayload) sent.getPayload();
        assertThat(payload.getNotificationId()).isEqualTo(notification.getId());
        assertThat(payload.getOrderId()).isEqualTo(1002L);
        assertThat(payload.getReason()).isEqualTo("Provider timeout");
    }

    @Test
    void handleNotificationRequested_duplicateEventDoesNotSendOrPublishTwice() {
        EventEnvelope<NotificationRequestedPayload> event = notificationRequestedEvent(1003L, "ORDER_FAILED", "corr-duplicate");
        when(simulator.send(any(Notification.class)))
                .thenReturn(NotificationDeliveryResult.success(5L));

        sagaService.handleNotificationRequested(event);

        assertThatThrownBy(() -> sagaService.handleNotificationRequested(event))
                .isInstanceOf(DuplicateProcessedEventException.class);

        assertThat(notificationRepository.findByRequestEventId(event.getEventId())).isPresent();
        verify(simulator, times(1)).send(any(Notification.class));
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(ExchangeNames.NOTIFICATION), eq(RoutingKeys.NOTIFICATION_SENT), captor.capture());
    }

    private static EventEnvelope<NotificationRequestedPayload> notificationRequestedEvent(Long orderId,
                                                                                          String type,
                                                                                          String correlationId) {
        NotificationRequestedPayload payload = NotificationRequestedPayload.builder()
                .userId(USER_ID)
                .orderId(orderId)
                .type(type)
                .summary("Notification for " + type)
                .build();
        return EventEnvelope.<NotificationRequestedPayload>builder()
                .eventId("evt-" + UUID.randomUUID())
                .eventType(RoutingKeys.NOTIFICATION_REQUESTED)
                .sourceService("order-service")
                .occurredAt(Instant.now())
                .version(1)
                .correlationId(correlationId)
                .userId(USER_ID)
                .payload(payload)
                .build();
    }
}
