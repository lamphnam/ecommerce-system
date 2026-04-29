package com.techlab.ecommerce.order.service;

import com.techlab.ecommerce.order.dto.request.CreateOrderRequest;
import com.techlab.ecommerce.order.dto.response.OrderResponse;
import com.techlab.ecommerce.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * REST-side business operations for the Order Service.
 * Saga-side reactions live in {@link OrderSagaService}.
 */
public interface OrderService {

    /**
     * Persist a new order, publish {@code order.created}, and return the snapshot.
     *
     * @param request        validated body
     * @param userId         resolved user from gateway-propagated context
     * @param idempotencyKey optional client-provided {@code Idempotency-Key} header;
     *                       if non-null and an order already exists for
     *                       {@code (userId, idempotencyKey)}, that existing order is
     *                       returned instead of creating a duplicate
     */
    OrderResponse createOrder(CreateOrderRequest request, Long userId, String idempotencyKey);

    OrderResponse getOrder(Long id, Long userId);

    Page<OrderResponse> listOrders(Long userId, OrderStatus statusFilter, Pageable pageable);
}
