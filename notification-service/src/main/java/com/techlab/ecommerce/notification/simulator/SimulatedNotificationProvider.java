package com.techlab.ecommerce.notification.simulator;

import com.techlab.ecommerce.notification.entity.Notification;
import com.techlab.ecommerce.notification.enums.NotificationErrorMessage;
import com.techlab.ecommerce.notification.exception.NotificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Small configurable fake notification provider.
 *
 * <p>Deterministic edges: failure-rate 0.0 always succeeds, failure-rate 1.0
 * always fails. Intermediate values use random sampling and are intended for
 * traffic/failure experiments, not exact assertions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedNotificationProvider implements NotificationProviderSimulator {

    private final NotificationSimulatorProperties properties;

    @Override
    public NotificationDeliveryResult send(Notification notification) {
        long started = System.nanoTime();
        sleep(properties.getLatencyMs());
        long elapsedMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

        double failureRate = properties.normalizedFailureRate();
        boolean failed = failureRate >= 1.0D
                || (failureRate > 0.0D && ThreadLocalRandom.current().nextDouble() < failureRate);
        if (failed) {
            String reason = "Simulated notification delivery failure for notification " + notification.getId();
            log.debug("Notification provider failed notification {} channel={} recipient={}",
                    notification.getId(), notification.getChannel(), notification.getRecipient());
            return NotificationDeliveryResult.failed(reason, elapsedMs);
        }

        log.info("Simulated notification sent notificationId={} channel={} recipient={} template={} body={}",
                notification.getId(), notification.getChannel(), notification.getRecipient(),
                notification.getTemplate(), notification.getBody());
        return NotificationDeliveryResult.success(elapsedMs);
    }

    private static void sleep(long latencyMs) {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationException(NotificationErrorMessage.INVALID_NOTIFICATION_EVENT,
                    "Notification simulation was interrupted");
        }
    }
}
