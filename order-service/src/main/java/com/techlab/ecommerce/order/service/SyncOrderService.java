package com.techlab.ecommerce.order.service;

import com.techlab.ecommerce.order.dto.request.CreateOrderRequest;
import com.techlab.ecommerce.order.dto.response.OrderResponse;

/**
 * Synchronous order creation for the experiment baseline.
 * Same order-creation logic as the async flow, but calls
 * inventory-service and payment-service via REST instead of RabbitMQ.
 */
public interface SyncOrderService {

    OrderResponse createOrderSync(CreateOrderRequest request, Long userId, String idempotencyKey);
}
