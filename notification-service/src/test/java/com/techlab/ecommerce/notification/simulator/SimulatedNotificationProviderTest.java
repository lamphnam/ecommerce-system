package com.techlab.ecommerce.notification.simulator;

import com.techlab.ecommerce.notification.entity.Notification;
import com.techlab.ecommerce.notification.enums.NotificationChannel;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedNotificationProviderTest {

    @Test
    void failureRateZeroAlwaysSucceeds() {
        NotificationSimulatorProperties properties = new NotificationSimulatorProperties();
        properties.setLatencyMs(0L);
        properties.setFailureRate(0.0D);
        SimulatedNotificationProvider simulator = new SimulatedNotificationProvider(properties);

        NotificationDeliveryResult result = simulator.send(sampleNotification());

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    void failureRateOneAlwaysFails() {
        NotificationSimulatorProperties properties = new NotificationSimulatorProperties();
        properties.setLatencyMs(0L);
        properties.setFailureRate(1.0D);
        SimulatedNotificationProvider simulator = new SimulatedNotificationProvider(properties);

        NotificationDeliveryResult result = simulator.send(sampleNotification());

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureReason()).contains("Simulated notification delivery failure");
    }

    private static Notification sampleNotification() {
        return Notification.builder()
                .requestEventId("evt-test")
                .userId(42L)
                .orderId(123L)
                .recipient("user-42@example.local")
                .channel(NotificationChannel.EMAIL)
                .template("ORDER_CONFIRMED")
                .type("ORDER_CONFIRMED")
                .body("Order confirmed")
                .payloadData("{}")
                .status(NotificationStatus.QUEUED)
                .attemptCount(0)
                .build();
    }
}
