package com.techlab.ecommerce.inventory.service.impl;

import com.techlab.ecommerce.inventory.dto.request.ReserveStockItemRequest;
import com.techlab.ecommerce.inventory.dto.request.ReserveStockRequest;
import com.techlab.ecommerce.inventory.dto.response.ReservationResponse;
import com.techlab.ecommerce.inventory.entity.StockReservation;
import com.techlab.ecommerce.inventory.enums.InventoryErrorMessage;
import com.techlab.ecommerce.inventory.enums.ReservationStatus;
import com.techlab.ecommerce.inventory.exception.InventoryException;
import com.techlab.ecommerce.inventory.repository.ProductRepository;
import com.techlab.ecommerce.inventory.repository.StockReservationRepository;
import com.techlab.ecommerce.inventory.service.InventorySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronous stock reservation for the experiment baseline.
 *
 * <p>Reuses the same atomic SQL decrement approach as {@link InventorySagaServiceImpl}:
 * {@code UPDATE products SET stock = stock - :qty WHERE stock >= :qty}.
 * On partial failure, already-decremented items are restored in the same transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySyncServiceImpl implements InventorySyncService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;

    @Override
    @Transactional
    public ReservationResponse reserveStock(ReserveStockRequest request) {
        Long orderId = request.getOrderId();
        List<ReserveStockItemRequest> items = request.getItems();

        Map<Long, Integer> decrementedStock = new LinkedHashMap<>();
        List<StockReservation> reservations = new ArrayList<>();

        for (ReserveStockItemRequest item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            int updated = productRepository.decrementStockIfAvailable(productId, quantity);
            if (updated == 0) {
                restoreAlreadyDecremented(decrementedStock);
                String reason = unavailableStockReason(productId, quantity);
                throw new InventoryException(InventoryErrorMessage.INSUFFICIENT_STOCK, reason);
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
        log.info("Sync: reserved {} inventory line(s) for order {}", saved.size(), orderId);

        List<ReservationResponse.ReservationItem> responseItems = saved.stream()
                .map(r -> ReservationResponse.ReservationItem.builder()
                        .reservationId(r.getId())
                        .productId(r.getProductId())
                        .quantity(r.getQuantity())
                        .status(r.getStatus())
                        .build())
                .toList();

        return ReservationResponse.builder()
                .orderId(orderId)
                .success(true)
                .reservations(responseItems)
                .build();
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
}
