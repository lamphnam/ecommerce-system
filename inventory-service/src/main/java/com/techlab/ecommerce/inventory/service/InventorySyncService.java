package com.techlab.ecommerce.inventory.service;

import com.techlab.ecommerce.inventory.dto.request.ReserveStockRequest;
import com.techlab.ecommerce.inventory.dto.response.ReservationResponse;

/**
 * Synchronous stock reservation for the experiment baseline.
 * Reuses the same atomic decrement logic as the async saga path,
 * but is callable via REST instead of RabbitMQ.
 */
public interface InventorySyncService {

    ReservationResponse reserveStock(ReserveStockRequest request);
}
