package com.techlab.ecommerce.order.service;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.order.dto.request.CreateOrderItemRequest;
import com.techlab.ecommerce.order.dto.request.CreateOrderRequest;
import com.techlab.ecommerce.order.dto.response.OrderResponse;
import com.techlab.ecommerce.order.entity.Order;
import com.techlab.ecommerce.order.enums.OrderStatus;
import com.techlab.ecommerce.order.exception.OrderException;
import com.techlab.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Service-level happy-path coverage for {@link OrderService}.
 *
 * <p>Uses the full Spring context (with H2 in PostgreSQL mode and
 * {@code RabbitAutoConfiguration} excluded) and a {@code @MockitoBean}
 * {@link RabbitTemplate} so we can both prove the bean wiring works and
 * verify the publisher was invoked with the right exchange / routing key.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private static final long USER_ID = 42L;

    @Test
    void createOrder_persistsOrderAndPublishesOrderCreated() {
        CreateOrderRequest request = sampleRequest();

        OrderResponse response = orderService.createOrder(request, USER_ID, null);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("75.00"); // 25.00*2 + 12.50*2
        assertThat(response.getItems()).hasSize(2);

        Order persisted = orderRepository.findWithItemsById(response.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(persisted.getItems()).hasSize(2);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(ExchangeNames.ORDER), eq(RoutingKeys.ORDER_CREATED), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(RoutingKeys.ORDER_CREATED);
        assertThat(sent.getSourceService()).isEqualTo("order-service");
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        assertThat(sent.getEventId()).isNotBlank();
        assertThat(sent.getCorrelationId()).isNotBlank();
    }

    @Test
    void createOrder_idempotencyKey_replayReturnsExistingOrder() {
        CreateOrderRequest request = sampleRequest();
        String idempotencyKey = "client-key-" + System.nanoTime();

        OrderResponse first = orderService.createOrder(request, USER_ID, idempotencyKey);
        OrderResponse second = orderService.createOrder(request, USER_ID, idempotencyKey);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.findByUserIdAndIdempotencyKey(USER_ID, idempotencyKey))
                .isPresent()
                .get()
                .extracting(Order::getId)
                .isEqualTo(first.getId());
    }

    @Test
    void getOrder_otherUserSeesNotFound() {
        OrderResponse mine = orderService.createOrder(sampleRequest(), USER_ID, null);

        assertThatThrownBy(() -> orderService.getOrder(mine.getId(), USER_ID + 1))
                .isInstanceOf(OrderException.class);
    }

    private static CreateOrderRequest sampleRequest() {
        return CreateOrderRequest.builder()
                .currency("USD")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(1001L)
                                .quantity(2)
                                .unitPrice(new BigDecimal("25.00"))
                                .build(),
                        CreateOrderItemRequest.builder()
                                .productId(1002L)
                                .quantity(2)
                                .unitPrice(new BigDecimal("12.50"))
                                .build()))
                .build();
    }
}
