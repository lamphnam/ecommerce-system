package com.techlab.ecommerce.inventory.service.impl;

import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.inventory.entity.StockReservation;
import com.techlab.ecommerce.inventory.enums.InventoryErrorMessage;
import com.techlab.ecommerce.inventory.enums.ReservationStatus;
import com.techlab.ecommerce.inventory.exception.InventoryException;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.InventoryReleaseRequestedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.OrderCreatedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.OrderItemPayload;
import com.techlab.ecommerce.inventory.messaging.publisher.InventoryEventPublisher;
import com.techlab.ecommerce.inventory.repository.ProductRepository;
import com.techlab.ecommerce.inventory.repository.StockReservationRepository;
import com.techlab.ecommerce.inventory.service.IdempotentEventProcessor;
import com.techlab.ecommerce.inventory.service.InventorySagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventory side of the order saga.
 *
 * <p>Reservation uses a single atomic SQL update per item:
 * {@code UPDATE products SET stock = stock - :qty WHERE id = :productId AND stock >= :qty}.
 * The database row lock and predicate prevent overselling under concurrent orders.
 * If a later item fails, this service adds back any stock already decremented in
 * the same transaction, commits the processed-event marker, and emits
 * {@code inventory.failed} so Order Service can fail the saga.
 *
 * <p>Phase-6 simplification: events are published inside the transaction. A
 * transactional outbox is the production-grade next improvement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySagaServiceImpl implements InventorySagaService {

    private final IdempotentEventProcessor idempotency;
    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryEventPublisher publisher;

    @Override
    @Transactional
    public void handleOrderCreated(EventEnvelope<OrderCreatedPayload> event) {
        idempotency.markProcessed(event, QueueNames.INVENTORY_RESERVE);

        OrderCreatedPayload payload = requirePayload(event);
        Long orderId = requireOrderId(payload.getOrderId(), "order.created");
        List<OrderItemPayload> items = payload.getItems();
        if (items == null || items.isEmpty()) {
            publishReservationFailure(event, orderId, "Order contains no items");
            return;
        }

        Map<Long, Integer> decrementedStock = new LinkedHashMap<>();
        List<StockReservation> reservations = new ArrayList<>();

        for (OrderItemPayload item : items) {
            String validationError = validateItem(item);
            if (validationError != null) {
                restoreAlreadyDecremented(decrementedStock);
                publishReservationFailure(event, orderId, validationError);
                return;
            }

            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();
            int updated = productRepository.decrementStockIfAvailable(productId, quantity);
            if (updated == 0) {
                restoreAlreadyDecremented(decrementedStock);
                publishReservationFailure(event, orderId, unavailableStockReason(productId, quantity));
                return;
            }

            decrementedStock.merge(productId, quantity, Integer::sum);
            reservations.add(StockReservation.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .status(ReservationStatus.RESERVED)
                    .build());
        }

        List<StockReservation> saved = reservationRepository.saveAllAndFlush(reservations);
        Long firstReservationId = saved.isEmpty() ? null : saved.get(0).getId();
        publisher.publishInventoryReserved(event, orderId, firstReservationId);
        log.info("Reserved {} inventory line(s) for order {}", saved.size(), orderId);
    }

    @Override
    @Transactional
    public void handleInventoryReleaseRequested(EventEnvelope<InventoryReleaseRequestedPayload> event) {
        idempotency.markProcessed(event, QueueNames.INVENTORY_RELEASE);

        InventoryReleaseRequestedPayload payload = requirePayload(event);
        Long orderId = requireOrderId(payload.getOrderId(), "inventory.release.requested");
        List<StockReservation> reservations = reservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        List<Long> releasedIds = new ArrayList<>();
        for (StockReservation reservation : reservations) {
            int updated = productRepository.incrementStock(reservation.getProductId(), reservation.getQuantity());
            if (updated == 0) {
                throw new InventoryException(InventoryErrorMessage.PRODUCT_NOT_FOUND,
                        "Product " + reservation.getProductId() + " not found while releasing order " + orderId);
            }
            reservation.setStatus(ReservationStatus.RELEASED);
            releasedIds.add(reservation.getId());
        }
        if (!reservations.isEmpty()) {
            reservationRepository.saveAllAndFlush(reservations);
        }

        String reason = nonBlankOrDefault(payload.getReason(), "Inventory release requested");
        publisher.publishInventoryReleased(event, orderId, releasedIds, reason);
        log.info("Released {} inventory reservation(s) for order {}", releasedIds.size(), orderId);
    }

    private void publishReservationFailure(EventEnvelope<?> inbound, Long orderId, String reason) {
        publisher.publishInventoryFailed(inbound, orderId, reason);
        log.info("Inventory reservation failed for order {}: {}", orderId, reason);
    }

    private void restoreAlreadyDecremented(Map<Long, Integer> decrementedStock) {
        for (Map.Entry<Long, Integer> entry : decrementedStock.entrySet()) {
            int updated = productRepository.incrementStock(entry.getKey(), entry.getValue());
            if (updated == 0) {
                throw new InventoryException(InventoryErrorMessage.PRODUCT_NOT_FOUND,
                        "Product " + entry.getKey() + " disappeared while rolling back reservation");
            }
        }
    }

    private String unavailableStockReason(Long productId, Integer quantity) {
        return productRepository.findById(productId)
                .map(product -> Boolean.FALSE.equals(product.getActive())
                        ? "Product " + productId + " is inactive"
                        : "Insufficient stock for product " + productId + " (requested " + quantity
                        + ", available " + product.getStock() + ")")
                .orElse("Product " + productId + " not found");
    }

    private static String validateItem(OrderItemPayload item) {
        if (item == null) {
            return "Order contains a null item";
        }
        if (item.getProductId() == null) {
            return "Order item is missing productId";
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            return "Order item for product " + item.getProductId() + " has invalid quantity";
        }
        return null;
    }

    private static <T> T requirePayload(EventEnvelope<T> event) {
        if (event == null || event.getPayload() == null) {
            throw new InventoryException(InventoryErrorMessage.INVALID_INVENTORY_EVENT,
                    "Inbound inventory event has no payload");
        }
        return event.getPayload();
    }

    private static Long requireOrderId(Long orderId, String eventName) {
        if (orderId == null) {
            throw new InventoryException(InventoryErrorMessage.INVALID_INVENTORY_EVENT,
                    eventName + " payload is missing orderId");
        }
        return orderId;
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
