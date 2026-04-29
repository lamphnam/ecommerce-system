package com.techlab.ecommerce.payment.simulator;

import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.enums.PaymentErrorMessage;
import com.techlab.ecommerce.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Small configurable fake payment provider.
 *
 * <p>Deterministic edges: failure-rate 0.0 always succeeds, failure-rate 1.0
 * always declines. Intermediate values use random sampling and are intended for
 * traffic/failure experiments, not exact assertions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedPaymentProvider implements PaymentProviderSimulator {

    private final PaymentSimulatorProperties properties;

    @Override
    public PaymentProviderResult process(Payment payment, int attemptNumber) {
        long started = System.nanoTime();
        sleep(properties.getLatencyMs());
        long elapsedMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

        double failureRate = properties.normalizedFailureRate();
        boolean declined = failureRate >= 1.0D
                || (failureRate > 0.0D && ThreadLocalRandom.current().nextDouble() < failureRate);
        if (declined) {
            String reason = "Simulated provider decline for order " + payment.getOrderId();
            log.debug("Payment provider declined order {} attempt {}", payment.getOrderId(), attemptNumber);
            return PaymentProviderResult.declined(reason, elapsedMs);
        }

        String providerReference = "PAY-" + UUID.randomUUID();
        log.debug("Payment provider approved order {} attempt {} providerRef={}",
                payment.getOrderId(), attemptNumber, providerReference);
        return PaymentProviderResult.success(providerReference, elapsedMs);
    }

    private static void sleep(long latencyMs) {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException(PaymentErrorMessage.PAYMENT_PROVIDER_UNAVAILABLE,
                    "Payment provider simulation was interrupted");
        }
    }
}
