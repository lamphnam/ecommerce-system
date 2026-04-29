package com.techlab.ecommerce.inventory.service;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.inventory.dto.request.AdjustStockRequest;
import com.techlab.ecommerce.inventory.dto.request.CreateProductRequest;
import com.techlab.ecommerce.inventory.dto.response.ProductResponse;
import com.techlab.ecommerce.inventory.entity.Product;
import com.techlab.ecommerce.inventory.enums.ReservationStatus;
import com.techlab.ecommerce.inventory.exception.DuplicateProcessedEventException;
import com.techlab.ecommerce.inventory.messaging.payload.InventoryFailedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.InventoryReservedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.OrderCreatedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.OrderItemPayload;
import com.techlab.ecommerce.inventory.repository.ProductRepository;
import com.techlab.ecommerce.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceImplTest {

    private static final long USER_ID = 42L;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventorySagaService sagaService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository reservationRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void resetMocks() {
        reset(rabbitTemplate);
    }

    @Test
    void createProductAndAdjustStock_persistsProduct() {
        ProductResponse created = productService.createProduct(sampleProduct("SKU-" + UUID.randomUUID(), 5));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStock()).isEqualTo(5);
        assertThat(created.getActive()).isTrue();

        ProductResponse adjusted = productService.adjustStock(created.getId(),
                AdjustStockRequest.builder().stock(12).build());

        assertThat(adjusted.getStock()).isEqualTo(12);
        assertThat(productService.getProduct(created.getId()).getStock()).isEqualTo(12);
    }

    @Test
    void handleOrderCreated_reservesStockAndPublishesInventoryReserved() {
        ProductResponse product = productService.createProduct(sampleProduct("SKU-" + UUID.randomUUID(), 10));
        EventEnvelope<OrderCreatedPayload> event = orderCreatedEvent(1001L, product.getId(), 3, "corr-reserve");

        sagaService.handleOrderCreated(event);

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persisted.getStock()).isEqualTo(7);
        assertThat(reservationRepository.findByOrderIdAndStatus(1001L, ReservationStatus.RESERVED))
                .hasSize(1)
                .first()
                .satisfies(reservation -> {
                    assertThat(reservation.getProductId()).isEqualTo(product.getId());
                    assertThat(reservation.getQuantity()).isEqualTo(3);
                });

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ExchangeNames.INVENTORY), eq(RoutingKeys.INVENTORY_RESERVED), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(RoutingKeys.INVENTORY_RESERVED);
        assertThat(sent.getSourceService()).isEqualTo("inventory-service");
        assertThat(sent.getCorrelationId()).isEqualTo("corr-reserve");
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        assertThat(sent.getPayload()).isInstanceOf(InventoryReservedPayload.class);
        InventoryReservedPayload payload = (InventoryReservedPayload) sent.getPayload();
        assertThat(payload.getOrderId()).isEqualTo(1001L);
        assertThat(payload.getReservationId()).isNotNull();
    }

    @Test
    void handleOrderCreated_insufficientStockRestoresAndPublishesInventoryFailed() {
        ProductResponse product = productService.createProduct(sampleProduct("SKU-" + UUID.randomUUID(), 1));
        EventEnvelope<OrderCreatedPayload> event = orderCreatedEvent(1002L, product.getId(), 2, "corr-fail");

        sagaService.handleOrderCreated(event);

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persisted.getStock()).isEqualTo(1);
        assertThat(reservationRepository.countByOrderId(1002L)).isZero();

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ExchangeNames.INVENTORY), eq(RoutingKeys.INVENTORY_FAILED), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getCorrelationId()).isEqualTo("corr-fail");
        assertThat(sent.getPayload()).isInstanceOf(InventoryFailedPayload.class);
        InventoryFailedPayload payload = (InventoryFailedPayload) sent.getPayload();
        assertThat(payload.getOrderId()).isEqualTo(1002L);
        assertThat(payload.getReason()).contains("Insufficient stock");
    }

    @Test
    void handleOrderCreated_duplicateEventDoesNotReserveTwice() {
        ProductResponse product = productService.createProduct(sampleProduct("SKU-" + UUID.randomUUID(), 5));
        EventEnvelope<OrderCreatedPayload> event = orderCreatedEvent(1003L, product.getId(), 2, "corr-duplicate");

        sagaService.handleOrderCreated(event);

        assertThatThrownBy(() -> sagaService.handleOrderCreated(event))
                .isInstanceOf(DuplicateProcessedEventException.class);

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persisted.getStock()).isEqualTo(3);
        assertThat(reservationRepository.findByOrderIdAndStatus(1003L, ReservationStatus.RESERVED)).hasSize(1);
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(ExchangeNames.INVENTORY), eq(RoutingKeys.INVENTORY_RESERVED), captor.capture());
    }

    private static CreateProductRequest sampleProduct(String sku, int stock) {
        return CreateProductRequest.builder()
                .sku(sku)
                .name("Test product " + sku)
                .price(new BigDecimal("19.99"))
                .stock(stock)
                .build();
    }

    private static EventEnvelope<OrderCreatedPayload> orderCreatedEvent(Long orderId,
                                                                        Long productId,
                                                                        int quantity,
                                                                        String correlationId) {
        OrderItemPayload item = OrderItemPayload.builder()
                .productId(productId)
                .quantity(quantity)
                .unitPrice(new BigDecimal("19.99"))
                .subtotal(new BigDecimal("19.99").multiply(BigDecimal.valueOf(quantity)))
                .build();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(orderId)
                .userId(USER_ID)
                .totalAmount(item.getSubtotal())
                .currency("USD")
                .items(List.of(item))
                .build();
        return EventEnvelope.<OrderCreatedPayload>builder()
                .eventId("evt-" + UUID.randomUUID())
                .eventType(RoutingKeys.ORDER_CREATED)
                .sourceService("order-service")
                .occurredAt(Instant.now())
                .version(1)
                .correlationId(correlationId)
                .userId(USER_ID)
                .payload(payload)
                .build();
    }
}
