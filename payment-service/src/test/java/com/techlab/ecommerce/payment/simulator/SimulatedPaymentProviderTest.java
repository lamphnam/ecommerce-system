package com.techlab.ecommerce.payment.simulator;

import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentProviderTest {

    @Test
    void failureRateZeroAlwaysSucceeds() {
        PaymentSimulatorProperties properties = new PaymentSimulatorProperties();
        properties.setLatencyMs(0L);
        properties.setFailureRate(0.0D);
        SimulatedPaymentProvider simulator = new SimulatedPaymentProvider(properties);

        PaymentProviderResult result = simulator.process(samplePayment(), 1);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getProviderReference()).startsWith("PAY-");
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    void failureRateOneAlwaysDeclines() {
        PaymentSimulatorProperties properties = new PaymentSimulatorProperties();
        properties.setLatencyMs(0L);
        properties.setFailureRate(1.0D);
        SimulatedPaymentProvider simulator = new SimulatedPaymentProvider(properties);

        PaymentProviderResult result = simulator.process(samplePayment(), 1);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getProviderReference()).isNull();
        assertThat(result.getFailureReason()).contains("Simulated provider decline");
    }

    private static Payment samplePayment() {
        return Payment.builder()
                .orderId(123L)
                .status(PaymentStatus.PROCESSING)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paymentMethod("SIMULATED_CARD")
                .build();
    }
}
