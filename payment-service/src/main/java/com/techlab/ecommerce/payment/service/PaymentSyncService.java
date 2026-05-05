package com.techlab.ecommerce.payment.service;

import com.techlab.ecommerce.payment.dto.request.ProcessPaymentRequest;
import com.techlab.ecommerce.payment.dto.response.PaymentResponse;

/**
 * Synchronous payment processing for the experiment baseline.
 * Reuses the same simulator and DB logic as the async saga path,
 * but is callable via REST instead of RabbitMQ.
 */
public interface PaymentSyncService {

    PaymentResponse processPayment(ProcessPaymentRequest request);
}
