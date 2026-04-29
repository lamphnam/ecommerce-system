package com.techlab.ecommerce.payment.simulator;

import com.techlab.ecommerce.payment.entity.Payment;

public interface PaymentProviderSimulator {

    PaymentProviderResult process(Payment payment, int attemptNumber);
}
