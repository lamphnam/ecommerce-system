package com.techlab.ecommerce.payment.simulator;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable fake provider knobs used by local experiments.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.simulator")
public class PaymentSimulatorProperties {

    /** Artificial provider latency in milliseconds. */
    private long latencyMs = 500L;

    /** Probability in [0.0, 1.0] that the provider declines the payment. */
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
