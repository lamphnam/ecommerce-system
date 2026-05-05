package com.techlab.ecommerce.order.service.impl;

import com.techlab.ecommerce.order.dto.request.CreateOrderItemRequest;
import com.techlab.ecommerce.order.dto.request.CreateOrderRequest;
import com.techlab.ecommerce.order.dto.response.OrderResponse;
import com.techlab.ecommerce.order.entity.Order;
import com.techlab.ecommerce.order.entity.OrderItem;
import com.techlab.ecommerce.order.enums.OrderErrorMessage;
import com.techlab.ecommerce.order.enums.OrderStatus;
import com.techlab.ecommerce.order.exception.OrderException;
import com.techlab.ecommerce.order.mapper.OrderMapper;
import com.techlab.ecommerce.order.repository.OrderRepository;
import com.techlab.ecommerce.order.service.SyncOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Synchronous order creation for the experiment baseline.
 *
 * <p>Uses the same order-building and persistence logic as {@link OrderServiceImpl},
 * but instead of publishing {@code order.created} to RabbitMQ, it calls
 * inventory-service and payment-service via synchronous REST.
 *
 * <p>Flow:
 * <ol>
 *   <li>Build and save Order (PENDING) — identical to async flow.</li>
 *   <li>POST /api/inventory/reserve to inventory-service — blocks until stock is reserved.</li>
 *   <li>POST /api/payments/process to payment-service — blocks until payment is processed.</li>
 *   <li>On success: mark CONFIRMED. On failure: mark FAILED (inventory compensation is skipped
 *       in this simplified sync flow, since the purpose is measuring latency, not saga correctness).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrderServiceImpl implements SyncOrderService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    @Qualifier("inventoryRestClient")
    private final RestClient inventoryRestClient;
    @Qualifier("paymentRestClient")
    private final RestClient paymentRestClient;

    @Override
    @Transactional
    public OrderResponse createOrderSync(CreateOrderRequest request, Long userId, String idempotencyKey) {
        // ---- idempotency check (identical to async flow) ----
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                log.debug("Sync idempotency hit for user={} key={} → returning order {}",
                        userId, idempotencyKey, existing.get().getId());
                return orderMapper.toDto(existing.get());
            }
        }

        // ---- build + save order (identical logic to OrderServiceImpl.buildOrder) ----
        Order order = buildOrder(request, userId, idempotencyKey);
        try {
            order = orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException duplicate) {
            if (idempotencyKey != null) {
                return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                        .map(orderMapper::toDto)
                        .orElseThrow(() -> new OrderException(OrderErrorMessage.ORDER_DUPLICATE_IDEMPOTENCY_KEY));
            }
            throw duplicate;
        }

        log.info("Sync: order {} created for user {} ({} items, total={})",
                order.getId(), userId, order.getItems().size(), order.getTotalAmount());

        // ---- synchronous inventory reservation ----
        try {
            List<Map<String, Object>> items = order.getItems().stream()
                    .map(item -> Map.<String, Object>of(
                            "productId", item.getProductId(),
                            "quantity", item.getQuantity()))
                    .toList();

            Map<String, Object> reserveBody = Map.of(
                    "orderId", order.getId(),
                    "userId", userId,
                    "items", items);

            inventoryRestClient.post()
                    .uri("/api/inventory/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reserveBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Sync: inventory reserved for order {}", order.getId());
        } catch (RestClientResponseException e) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("Inventory reservation failed: " + e.getStatusText());
            orderRepository.saveAndFlush(order);
            log.info("Sync: order {} FAILED at inventory: {}", order.getId(), e.getStatusText());
            return orderMapper.toDto(order);
        } catch (Exception e) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("Inventory service unavailable: " + e.getMessage());
            orderRepository.saveAndFlush(order);
            log.info("Sync: order {} FAILED - inventory unreachable: {}", order.getId(), e.getMessage());
            return orderMapper.toDto(order);
        }

        // ---- synchronous payment processing ----
        try {
            Map<String, Object> paymentBody = Map.of(
                    "orderId", order.getId(),
                    "userId", userId,
                    "amount", order.getTotalAmount(),
                    "currency", order.getCurrency());

            paymentRestClient.post()
                    .uri("/api/payments/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(paymentBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.saveAndFlush(order);
            log.info("Sync: order {} CONFIRMED", order.getId());
        } catch (RestClientResponseException e) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("Payment failed: " + e.getStatusText());
            orderRepository.saveAndFlush(order);
            log.info("Sync: order {} FAILED at payment: {}", order.getId(), e.getStatusText());
        } catch (Exception e) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("Payment service unavailable: " + e.getMessage());
            orderRepository.saveAndFlush(order);
            log.info("Sync: order {} FAILED - payment unreachable: {}", order.getId(), e.getMessage());
        }

        return orderMapper.toDto(order);
    }

    /**
     * Identical to {@link OrderServiceImpl}'s private buildOrder method.
     * Duplicated here to avoid modifying the existing async service.
     */
    private Order buildOrder(CreateOrderRequest request, Long userId, String idempotencyKey) {
        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency() : DEFAULT_CURRENCY;

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .currency(currency)
                .idempotencyKey(idempotencyKey)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .subtotal(subtotal)
                    .build();
            order.addItem(item);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);
        return order;
    }
}
