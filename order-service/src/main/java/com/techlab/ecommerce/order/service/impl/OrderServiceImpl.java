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
import com.techlab.ecommerce.order.messaging.publisher.OrderEventPublisher;
import com.techlab.ecommerce.order.repository.OrderRepository;
import com.techlab.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Implementation of the HTTP-facing Order Service operations.
 *
 * <h3>Idempotency on POST</h3>
 * If the request carries an {@code Idempotency-Key} header, the service first
 * looks for an existing order under {@code (userId, idempotencyKey)}. A hit
 * returns the existing order; a miss inserts the new order and relies on the
 * unique constraint to win any race, in which case the duplicate insert raises
 * {@link DataIntegrityViolationException} and we re-fetch and return the
 * already-created order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher publisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                log.debug("Idempotency hit for user={} key={} → returning order {}",
                        userId, idempotencyKey, existing.get().getId());
                return orderMapper.toDto(existing.get());
            }
        }

        Order order = buildOrder(request, userId, idempotencyKey);

        try {
            order = orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException duplicate) {
            // Race: another thread won the unique (userId, idempotencyKey) insert.
            // Re-fetch and return the winner so the client gets a stable response.
            if (idempotencyKey != null) {
                return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                        .map(orderMapper::toDto)
                        .orElseThrow(() -> new OrderException(OrderErrorMessage.ORDER_DUPLICATE_IDEMPOTENCY_KEY));
            }
            throw duplicate;
        }

        publisher.publishOrderCreated(order);
        log.info("Order {} created for user {} ({} items, total={})",
                order.getId(), userId, order.getItems().size(), order.getTotalAmount());
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, Long userId) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderException(OrderErrorMessage.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            // Ownership enforced at the service boundary so admins reading a different
            // user's order would have to use a separate admin endpoint (out of scope).
            throw new OrderException(OrderErrorMessage.ORDER_NOT_FOUND);
        }
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(Long userId, OrderStatus statusFilter, Pageable pageable) {
        Page<Order> page = (statusFilter == null)
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndStatus(userId, statusFilter, pageable);
        return page.map(orderMapper::toDto);
    }

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
