package com.techlab.ecommerce.notification.simulator;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable fake notification provider knobs used by local experiments.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notification.simulator")
public class NotificationSimulatorProperties {

    /** Artificial delivery latency in milliseconds. */
    private long latencyMs = 200L;

    /** Probability in [0.0, 1.0] that simulated delivery fails. */
    private double failureRate = 0.0D;

    public double normalizedFailureRate() {
        if (failureRate < 0.0D) {
            return 0.0D;
        }
        if (failureRate > 1.0D) {
            return 1.0D;
        }
        return failureRate;
    }
}
