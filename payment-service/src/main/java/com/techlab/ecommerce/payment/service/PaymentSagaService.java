package com.techlab.ecommerce.payment.service;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.payment.messaging.payload.inbound.PaymentRequestedPayload;

public interface PaymentSagaService {

    void handlePaymentRequested(EventEnvelope<PaymentRequestedPayload> event);
}
